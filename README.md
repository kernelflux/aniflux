# AniFlux

> A unified Android animation loading framework with modular architecture and compile-time registration

[![Maven Central](https://img.shields.io/maven-central/v/com.kernelflux.mobile/aniflux-core.svg)](https://search.maven.org/search?q=g:com.kernelflux.mobile)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21-green.svg)](https://developer.android.com/about/versions/android-5.0)

**Languages**: [English](README.md) | [中文](README_cn.md)

## Overview

AniFlux is a powerful Android animation loading framework inspired by [Glide](https://github.com/bumptech/glide)'s design philosophy. It provides a unified API for loading and managing multiple animation formats (GIF, Lottie, SVGA, PAG, VAP) with automatic lifecycle management, smart caching, and automatic loader registration.

### Key Features

- 🎨 **Multi-format Support**: GIF, Lottie, SVGA, PAG, VAP
- 🔌 **Unified API**: One chain API for all formats
- 🔄 **Lifecycle-aware**: Automatic Activity/Fragment lifecycle management
- ⏸️ **Smart Pause/Resume**: Auto-pause when pages are invisible
- 💾 **Intelligent Caching**: Memory + disk cache
- 🏗️ **Modular Architecture**: Core + format-specific modules
- ⚡ **Auto-registration**: Automatic loader registration via Gradle plugin

## Quick Start

### Installation

**Option 1: All-in-one bundle (Recommended for most cases)**

```kotlin
dependencies {
    implementation("com.kernelflux.mobile:aniflux:1.1.1")
}
```

**Option 2: Modular dependencies (For size optimization)**

```kotlin
// In your project's build.gradle.kts (project level)
plugins {
    id("com.kernelflux.aniflux.register") version "1.1.1" apply false
}

// In your app's build.gradle.kts (module level)
plugins {
    id("com.kernelflux.aniflux.register")
}

dependencies {
    // Core module (required)
    implementation("com.kernelflux.mobile:aniflux-core:1.1.1")
    
    // Format modules (add as needed)
    implementation("com.kernelflux.mobile:aniflux-gif:1.1.1")
    implementation("com.kernelflux.mobile:aniflux-lottie:1.1.1")
    implementation("com.kernelflux.mobile:aniflux-svga:1.1.1")
    implementation("com.kernelflux.mobile:aniflux-pag:1.1.1")
    implementation("com.kernelflux.mobile:aniflux-vap:1.1.1")
}
```

**Note**: 
- The all-in-one bundle (`aniflux`) includes all loaders pre-registered, so the register plugin is **not required**
- For modular dependencies, the `com.kernelflux.aniflux.register` plugin is **required** to automatically register loaders from dependencies

### Initialize

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AniFlux.init(this)
    }
}
```

### Basic Usage

```kotlin
// Load GIF
AniFlux.with(context)
    .asGif()
    .load("https://example.com/animation.gif")
    .into(gifImageView)

// Load Lottie
AniFlux.with(context)
    .asLottie()
    .load("https://example.com/animation.json")
    .into(lottieAnimationView)

// Load SVGA
AniFlux.with(context)
    .asSVGA()
    .load("https://example.com/animation.svga")
    .into(svgaImageView)
```

## Architecture

### Modular Design

AniFlux follows a modular architecture for better maintainability and flexibility:

```
aniflux-core          # Core engine, caching, lifecycle management
├── aniflux-gif       # GIF format support
├── aniflux-lottie    # Lottie format support
├── aniflux-svga      # SVGA format support
├── aniflux-pag       # PAG format support
└── aniflux-vap       # VAP format support
```

Each format module is independent and can be included as needed, reducing app size.

## Core Features

### Unified Chain API

```kotlin
AniFlux.with(context)
    .asGif()
    .load(url)
    .size(200, 200)
    .cacheStrategy(AnimationCacheStrategy.BOTH)
    .repeatCount(3)
    .retainLastFrame(true)
    .autoPlay(true)
    .playListener(listener)
    .into(imageView)
```

### Multiple Data Sources

```kotlin
.load("https://example.com/animation.gif")  // URL
.load(File("/sdcard/animation.gif"))         // File
.load("asset://animations/loading.gif")       // Asset
.load(R.raw.animation)                        // Resource
.load(byteArray)                              // ByteArray
.load(Uri.parse("content://..."))            // Uri
```

### Smart Caching

```kotlin
enum class AnimationCacheStrategy {
    NONE,        // No cache
    MEMORY_ONLY, // Memory only
    DISK_ONLY,   // Disk only
    BOTH         // Memory + disk (default)
}
```

### Placeholder Replacement

Support for SVGA, PAG, and Lottie:

```kotlin
AniFlux.with(context)
    .asSVGA()
    .load(url)
    .placeholderReplacements {
        add("user_1", "https://example.com/user1.jpg")
        add("logo", R.drawable.logo)
    }
    .into(svgaImageView)
```

### Lifecycle Management

Automatically handles Activity/Fragment lifecycle:

```kotlin
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Automatically bound to Fragment lifecycle
        AniFlux.with(this)
            .asGif()
            .load(url)
            .into(gifImageView)
    }
}
```

## Supported Formats

| Format | Module | Features |
|--------|--------|----------|
| GIF | `aniflux-gif` | Good compatibility, larger file size |
| Lottie | `aniflux-lottie` | Vector animation, small size, high quality, placeholder support |
| SVGA | `aniflux-svga` | High performance, audio support, placeholder support |
| PAG | `aniflux-pag` | Adobe AE export, high performance, placeholder support |
| VAP | `aniflux-vap` | Video format, transparency support |

## Advanced Usage

### Request Management

```kotlin
val requestManager = AniFlux.with(context)

requestManager.pauseAllRequests()
requestManager.resumeRequests()
requestManager.clearRequests()
```

## Best Practices

1. **Choose the right dependency**: Use all-in-one bundle for simplicity, or modular dependencies for size optimization
2. **Apply register plugin for modular dependencies**: The `com.kernelflux.aniflux.register` plugin is required when using modular dependencies
3. **Lifecycle-aware**: Use Fragment/Activity context for automatic cleanup
4. **Cache strategy**: Choose appropriate strategy based on usage patterns
5. **Placeholder replacement**: Use for dynamic content in SVGA/PAG/Lottie

## License

```
Copyright 2025 KernelFlux

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

**AniFlux** - Making animation loading simple and unified 🎉
