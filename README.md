# AniFlux

> **A powerful Android animation loading framework that unifies loading and management of multiple animation formats**

[![Maven Central](https://img.shields.io/maven-central/v/com.kernelflux.mobile/aniflux.svg)](https://search.maven.org/artifact/com.kernelflux.mobile/aniflux)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21-green.svg)](https://developer.android.com/about/versions/android-5.0)

**Languages / 语言**: [English](README.md) | [中文](README_cn.md)

## 📖 Introduction

AniFlux is an Android animation loading framework inspired by [Glide](https://github.com/bumptech/glide)'s design philosophy. It provides a unified and concise API for loading and managing multiple animation formats, making animation integration simple and efficient.

### 🎯 Core Values

- **🎨 Multi-format Support**: Unified management of five mainstream animation formats: GIF, Lottie, SVGA, PAG, and VAP
- **🔌 Unified API**: One chain API for all animation formats, reducing learning curve
- **🔄 Automatic Lifecycle Management**: Automatically handles Activity/Fragment lifecycle to prevent memory leaks
- **⏸️ Smart Pause/Resume**: Automatically pauses animations when pages are invisible, saving CPU and battery
- **💾 Smart Caching**: Memory cache + disk cache for better loading performance
- **📡 Multiple Data Sources**: Supports network URL, local file, Asset, Resource, ByteArray, etc.
- **🎵 Unified Callback Interface**: Unified playback listener compatible with different animation library callback semantics
- **🖼️ Placeholder Replacement**: Dynamic image replacement for SVGA, PAG, and Lottie animations

## 🚀 Quick Start

### Add Dependencies

Add the AniFlux dependency to your `build.gradle`. The core library includes all animation format support (GIF, Lottie, SVGA, PAG, VAP).

**Kotlin DSL (build.gradle.kts)**:
```kotlin
dependencies {
    implementation("com.kernelflux.mobile:aniflux:1.0.4")
}
```

**Groovy DSL (build.gradle)**:
```groovy
dependencies {
    implementation 'com.kernelflux.mobile:aniflux:1.0.4'
}
```

> **Note**: The `aniflux` library includes all animation format libraries. You don't need to add them separately.

**Find the latest version**: [Maven Central](https://search.maven.org/search?q=g:com.kernelflux.mobile)

### Initialize

Initialize in `Application`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Basic initialization
        AniFlux.init(this)
        
        // Or with configuration (e.g., placeholder image loader)
        AniFlux.init(this) {
            setPlaceholderImageLoader(GlidePlaceholderImageLoader())
        }
    }
}
```

### Basic Usage

```kotlin
// Load GIF animation
AniFlux.with(context)
    .asGif()
    .load("https://example.com/animation.gif")
    .into(gifImageView)

// Load Lottie animation
AniFlux.with(context)
    .asLottie()
    .load("https://example.com/animation.json")
    .into(lottieAnimationView)

// Load SVGA animation
AniFlux.with(context)
    .asSVGA()
    .load("https://example.com/animation.svga")
    .into(svgaImageView)

// Load PAG animation
AniFlux.with(context)
    .asPAG()
    .load("https://example.com/animation.pag")
    .into(pagImageView)

// Load VAP animation
AniFlux.with(context)
    .asFile()
    .load("https://example.com/animation.mp4")
    .into(vapImageView)
```

## 📚 Core Features

### 1. Unified Chain API

AniFlux provides a concise chain API with the same calling method for all animation formats:

```kotlin
AniFlux.with(context)
    .asGif()                          // Specify animation format
    .load(url)                        // Load resource
    .size(200, 200)                   // Set size (optional)
    .cacheStrategy(AnimationCacheStrategy.BOTH)   // Cache strategy
    .repeatCount(3)                   // Loop count
    .retainLastFrame(true)            // Retain last frame
    .autoPlay(true)                   // Auto play
    .placeholderReplacements {        // Placeholder replacement (SVGA/PAG/Lottie)
        add("user_1", "https://example.com/user1.jpg")
        add("user_2", File("/sdcard/user2.jpg"))
    }
    .playListener(playListener)       // Play listener
    .into(imageView)                  // Load into View
```

### 2. Multiple Data Source Support

```kotlin
// Network URL
.load("https://example.com/animation.gif")

// Local file
.load(File("/sdcard/animation.gif"))

// Asset resource
.load("asset://animations/loading.gif")

// Resource ID
.load(R.raw.animation)

// ByteArray
.load(byteArray)

// Uri
.load(Uri.parse("content://..."))
```

### 3. Smart Caching Strategy

AniFlux provides flexible caching strategies for different scenarios:

```kotlin
enum class AnimationCacheStrategy {
    NONE,           // No cache (memory and disk)
    MEMORY_ONLY,    // Memory cache only
    DISK_ONLY,      // Disk cache only (memory not cached)
    BOTH            // Memory + disk cache (default)
}

// Usage
.cacheStrategy(AnimationCacheStrategy.BOTH)  // Default
```

**Caching Flow**:
1. **Memory Cache Check**: Check `activeResources` and `memoryCache`
2. **Disk Cache Check**: If enabled, check disk cache
3. **Network Download**: If cache miss, download and cache

### 4. Placeholder Replacement

AniFlux supports dynamic image replacement for SVGA, PAG, and Lottie animations. This feature allows you to replace placeholder images in animations with custom content at runtime.

**Setup**:

1. Implement `PlaceholderImageLoader` interface (e.g., using Glide, Coil, etc.):
```kotlin
class GlidePlaceholderImageLoader : PlaceholderImageLoader {
    override fun load(
        context: Context,
        source: Any,
        width: Int,
        height: Int,
        callback: PlaceholderImageLoadCallback
    ): PlaceholderImageLoadRequest {
        // Implement image loading logic
        // Support: String (URL), File, Uri, Int (Resource ID), "asset://xxx.jpg"
    }
    
    override fun cancel(request: PlaceholderImageLoadRequest) {
        // Cancel loading request
    }
}
```

2. Initialize with placeholder image loader:
```kotlin
AniFlux.init(this) {
    setPlaceholderImageLoader(GlidePlaceholderImageLoader())
}
```

3. Use placeholder replacement:
```kotlin
AniFlux.with(context)
    .asSVGA()
    .load("https://example.com/animation.svga")
    .placeholderReplacements {
        add("user_1", "https://example.com/user1.jpg")  // Remote image
        add("user_2", File("/sdcard/user2.jpg"))         // Local file
        add("logo", R.drawable.logo)                     // Resource ID
        add("avatar", "asset://avatar.jpg")              // Asset resource
    }
    .into(svgaImageView)
```

**Supported Formats**:
- ✅ **SVGA**: Uses `SVGADynamicEntity` to set dynamic images
- ✅ **PAG**: Uses `PAGFile.replaceImage()` to replace image layers
- ✅ **Lottie**: Uses `ImageAssetDelegate` to provide images dynamically

**Features**:
- Asynchronous loading (non-blocking)
- Lifecycle-aware (automatic cleanup)
- Request cancellation support
- Batch updates for better performance
- Safe error handling (graceful degradation)

### 5. Unified Playback Listener

AniFlux provides a unified `AnimationPlayListener` interface compatible with all animation formats:

```kotlin
AniFlux.with(context)
    .asGif()
    .load(url)
    .playListener(object : AnimationPlayListener {
        override fun onAnimationStart() {
            // Animation starts playing
        }
        
        override fun onAnimationEnd() {
            // Animation ends
        }
        
        override fun onAnimationRepeat() {
            // Animation loop repeats
        }
        
        override fun onAnimationCancel() {
            // Animation cancelled
        }
        
        override fun onAnimationUpdate(currentFrame: Int, totalFrames: Int) {
            // Animation frame update (called every frame)
        }
        
        override fun onAnimationFailed(error: Throwable?) {
            // Animation loading/playback failed
        }
    })
    .into(imageView)
```

### 6. Automatic Lifecycle Management

AniFlux automatically handles Activity/Fragment lifecycle:

- **onStart()**: Automatically resume animation requests and playback
- **onStop()**: Automatically pause animation requests and playback
- **onDestroy()**: Automatically clean up all resources to prevent memory leaks

```kotlin
// Usage in Fragment
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // AniFlux automatically binds Fragment lifecycle
        AniFlux.with(this)
            .asGif()
            .load(url)
            .into(gifImageView)
        // No manual lifecycle management needed!
    }
}
```

### 7. Automatic Pause/Resume Mechanism

AniFlux implements intelligent automatic pause/resume mechanism:

- **When page is invisible**: Automatically pause animation, stop rendering and callbacks
- **When page is visible**: Automatically resume animation from paused position
- **Fragment visibility support**: Works correctly in ViewPager2 + Fragment scenarios

```kotlin
// For special scenarios, manually control Fragment visibility
svgaImageView.setFragmentVisible(false)  // Pause
svgaImageView.setFragmentVisible(true)   // Resume
```

### 8. Retain Last Frame Configuration

AniFlux supports controlling whether to retain the last frame after animation completes:

```kotlin
AniFlux.with(context)
    .asGif()
    .load(url)
    .retainLastFrame(true)   // Retain the frame where animation stopped (default: true)
    .into(gifImageView)

