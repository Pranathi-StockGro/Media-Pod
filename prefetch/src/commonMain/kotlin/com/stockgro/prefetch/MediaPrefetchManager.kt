package com.stockgro.prefetch

import com.stockgro.prefetch.data.PrefetchChunkEntity
import com.stockgro.prefetch.data.PrefetchDatabase
import com.stockgro.prefetch.data.PrefetchEntity
import com.stockgro.prefetch.datasource.ChunkMerger
import com.stockgro.prefetch.exception.ChunkDownloadException
import com.stockgro.prefetch.exception.MetadataResolutionException
import com.stockgro.prefetch.util.FileUtils
import com.stockgro.prefetch.util.FileUtils.ensureExists
import com.stockgro.prefetch.util.FileUtils.sanitizeUrlToFilename
import com.stockgro.prefetch.util.throwIfCancelled
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.etag
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readFully
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * The core manager responsible for coordinating video prefetching, caching, and retrieval.
 *
 * It manages the lifecycle of cached media chunks, handles parallel downloads with retries,
 * and provides data sources for media players on various platforms.
 *
 * @property httpClient The Ktor client used for network requests.
 * @property database The Room database for tracking prefetch metadata and chunks.
 * @property cacheDirectoryPath The filesystem path where media chunks are stored.
 * @property config Configuration parameters for prefetching behavior.
 * @property interceptors Optional listener for prefetch lifecycle events.
 */
