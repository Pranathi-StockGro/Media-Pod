package com.stockgro.mediapod.enums

//enum class RequestPriority { LOW, NORMAL, HIGH }
enum class DataSource {

    /**
     * Represents an [com.stockgro.mediapod.ImageLoader]'s memory cache.
     *
     * This is a special data source as it means the request was
     * short-circuited and skipped the full image pipeline.
     */
    MEMORY_CACHE,

    /**
     * Represents an in-memory data source (e.g. `Bitmap`, `ByteBuffer`).
     */
    MEMORY,

    /**
     * Represents a disk-based data source (e.g. `DrawableRes`, `File`).
     */
    DISK,

    /**
     * Represents a network-based data source (e.g. `Url`).
     */
    NETWORK,
}