// Or set to false to clear the frame
.retainLastFrame(false)  // Clear frame after animation ends
```

**Supported Formats**:
- ✅ **GIF**: Retains current stopped frame
- ✅ **Lottie**: Retains current stopped frame
- ✅ **SVGA**: Controlled via `fillMode` (Forward = retain, Clear = clear)
- ✅ **PAG**: Retains current stopped frame
- ✅ **VAP**: Controlled via `retainLastFrame` property

> **Note**: `retainLastFrame(true)` retains the **current stopped frame**, not necessarily the last frame of the animation. If the animation is paused or stopped in the middle, it will retain that frame.

### 9. Unified Repeat Count Semantics

AniFlux unifies repeat count semantics for all animation formats:

| User Setting | Semantics | GIF | Lottie | SVGA | PAG | VAP |
|---------|------|-----|--------|------|-----|-----|
| `repeatCount(-1)` | Infinite loop | ✅ | ✅ | ✅ | ✅ | ✅ |
| `repeatCount(0)` | Play once | ✅ | ❌ | ✅ | ✅ | ✅ |
| `repeatCount(1)` | Play once | ✅ | ✅ | ✅ | ✅ | ✅ |
| `repeatCount(3)` | Play 3 times | ✅ | ✅ | ✅ | ✅ | ✅ |

> **Note**: Different animation libraries have different underlying implementations. AniFlux automatically handles conversions to ensure consistent behavior.

### 10. Type Inference Support

AniFlux supports type inference when loading animations:

```kotlin
// Type inference based on View type
AniFlux.with(context)
    .load("https://example.com/animation.svga")
    .into(svgaImageView)  // Automatically inferred as SVGA

