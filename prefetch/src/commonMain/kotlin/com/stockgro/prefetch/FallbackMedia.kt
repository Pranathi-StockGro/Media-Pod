package com.stockgro.prefetch

/**
 * Platform-agnostic fallback container.
 */
sealed interface FallbackMedia {
    data class BundledAsset(val assetName: String) : FallbackMedia
    data class PlatformResource(val resId: Int) : FallbackMedia
}