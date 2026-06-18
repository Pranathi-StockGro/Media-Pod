# MediaPod

MediaPod is a flexible, engine-agnostic image loading library for Kotlin Multiplatform (KMP). It provides a unified API for loading images while allowing you to choose the underlying engine (Coil or Glide).

---

## 🚀 Getting Started

### 1. Dependency Configuration

Add the relevant MediaPod modules to your `build.gradle.kts`:

#### For KMP Projects (Common Main)
Use the Coil-based implementation for cross-platform support.
```kotlin
commonMain.dependencies {
    implementation("com.stockgro.mediapod:mediapod-core:1.0.0")
    implementation("com.stockgro.mediapod:mediapod-coil:1.0.0")
}
```

#### For Android-Only Projects
You can choose between Coil or Glide.
```kotlin
// Option A: Coil (KMP Friendly)
implementation("com.stockgro.mediapod:mediapod-coil:1.0.0")

// Option B: Glide (Android Only)
implementation("com.stockgro.mediapod:mediapod-glide:1.0.0")
```

> [!IMPORTANT]
> **Glide can only be used for Android projects.** For iOS or multiplatform targets, use the Coil implementation.
>
> **⚠️ CRITICAL: Either Glide or Coil can be used. Using both simultaneously is NOT possible because the Image Loader is a singleton instance and cannot hold two different engine instances at once.**

---

## 🛠️ Initialization

You must initialize the `ImageLoaderProvider` once at application startup (e.g., in `Application.onCreate()` for Android).

### Using Coil (KMP/Android)
```kotlin
// Android
ImageLoaderProvider.setDefault(CoilImageLoaderImpl(context))

// iOS
ImageLoaderProvider.setDefault(CoilImageLoaderImpl())
```

### Using Glide (Android Only)
```kotlin
ImageLoaderProvider.setDefault(GlideImageLoaderImpl(context))
```

### Lazy Initialization (Recommended)
To avoid initializing the loader until it's actually needed, use a factory:
```kotlin
ImageLoaderProvider.setFactory {
    CoilImageLoaderImpl(context)
}
```

---

## 📸 Image Request

The `ImageRequest` class describes what to load and how to display it. Use the `Builder` to construct a request.

### Basic Usage
```kotlin
val request = ImageRequest.Builder("https://example.com/image.jpg")
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error_image)
    .circleCrop()
    .crossfade(true)
    .build()

// Load via the provider
ImageLoaderProvider.default.enqueue(request)
```

### Request Parameters
| Parameter | Description |
| :--- | :--- |
| `data` | The image source (URL String, ByteArray, File, or Resource ID). |
| `placeholder(resId)` | Drawable to show while the image is loading. |
| `error(resId)` | Drawable to show if the request fails. |
| `fallback(resId)` | Drawable to show if the data source is null. |
| `thumbnail(data)` | Secondary data (usually low-res) to show as a placeholder. |
| `size(width, height)` | Specific dimensions to decode the image at. |
| `memoryCachePolicy` | `ENABLED`, `DISABLED`, `READ_ONLY`, or `WRITE_ONLY`. |
| `diskCachePolicy` | Same as memory cache policy, applied to disk. |
| `crossfade(durationMs)` | Enables a crossfade transition (default: 300ms). |
| `transformations` | Apply effects like `circleCrop()` or `roundedCorners(radius)`. |
| `headers(map)` | Custom HTTP headers for network requests. |

---

## ⚙️ Image Loader Configuration

You can tune the behavior of the underlying engine using `ImageLoaderConfig`.

```kotlin
val config = ImageLoaderConfig.Builder()
    .memoryCache {
        maxSizePercent(0.25) // Use 25% of available RAM
    }
    .diskCache {
        maxSizeBytes(100L * 1024 * 1024) // 100 MB
        directory("/custom/cache/path")
    }
    .network {
        connectTimeoutMillis(15_000)
        readTimeoutMillis(30_000)
    }
    .respectCacheHeaders(true)
    .build()

// Apply to the loader
ImageLoaderProvider.default.configure(config)
```

---

## 🔄 Initialization Functions Summary

- **`ImageLoaderProvider.setDefault(loader)`**: Sets the global instance immediately. Throws an error if already initialized.
- **`ImageLoaderProvider.setFactory(factory)`**: Registers a lambda to create the loader lazily upon first access.
- **`ImageLoaderProvider.isInitialized`**: Checks if the loader is ready.
- **`ImageLoaderProvider.reset()`**: Clears the singleton (mainly for testing).

---

## 🏗 Project Structure

This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application.
* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - [androidMain](./shared/src/androidMain/kotlin) and [iosMain](./shared/src/iosMain/kotlin) are for platform-specific code.