AniFlux.with(context)
    .load("https://example.com/animation.pag")
    .into(pagImageView)  // Automatically inferred as PAG
```

## 🎨 Supported Animation Formats

All animation formats are included in the `aniflux` library. No additional dependencies are required.

### GIF
- **Based on**: android-gif-drawable
- **Format**: `.gif`
- **Features**: Good compatibility, large file size

### Lottie
- **Based on**: lottie-android
- **Format**: `.json`
- **Features**: Vector animation, small file size, high quality
- **Placeholder Support**: ✅ Yes

### SVGA
- **Based on**: SVGAPlayer-Android (Enhanced auto-pause feature)
- **Format**: `.svga`
- **Features**: High performance, audio support, small file size
- **Placeholder Support**: ✅ Yes

### PAG
- **Based on**: libpag
- **Format**: `.pag`
- **Features**: Adobe After Effects export, high performance, powerful
- **Placeholder Support**: ✅ Yes

### VAP
- **Based on**: vap
- **Format**: `.mp4` (Special format)
- **Features**: Video format, transparency support, small file size
- **Placeholder Support**: ❌ No

## 🔧 Advanced Usage

### Custom Configuration

```kotlin
val options = AnimationOptions.create()
    .cacheStrategy(AnimationCacheStrategy.BOTH)
    .repeatCount(3)
    .retainLastFrame(true)
    .autoPlay(true)

