package com.stockgro.mediapod

import com.stockgro.mediapod.ImageLoaderProvider.default
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet

/**
 * Singleton registry for the active [ImageLoader] implementation.
 *
 * Set the default loader once at app startup:
 * ```kotlin
 * // In Application.onCreate() or your DI module
 * ImageLoaderProvider.setDefault(CoilImageLoader(context))
 * ```
 *
 * Then consume it anywhere without threading the loader through call sites:
 * ```kotlin
 * imageView.load("https://example.com/photo.jpg")
 * ```
 */
object ImageLoaderProvider {

    // Holds either an ImageLoader, an ImageLoaderFactory, or null.
    // Using Any? allows us to pass a lazy factory or the fully realized instance atomically.
    private val reference = atomic<Any?>(null)

    /**
     * Get the singleton [ImageLoader].
     * If an ImageLoaderFactory was supplied, it will lazily initialize here thread-safely.
     */
    val default: ImageLoader
        get() = when (val value = reference.value) {
            is ImageLoader -> value
            is ImageLoaderFactory -> initializeLazyLoader(value)
            else -> error(
                "No default ImageLoader or Factory set. " +
                        "Call ImageLoaderProvider.setFactory(...) or setDefault(...) during app initialization."
            )
        }

    /**
     * Register a pre-instantiated [ImageLoader] as the process-wide default.
     * Throws an exception if an ImageLoader has already been created/set.
     */
    fun setDefault(loader: ImageLoader) {
        val currentValue = reference.value
        if (currentValue is ImageLoader) {
            error("The singleton image loader has already been initialized and cannot be overwritten safely.")
        }

        // Atomically replace whatever was there (null or factory) with the explicit loader
        reference.value = loader
    }

    /**
     * Set a [ImageLoaderFactory] that will be used to lazily create the [ImageLoader]
     * the very first time [default] is accessed.
     */
    fun setFactory(factory: ImageLoaderFactory) {
        val currentValue = reference.value
        if (currentValue is ImageLoader) {
            error("Cannot set factory: The singleton image loader has already been instantiated.")
        }

        // Safely exchange the value if it hasn't changed
        reference.compareAndSet(currentValue, factory)
    }

    /**
     * Returns true if a default loader or factory has already been registered.
     */
    val isInitialized: Boolean
        get() = reference.value != null

    /**
     * Reset the provider state back to null (highly useful for unit/UI tests).
     */
    fun reset() {
        reference.value = null
    }

    /**
     * Handles the lock-free, atomic creation of the ImageLoader from a factory.
     * Guarantees that the factory's create() function is invoked at most once.
     */
    private fun initializeLazyLoader(factory: ImageLoaderFactory): ImageLoader {
        var createdLoader: ImageLoader? = null

        return reference.updateAndGet { current ->
            when (current) {
                is ImageLoader -> current
                (createdLoader != null) -> createdLoader // Handled on fallback loop
                else -> {
                    // Invoke the factory out-of-loop safely, then save it
                    factory.create().also { createdLoader = it }
                }
            }
        } as ImageLoader
    }
}

/**
 * Factory interface for constructing [ImageLoader] instances lazily.
 */
fun interface ImageLoaderFactory {
    fun create(): ImageLoader
}