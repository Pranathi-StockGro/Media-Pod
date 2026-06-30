package com.stockgro.mediapod.utils

import kotlin.math.min

object ImageKitUrlTransformer {

    fun transform(
        url: String?,
        widthPx: Int?,
        heightPx: Int?,
        deviceDpr: Float
    ): String {
        if (url == null) return ""
        if (!url.contains("cdn.stockgro.com")) return url

        val cappedDpr = min(deviceDpr, 2.0f)

        val transformRule = buildString {
            if (widthPx != null && widthPx > 0) {
                val baseDpWidth = (widthPx / deviceDpr).toInt()
                append("w-$baseDpWidth,")
            }

            if (heightPx != null && heightPx > 0) {
                val baseDpHeight = (heightPx / deviceDpr).toInt()
                append("h-$baseDpHeight,")
            }

            if ((widthPx != null && widthPx > 0) || (heightPx != null && heightPx > 0)) {
                append("dpr-$cappedDpr,")
            }

            append("f-webp,q-80")
        }

        // Simple utility to append query parameters without relying on external URLBuilders
        return appendQueryParameter(url, "tr", transformRule)
    }

    private fun appendQueryParameter(url: String, key: String, value: String): String {
        val separator = if (url.contains("?")) "&" else "?"
        return "$url$separator$key=$value"
    }
}