class MediaPrefetchManager(
    private val httpClient: HttpClient,
    val database: PrefetchDatabase,
    private val cacheDirectoryPath: Path,
    private var config: PrefetchConfig = PrefetchConfig(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(4),
    private val interceptors: PrefetchInterceptor? = null,
) {

    private val _statusMap = MutableStateFlow<Map<String, PrefetchStatus>>(emptyMap())
    val statusMap: StateFlow<Map<String, PrefetchStatus>> = _statusMap.asStateFlow()

    private val dbDao = database.prefetchDao()
    private val urlLocks = mutableMapOf<String, Mutex>()
    private val chunkLocks = mutableMapOf<Pair<String, Int>, Mutex>()
    private val activeChunkDownloads = mutableSetOf<Pair<String, Int>>()
    private val metadataCache = mutableMapOf<String, PrefetchEntity>()
    private val metadataCacheMutex = Mutex()
    private val activeDownloadsMutex = Mutex()
    private val lockMutex = Mutex()
    private val managerScope = CoroutineScope(SupervisorJob() + dispatcher)
    
    private val chunkDir by lazy { Path(cacheDirectoryPath, "chunks") }

    private suspend fun getLock(url: String): Mutex = lockMutex.withLock {
        urlLocks.getOrPut(url) { Mutex() }
    }

    private suspend fun getChunkLock(url: String, index: Int): Mutex = lockMutex.withLock {
        chunkLocks.getOrPut(url to index) { Mutex() }
    }

    init {
        managerScope.launch {
            pruneStaleCache()
        }
    }

    /**
     * Entry point for background warming of media content.
     * Starts parallel downloading of chunks for the provided URLs based on the strategy.
     *
     * @param urls List of media URLs to prefetch.
     * @param type The type of media (e.g., MP4).
     * @param strategy The prefetching strategy (e.g., Full or First N Chunks).
     */
    fun prefetchVideos(urls: List<String>, type: PrefetchMediaType, strategy: PrefetchStrategy) {
        urls.forEach { url ->
            managerScope.launch {
                executePrefetch(url, type, strategy)
            }
        }
    }

    private suspend fun getMetadataCached(url: String): PrefetchEntity? {
        return metadataCacheMutex.withLock {
            metadataCache[url] ?: dbDao.getMetadata(url)?.also { metadataCache[url] = it }
        }
    }

    private suspend fun executePrefetch(url: String, type: PrefetchMediaType, strategy: PrefetchStrategy) {
        val mutex = getLock(url)
        mutex.withLock<Unit> {
            _statusMap.update { current ->
                val initialStatus: PrefetchStatus = PrefetchStatus.Loading(url, 0f)
                current + (url to initialStatus)
            }

            runCatching {
                val response = try {
                    httpClient.head(url)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // Fallback to a range GET if HEAD fails
                    httpClient.get(url) {
                        header(HttpHeaders.Range, "bytes=0-0")
                    }
                }

                if (response.status.value >= 400) {
                    throw MetadataResolutionException(url, "Server returned ${response.status}")
                }

                val contentType = response.headers[HttpHeaders.ContentType]
                if (contentType?.contains("text") == true || contentType?.contains("html") == true) {
                    throw MetadataResolutionException(url, "Invalid content type: $contentType. Expected video.")
                }

                // Robust total size resolution
                val totalSize = when (response.status) {
                    HttpStatusCode.PartialContent -> {
                        // Priority 1: Content-Range header (e.g. bytes 0-0/12345)
                        response.headers[HttpHeaders.ContentRange]?.split("/")?.lastOrNull()?.toLongOrNull()
                            ?: response.contentLength()
                    }
                    HttpStatusCode.OK -> {
                        // Priority 2: Full Content-Length
                        response.contentLength()
                    }
                    else -> null
                } ?: throw MetadataResolutionException(url, "Could not determine content length")

                val etag = response.etag()

                val existing = getMetadataCached(url)
                if (existing != null && (existing.totalSize != totalSize || (etag != null && existing.etag != etag))) {
                    clearCacheForUrl(url)
                }

                val chunkSize = config.chunkSize
                val metadata = PrefetchEntity(
                    url = url,
                    totalSize = totalSize,
                    etag = etag,
                    contentType = response.headers[HttpHeaders.ContentType],
                    createdAt = existing?.createdAt ?: Clock.System.now().toEpochMilliseconds(),
                    lastAccessedAt = Clock.System.now().toEpochMilliseconds(),
                    chunkSize = chunkSize
                )

                if (existing == null || existing.totalSize != totalSize || existing.etag != etag) {
                    dbDao.insertMetadata(metadata)
                    metadataCacheMutex.withLock {
                        metadataCache[url] = metadata
                    }
                }

                val maxChunks = (totalSize + chunkSize - 1) / chunkSize
                val chunksToDownload = when (strategy) {
                    is PrefetchStrategy.FirstNChunks -> min(strategy.n.toLong(), maxChunks).toInt()
                    is PrefetchStrategy.Full -> maxChunks.toInt()
                }

                val completedChunks = atomic(0)
                val successCount = atomic(0)
                
                supervisorScope {
                    (0 until chunksToDownload).map { i ->
                        async {
                            val success = downloadChunk(url, i, totalSize, chunkSize)
                            if (success) successCount.incrementAndGet()
                            
                            val finished = completedChunks.incrementAndGet()
                            val progress = finished.toFloat() / chunksToDownload
                            _statusMap.update { current ->
                                val loadingStatus: PrefetchStatus = PrefetchStatus.Loading(url, progress)
                                current + (url to loadingStatus)
                            }
                            success
                        }
                    }.awaitAll()
                }

                // Prune cache by size in background after a successful prefetch
                managerScope.launch {
                    pruneCacheBySize()
                }

                if (successCount.value == chunksToDownload) {
                    val successStatus: PrefetchStatus = PrefetchStatus.Success(url, "", type)
                    _statusMap.update { current ->
                        current + (url to successStatus)
                    }
                    this.interceptors?.onChunkSuccess(url, -1)
                } else {
                    val error = ChunkDownloadException(url, -1, "Partial download: ${successCount.value}/$chunksToDownload chunks success")
                    _statusMap.update { current ->
                        val errorStatus: PrefetchStatus = PrefetchStatus.Error(url, error)
                        current + (url to errorStatus)
                    }
                    this.interceptors?.onFailure(url, type, error)
                }
            }.throwIfCancelled().onFailure { e ->
                _statusMap.update { current ->
                    val errorStatus: PrefetchStatus = PrefetchStatus.Error(url, e)
                    current + (url to errorStatus)
                }
                this.interceptors?.onFailure(url, type, e)
            }
        }
    }

    private suspend fun downloadChunk(url: String, index: Int, totalSize: Long, chunkSize: Int): Boolean = getChunkLock(url, index).withLock {
        val start = index * chunkSize.toLong()
        if (start >= totalSize) return true

        val existingChunk = dbDao.getChunk(url, index)
        if (existingChunk?.isCompleted == true && SystemFileSystem.exists(Path(existingChunk.localFilePath))) {
            return true
        }

        interceptors?.onChunkStart(url, index)

        val end = min(start + chunkSize - 1, totalSize - 1)
        val expectedLength = end - start + 1

        chunkDir.ensureExists()

        val fileNameBase = sanitizeUrlToFilename(url)
        val tempPath = Path(chunkDir, "${fileNameBase}_chunk_$index.part")
        val finalPath = Path(chunkDir, "${fileNameBase}_chunk_$index.chunk")

        var lastError: Exception? = null
        repeat(config.maxRetries) { attempt ->
            runCatching {
                val response = httpClient.get(url) {
                    header(HttpHeaders.Range, "bytes=$start-$end")
                }

                val isCorrectStatus = response.status == HttpStatusCode.PartialContent ||
                        (response.status == HttpStatusCode.OK && start == 0L)

                if (isCorrectStatus) {
                    val channel: ByteReadChannel = response.bodyAsChannel()

                    try {
                        FileUtils.saveChannelToFile(
                            channel = channel,
                            path = tempPath,
                            expectedLength = expectedLength,
                            bufferSize = config.bufferSize
                        )
                    } finally {
                        if (response.status == HttpStatusCode.OK) {
                            channel.cancel(null)
                        }
                    }

                    // Move temp file to final path
                    FileUtils.atomicMove(tempPath, finalPath)

                    dbDao.insertChunk(
                        PrefetchChunkEntity(
                            url = url,
                            chunkIndex = index,
                            startByte = start,
                            endByte = end,
                            localFilePath = finalPath.toString(),
                            isCompleted = true,
                            downloadedAtMillis = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                    interceptors?.onChunkSuccess(url, index)
                    return true
                } else {
                    throw ChunkDownloadException(url, index, "HTTP ${response.status} for range bytes=$start-$end")
                }
            }.throwIfCancelled().onFailure { e ->
                lastError = e as Exception
                FileUtils.delete(tempPath)
                if (attempt < config.maxRetries - 1) delay((config.retryDelay.inWholeMilliseconds * (attempt + 1)).milliseconds)
            }
        }

        interceptors?.onChunkFailure(url, index, lastError ?: ChunkDownloadException(url, index, "Unknown error"))
        false
    }

    /**
     * Triggers a background download for a specific chunk if it's missing.
     * This is non-blocking and returns immediately.
     */
    fun prefetchChunk(url: String, index: Int) {
        managerScope.launch {
            val key = url to index
            
            // Check if already in progress to avoid flooding
            val isAlreadyDownloading = activeDownloadsMutex.withLock {
                if (activeChunkDownloads.contains(key)) true
                else {
                    activeChunkDownloads.add(key)
                    false
                }
            }
            if (isAlreadyDownloading) return@launch

            try {
                val metadata = getMetadataCached(url) ?: return@launch
                downloadChunk(url, index, metadata.totalSize, metadata.chunkSize)
            } finally {
                activeDownloadsMutex.withLock {
                    activeChunkDownloads.remove(key)
                }
            }
        }
    }

    /**
     * Gets a [ChunkMerger] for a specific URL.
     * If metadata is not present, it triggers an initial prefetch to resolve it.
     *
     * @param url The media URL.
     * @return A [ChunkMerger] capable of reading cached and network data.
     * @throws MetadataResolutionException If metadata cannot be resolved.
     */
    suspend fun getChunkMerger(url: String): ChunkMerger {
        val metadata = getMetadataCached(url) ?: run {
            executePrefetch(url, PrefetchMediaType.MP4, PrefetchStrategy.FirstNChunks(1))
            getMetadataCached(url) ?: throw MetadataResolutionException(url, "Failed to resolve metadata after prefetch attempt")
        }

        return ChunkMerger(
            totalSize = metadata.totalSize,
            contentType = metadata.contentType,
            chunkSize = metadata.chunkSize,
            getChunk = { index -> getCachedChunk(url, index) }
        )
    }

    /**
     * Fetches a specific range of bytes from the given URL.
     * This is used as a fallback when prefetch chunks are missing during playback.
     * Successfully fetched ranges that match chunk boundaries are automatically cached.
     *
     * @param url The media URL.
     * @param start The starting byte offset.
     * @param length The number of bytes to fetch.
     * @return The downloaded [ByteArray], or null if the request failed.
     */
    suspend fun fetchRange(url: String, start: Long, length: Int): ByteArray? {
        return runCatching {
            val response = httpClient.get(url) {
                header(HttpHeaders.Range, "bytes=$start-${start + length - 1}")
            }
            
            val isPartial = response.status == HttpStatusCode.PartialContent
            val isFull = response.status == HttpStatusCode.OK

            if (isPartial || (isFull && start == 0L)) {
                val channel = response.bodyAsChannel()
                val data = if (isPartial) {
                    // It's exactly the range we asked for
                    val buffer = ByteArray(length)
                    channel.readFully(buffer, 0, length)
                    buffer
                } else {
                    // It's the full file, we only want the first 'length' bytes
                    val buffer = ByteArray(length)
                    channel.readFully(buffer, 0, length)
                    channel.cancel(null) // Stop downloading the rest
                    buffer
                }
                
                val metadata = getMetadataCached(url)
                if (metadata != null) {
                    val chunkSize = metadata.chunkSize
                    // If we happened to fetch a full chunk, try to save it
                    if (start % chunkSize == 0L && data.size == chunkSize) {
                        val index = (start / chunkSize).toInt()
                        saveDataAsChunk(url, index, start, start + data.size - 1, data)
                    }
                }
                
                data
            } else {
                null
            }
        }.throwIfCancelled().getOrNull()
    }

    private suspend fun saveDataAsChunk(url: String, index: Int, start: Long, end: Long, data: ByteArray) {
        chunkDir.ensureExists()
        val finalPath = Path(chunkDir, "${sanitizeUrlToFilename(url)}_chunk_$index.chunk")
        
        runCatching {
            if (!SystemFileSystem.exists(finalPath)) {
                FileUtils.saveByteArrayToFile(data, finalPath)
                dbDao.insertChunk(
                    PrefetchChunkEntity(
                        url = url,
                        chunkIndex = index,
                        startByte = start,
                        endByte = end,
                        localFilePath = finalPath.toString(),
                        isCompleted = true,
                        downloadedAtMillis = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }.throwIfCancelled()
    }

    private suspend fun getCachedChunk(url: String, index: Int): PrefetchChunkEntity? {
        val chunk = dbDao.getChunk(url, index)
        if (chunk?.isCompleted == true && SystemFileSystem.exists(Path(chunk.localFilePath))) {
            return chunk
        }
        return null
    }

    private suspend fun clearCacheForUrl(url: String) {
        val chunks = dbDao.getAllChunksForUrl(url)
        chunks.forEach {
            FileUtils.delete(Path(it.localFilePath))
        }
        dbDao.deleteChunksForUrl(url)
        metadataCacheMutex.withLock {
            metadataCache.remove(url)
        }
    }

    private suspend fun pruneStaleCache() {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val expiredThreshold = currentTime - config.maxFileAge.inWholeMilliseconds

        // Delete individual old chunks instead of the whole file metadata
        dbDao.deleteOldChunks(expiredThreshold)

        // Prune metadata if it has no chunks left and is old
        val expiredMetadata = dbDao.getExpiredMetadata(expiredThreshold)
        expiredMetadata.forEach { metadata ->
            val chunks = dbDao.getAllChunksForUrl(metadata.url)
            if (chunks.isEmpty()) {
                dbDao.deleteMetadata(metadata.url)
                metadataCacheMutex.withLock {
                    metadataCache.remove(metadata.url)
                }
            }
        }

        // Also prune by total cache size
        pruneCacheBySize()
    }

    /**
     * Enforces the [PrefetchConfig.maxCacheSize] limit by deleting the oldest chunks (LRU).
     */
    private suspend fun pruneCacheBySize() {
        val currentSize = dbDao.getTotalCacheSize() ?: 0L
        if (currentSize <= config.maxCacheSize) return

        val oldestChunks = dbDao.getOldestChunks()
        val chunksToDelete = mutableListOf<PrefetchChunkEntity>()
        var sizeToFree = currentSize - config.maxCacheSize
        
        for (chunk in oldestChunks) {
            if (sizeToFree <= 0) break
            
            FileUtils.delete(Path(chunk.localFilePath))
            chunksToDelete.add(chunk)
            sizeToFree -= (chunk.endByte - chunk.startByte + 1)
        }

        if (chunksToDelete.isNotEmpty()) {
            dbDao.deleteChunks(chunksToDelete)
        }
    }

    /**
     * Deletes all cached files and clears the database entries for all prefetched media.
     */
    suspend fun clearAllCaches() {
        val allMetadata = dbDao.getAllMetadata()
        allMetadata.forEach {
            clearCacheForUrl(it.url)
            dbDao.deleteMetadata(it.url)
        }
        metadataCacheMutex.withLock {
            metadataCache.clear()
        }

        if (SystemFileSystem.exists(chunkDir)) {
            SystemFileSystem.list(chunkDir).forEach { FileUtils.delete(it) }
        }
        _statusMap.value = emptyMap()
    }
}
