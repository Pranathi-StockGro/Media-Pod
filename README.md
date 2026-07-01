# MediaPod

MediaPod is a flexible, engine-agnostic image loading library for Kotlin Multiplatform (KMP). It provides a unified API for loading images while allowing you to choose the underlying engine (Coil or Glide).

---

## 🚀 Getting Started

### 1. Dependency Configuration

Add the relevant MediaPod modules to your `build.gradle.kts`:

#### For KMP Projects (Compose Multiplatform)
```kotlin
commonMain.dependencies {
    implementation("com.stockgro.mediapod:coil-compose:1.0.0")
}
```

#### For Android Projects
You can choose between Coil or Glide engines.

**Coil-based (KMP Friendly):**
```kotlin
// For Jetpack Compose
implementation("com.stockgro.mediapod:coil-compose:1.0.0")
// For Android Views (XML)
implementation("com.stockgro.mediapod:coil-view:1.0.0")
```

**Glide-based (Android Only):**
```kotlin
// For Jetpack Compose
implementation("com.stockgro.mediapod:glide-compose:1.0.0")
// For Android Views (XML)
implementation("com.stockgro.mediapod:glide-view:1.0.0")
```

> [!IMPORTANT]
> **Glide can only be used for Android projects.** For iOS or multiplatform targets, use the Coil implementation.
>
> **⚠️ CRITICAL: Either Glide or Coil can be used. Using both simultaneously is NOT possible because the Image Loader is a singleton instance and cannot hold two different engine instances at once.**

---

## 🛠️ Initialization

MediaPod provides platform-specific helper functions to initialize the `ImageLoaderProvider` with your desired configuration.

### KMP / Compose Multiplatform (Coil)
Use `setupCoilImageLoader` in your `@Composable` entry point:
```kotlin
setupCoilImageLoader(
    ImageLoaderConfig.Builder()
        .memoryCache { maxSizePercent(0.25) }
        .diskCache { maxSizeBytes(100L * 1024 * 1024) }
        .build()
)
```

### Android Jetpack Compose (Glide)
Use `setupGlideImageLoader` in your `@Composable`:
```kotlin
setupGlideImageLoader(
    config = ImageLoaderConfig.Builder().build()
)
```

### Android Views / XML
Initialize the loader in your `Activity` or `Application` class:
```kotlin
// Using Coil
this.setupCoilImageLoader(config)

// OR Using Glide
this.setupGlideImageLoader(config)
```

---

## 📸 Usage

### Jetpack Compose / Compose Multiplatform
Use the `MPImage` composable to display images.

```kotlin
MPImage(
    data = "https://example.com/image.jpg",
    contentDescription = "Sample Image",
    modifier = Modifier.size(150.dp).clip(CircleShape),
    placeholder = { CircularProgressIndicator() },
    error = { Text("Failed to load") },
    requestBuilder = {
        crossfade(true)
        circleCrop()
    }
)
```

### Android Views (XML)
Use the `load` extension function for `ImageView`.

```kotlin
imageView.load("https://example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.ic_placeholder)
    error(R.drawable.ic_error)
    transformations(Transformation.CircleCrop)
}
```

---

## ⚙️ Image Loader Configuration

Tweak the behavior of the underlying engine using `ImageLoaderConfig`.

```kotlin
val config = ImageLoaderConfig.Builder()
    .memoryCache {
        enabled(true)
        maxSizePercent(0.25) // Use 25% of available RAM
    }
    .diskCache {
        enabled(true)
        maxSizeBytes(100L * 1024 * 1024) // 100 MB
    }
    .network {
        connectTimeoutMillis(10_000)
        readTimeoutMillis(30_000)
    }
    .respectCacheHeaders(true)
    .build()
```

---

## 🧪 Image Request Parameters

The `ImageRequest.Builder` allows fine-grained control over each load:

| Parameter | Description |
| :--- | :--- |
| `data` | The image source (URL String, ByteArray, File, or Resource ID). |
| `placeholder(resId)` | Drawable resource to show while loading. |
| `error(resId)` | Drawable resource to show on failure. |
| `fallback(resId)` | Drawable resource to show if data is null. |
| `thumbnail(data)` | Secondary data to show as a placeholder. |
| `size(width, height)` | Specific dimensions to decode at. |
| `memoryCachePolicy` | `ENABLED`, `DISABLED`, `READ_ONLY`, or `WRITE_ONLY`. |
| `diskCachePolicy` | Same as above, for disk. |
| `crossfade(enabled/ms)` | Enable crossfade transition (default 300ms). |
| `circleCrop()` | Transform image into a circle. |
| `roundedCorners(px)` | Apply rounded corners transformation. |
| `headers(map)` | Custom HTTP headers. |

---

## 🏗 Project Structure

* **`mediapod-core`**: Common engine-agnostic contracts.
* **`mediapod-coil` / `mediapod-glide`**: Engine implementations.
* **`mediapod-compose`**: `MPImage` for Compose Multiplatform.
* **`coil-compose` / `glide-compose`**: Compose-specific setup helpers.
* **`coil-view` / `glide-view`**: Android View-specific setup helpers and `load` extensions.