AniFlux.with(context)
    .asGif()
    .load(url)
    .apply(options)
    .into(imageView)
```

### Manual Request Management

```kotlin
val requestManager = AniFlux.with(context)

// Pause all requests
requestManager.pauseAllRequests()

// Resume all requests
requestManager.resumeRequests()

// Clear all requests
requestManager.clearRequests()
```

### Custom Placeholder Image Loader

```kotlin
// Implement PlaceholderImageLoader interface
class MyPlaceholderImageLoader : PlaceholderImageLoader {
    override fun load(
        context: Context,
        source: Any,
        width: Int,
        height: Int,
        callback: PlaceholderImageLoadCallback
    ): PlaceholderImageLoadRequest {
        // Your image loading logic (e.g., using Glide, Coil, etc.)
        // Support: String (URL), File, Uri, Int (Resource ID), "asset://xxx.jpg"
    }
    
    override fun cancel(request: PlaceholderImageLoadRequest) {
        // Cancel loading request
    }
}

// Initialize with custom loader
AniFlux.init(this) {
    setPlaceholderImageLoader(MyPlaceholderImageLoader())
}
```

## 💡 Best Practices

### 1. Usage in Fragment

```kotlin
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ✅ Usage in Fragment, automatically binds lifecycle
        AniFlux.with(this)
            .asGif()
            .load(url)
            .into(gifImageView)
    }
}
```

### 2. Handling Visibility Changes

```kotlin
// ✅ In ViewPager2 + Fragment scenarios, manually control Fragment visibility
override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    svgaImageView.setFragmentVisible(!hidden)
}
```

### 3. Memory Optimization

```kotlin
// ✅ Use appropriate cache strategy for low-memory devices
.cacheStrategy(AnimationCacheStrategy.DISK_ONLY)
```

### 4. Performance Optimization

- Use placeholder replacement for dynamic content
- Enable caching for frequently accessed animations
- Use appropriate cache strategy based on usage patterns

## 📝 Notes

### 1. Repeat Count Semantics

Different animation libraries have different underlying implementations. AniFlux handles this uniformly:

- **GIF/Lottie/SVGA/PAG/VAP**: `repeatCount(N)` means total N plays
- **Callback Count**: For `repeatCount(3)`, `onAnimationRepeat()` is called 2 times

### 2. Memory Management

- AniFlux automatically manages memory cache, uses 1/8 of available memory by default
- Adjust cache strategy via `AnimationOptions`
- Automatically clears cache in `onTrimMemory()`

### 3. Lifecycle

- AniFlux automatically handles Activity/Fragment lifecycle
- Automatically cleans up all resources in `onDestroy()`
- No need to manually call `clear()` or `pause()`

### 4. Placeholder Replacement

- Only supported for SVGA, PAG, and Lottie formats
- Requires implementing `PlaceholderImageLoader` interface
- Asynchronous loading with automatic lifecycle management
- Safe error handling (graceful degradation)

## 🤝 Contributing

Contributions are welcome! Please submit Issues and Pull Requests!

## 📄 License

This project is licensed under the Apache 2.0 License.

---

**AniFlux** - Making animation loading simple and unified 🎉

