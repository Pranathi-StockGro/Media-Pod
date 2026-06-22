package com.stockgro.prefetch

import com.stockgro.prefetch.data.PrefetchDatabase
import com.stockgro.prefetch.data.PrefetchEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.time.Clock

class MediaPrefetchManager(
    private val httpClient: HttpClient,
    database: PrefetchDatabase,
    private val cacheDirectoryPath: Path,
    private val config: PrefetchConfig = PrefetchConfig(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(4),
    private val interceptors: PrefetchInterceptor? = null,
) {

    private val dbDao = database.prefetchDao()
    private val stateMutex = Mutex()
    private val inFlightDownloads = mutableMapOf<String, Deferred<PrefetchStatus>>()
    private val activePartialFiles = mutableSetOf<String>()

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    init {
        // Asynchronously clean up stale tracking entries and detached temp files on startup
        coroutineScope.launch {
            pruneStaleCache()
        }
    }

    /**
     * Resolves a media file instantly if cached and valid.
     * If missing, launches a single-flight background download worker.
     */
    fun resolveOrFetch(
        url: String,
        type: PrefetchMediaType,
        fallback: FallbackMedia
    ): Flow<PrefetchStatus> = flow {
        emit(PrefetchStatus.Loading)

        val cachedEntry = dbDao.getEntry(url)
        if (cachedEntry != null && isEntryValid(cachedEntry, type)) {
            emit(PrefetchStatus.Success(cachedEntry.localPath, type))
            return@flow
        }

        // Cache miss or invalid item -> coordinate network load
        val downloadJob = stateMutex.withLock {
            inFlightDownloads.getOrPut(url) {
                coroutineScope.async {
                    executeDownloadPipeline(url, type, fallback)
                }
            }
        }

        try {
            val result = downloadJob.await()
            emit(result)
        } catch (e: Exception) {
            emit(PrefetchStatus.Error(e, fallback))
        } finally {
            stateMutex.withLock {
                inFlightDownloads.remove(url)
            }
        }
    }

    private suspend fun executeDownloadPipeline(
        url: String,
        type: PrefetchMediaType,
        fallback: FallbackMedia
    ): PrefetchStatus {
        interceptors?.onStart(url, type)
        val sanitizedFileName = sanitizeUrlToFilename(url)
        val targetDir = Path(cacheDirectoryPath, type.name.lowercase())

        if (!SystemFileSystem.exists(targetDir)) {
            SystemFileSystem.createDirectories(targetDir)
        }

        val destinationFile = Path(targetDir, "$sanitizedFileName.${type.extension}")
        val partialFile = Path(targetDir, "$sanitizedFileName-${Clock.System.now().toEpochMilliseconds()}.part")

        stateMutex.withLock {
            activePartialFiles.add(partialFile.toString())
        }

        return try {
            // Simplified Ktor 3 file retrieval engine avoiding generic inference bugs
            val response = httpClient.get(url)
            if (response.status.value !in 200..299) {
                throw Exception("Network fetch failure code: ${response.status.value}")
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
            val buffer = ByteArray(config.bufferSize)

            // FIX: .buffered() converts RawSink to standard Sink allowing ByteArray inputs
            SystemFileSystem.sink(partialFile).buffered().use { fileSink ->
                while (!channel.isClosedForRead) {
                    val readBytes = channel.readAvailable(buffer, 0, buffer.size)
                    if (readBytes == -1) break
                    if (readBytes > 0) {
                        fileSink.write(buffer, 0, readBytes)
                    }
                }
                fileSink.flush()
            }

            if (!SystemFileSystem.exists(partialFile) || SystemFileSystem.metadataOrNull(partialFile)?.size == 0L) {
                throw Exception("Downloaded target source structure is empty or failed verification rules.")
            }

            // Pure multiplatform atomic progression
            safeMoveFile(partialFile, destinationFile)

            val finalSize = SystemFileSystem.metadataOrNull(destinationFile)?.size ?: 0L
            val registryEntry = PrefetchEntity(
                url = url,
                type = type.name,
                localPath = destinationFile.toString(),
                downloadedAtMillis = Clock.System.now().toEpochMilliseconds(),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                sizeBytes = finalSize
            )
            dbDao.insertEntry(registryEntry)

            interceptors?.onSuccess(url, type, finalSize)
            PrefetchStatus.Success(destinationFile.toString(), type)
        } catch (e: Exception) {
            if (SystemFileSystem.exists(partialFile)) {
                SystemFileSystem.delete(partialFile)
            }
            interceptors?.onFailure(url, type, e)
            PrefetchStatus.Error(e, fallback)
        } finally {
            stateMutex.withLock {
                activePartialFiles.remove(partialFile.toString())
            }
        }
    }

    private fun isEntryValid(entry: PrefetchEntity, expectedType: PrefetchMediaType): Boolean {
        if (entry.type != expectedType.name) return false

        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (currentTime - entry.createdAt > config.maxFileAge.inWholeMilliseconds) return false

        val file = Path(entry.localPath)
        if (!SystemFileSystem.exists(file) || SystemFileSystem.metadataOrNull(file)?.size == 0L) {
            return false
        }
        return true
    }

    private suspend fun pruneStaleCache() {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val expiredThreshold = currentTime - config.maxFileAge.inWholeMilliseconds

        // 1. Efficiently fetch and prune items that are strictly expired based on DB timestamp
        val expiredEntries = dbDao.getExpiredEntries(expiredThreshold)
        for (entry in expiredEntries) {
            cleanupEntry(entry)
        }

        // 2. Fetch remaining items to check if they still exist on disk
        val remainingEntries = dbDao.getAllEntries()
        for (entry in remainingEntries) {
            val file = Path(entry.localPath)
            if (!SystemFileSystem.exists(file)) {
                dbDao.deleteEntry(entry.url)
            }
        }

        // Clean up orphaned .part files remaining from legacy crashed processes
        cleanOrphanedPartFiles(cacheDirectoryPath)
    }

    private suspend fun cleanupEntry(entry: PrefetchEntity) {
        val file = Path(entry.localPath)
        if (SystemFileSystem.exists(file)) {
            SystemFileSystem.delete(file)
        }
        dbDao.deleteEntry(entry.url)
    }

    private fun cleanOrphanedPartFiles(directory: Path) {
        if (!SystemFileSystem.exists(directory)) return
        val metadata = SystemFileSystem.metadataOrNull(directory) ?: return
        if (!metadata.isDirectory) return

        PrefetchMediaType.entries.forEach { mediaType ->
            val nestedDir = Path(directory, mediaType.name.lowercase())
            if (!SystemFileSystem.exists(nestedDir)) return@forEach

            SystemFileSystem.list(nestedDir)
                .filter { path ->
                    path.name.endsWith(".part") &&
                            path.toString() !in activePartialFiles
                }
                .forEach { stalePart ->
                    SystemFileSystem.delete(stalePart)
                }
        }
    }

    /**
     * Converts a raw internet endpoint uniform locator safely into a valid OS file name
     * without hashing requirements.
     */
    private fun sanitizeUrlToFilename(url: String): String {
        return url.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
            .take(120) // Limit filename depth length boundaries safely
    }

    private fun safeMoveFile(source: Path, destination: Path) {
        if (SystemFileSystem.exists(destination)) {
            SystemFileSystem.delete(destination)
        }

        SystemFileSystem.source(source).buffered().use { input ->
            SystemFileSystem.sink(destination).buffered().use { output ->
                val buffer = ByteArray(config.bufferSize)
                while (true) {
                    val read = input.readAtMostTo(buffer, 0, buffer.size)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
        SystemFileSystem.delete(source)
    }
}
