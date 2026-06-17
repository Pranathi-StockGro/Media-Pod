package com.stockgro.mediapod.enums

/** Controls how the memory and disk caches are consulted. */
enum class CachePolicy {
    /** Read from cache; write results to cache (default). */
    ENABLED,

    /** Bypass both caches — always fetch from source. */
    DISABLED,

    /** Read from cache; do **not** write new results. */
    READ_ONLY,

    /** Skip reading cache; write results after fetching. */
    WRITE_ONLY,
}