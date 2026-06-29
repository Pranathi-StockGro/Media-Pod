package com.stockgro.prefetch.exception

sealed class PrefetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

class MetadataResolutionException(url: String, message: String) : 
    PrefetchException("Failed to resolve metadata for $url: $message")

class ChunkDownloadException(url: String, index: Int, message: String, cause: Throwable? = null) : 
    PrefetchException("Failed to download chunk $index for $url: $message", cause)

class InvalidConfigException(message: String) : PrefetchException(message)

class FileOperationException(message: String, cause: Throwable? = null) : 
    PrefetchException(message, cause)
