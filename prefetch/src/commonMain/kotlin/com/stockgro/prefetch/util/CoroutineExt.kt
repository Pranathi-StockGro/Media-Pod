package com.stockgro.prefetch.util

import kotlinx.coroutines.CancellationException

/**
 * Rethrows [CancellationException] if the [Result] is a failure due to cancellation.
 * This is useful when using [runCatching] to avoid swallowing cancellation.
 */
inline fun <T> Result<T>.throwIfCancelled(): Result<T> {
    val exception = exceptionOrNull()
    if (exception is CancellationException) {
        throw exception
    }
    return this
}
