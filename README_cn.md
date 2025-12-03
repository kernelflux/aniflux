# AniFlux

> 采用模块化架构和编译时注册的统一 Android 动画加载框架

[![Maven Central](https://img.shields.io/maven-central/v/com.kernelflux.mobile/aniflux-core.svg)](https://search.maven.org/search?q=g:com.kernelflux.mobile)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21-green.svg)](https://developer.android.com/about/versions/android-5.0)

**Languages**: [English](README.md) | [中文](README_cn.md)

## 简介

AniFlux 是一个强大的 Android 动画加载框架，灵感来源于 [Glide](https://github.com/bumptech/glide) 的设计理念。它提供统一的 API 来加载和管理多种动画格式（GIF、Lottie、SVGA、PAG、VAP），具备自动生命周期管理、智能缓存，以及自动 Loader 注册。

### 核心特性

- 🎨 **多格式支持**：GIF、Lottie、SVGA、PAG、VAP
- 🔌 **统一 API**：一套链式 API 适配所有格式
- 🔄 **生命周期感知**：自动处理 Activity/Fragment 生命周期
- ⏸️ **智能暂停/恢复**：页面不可见时自动暂停
- 💾 **智能缓存**：内存 + 磁盘缓存
- 🏗️ **模块化架构**：核心模块 + 格式模块
- ⚡ **自动注册**：通过 Gradle 插件自动注册 Loader

## 快速开始

### 安装

**方式一：一体化包（推荐，适用于大多数场景）**

```kotlin
dependencies {
    implementation("com.kernelflux.mobile:aniflux:1.1.1")
}
```

**方式二：模块化依赖（用于体积优化）**

```kotlin
// 在项目根目录的 build.gradle.kts
plugins {
    id("com.kernelflux.aniflux.register") version "1.0.0" apply false
}

// 在 app 模块的 build.gradle.kts
plugins {
    id("com.kernelflux.aniflux.register")
}

dependencies {
    // 核心模块（必需）
    implementation("com.kernelflux.mobile:aniflux-core:1.1.1")
    
    // 格式模块（按需添加）
    implementation("com.kernelflux.mobile:aniflux-gif:1.1.1")
    implementation("com.kernelflux.mobile:aniflux-lottie:1.1.1")
    implementation("com.kernelflux.mobile:aniflux-svga:1.1.1")
    implementation("com.kernelflux.mobile:aniflux-pag:1.1.1")
    implementation("com.kernelflux.mobile:aniflux-vap:1.1.1")
}
```

**注意**：
- 一体化包（`aniflux`）已包含所有预注册的 Loader，**不需要注册插件**
- 使用模块化依赖时，`com.kernelflux.aniflux.register` 插件是**必需的**，用于自动注册依赖中的 Loader

### 初始化

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AniFlux.init(this)
    }
}
```

### 基础用法

```kotlin
// 加载 GIF
AniFlux.with(context)
    .asGif()
    .load("https://example.com/animation.gif")
    .into(gifImageView)

// 加载 Lottie
AniFlux.with(context)
    .asLottie()
    .load("https://example.com/animation.json")
    .into(lottieAnimationView)

// 加载 SVGA
AniFlux.with(context)
    .asSVGA()
    .load("https://example.com/animation.svga")
    .into(svgaImageView)
```

## 架构设计

### 模块化设计

AniFlux 采用模块化架构，便于维护和灵活扩展：

```
aniflux-core          # 核心引擎、缓存、生命周期管理
├── aniflux-gif       # GIF 格式支持
├── aniflux-lottie    # Lottie 格式支持
├── aniflux-svga      # SVGA 格式支持
├── aniflux-pag       # PAG 格式支持
└── aniflux-vap       # VAP 格式支持
```

每个格式模块都是独立的，可按需引入，减少应用体积。

## 核心功能

### 统一链式 API

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

### 多种数据源

```kotlin
.load("https://example.com/animation.gif")  // URL
.load(File("/sdcard/animation.gif"))            // 文件
.load("asset://animations/loading.gif")         // Asset
.load(R.raw.animation)                           // 资源
.load(byteArray)                                 // 字节数组
.load(Uri.parse("content://..."))               // Uri
```

### 智能缓存

```kotlin
enum class AnimationCacheStrategy {
    NONE,        // 不缓存
    MEMORY_ONLY, // 仅内存
    DISK_ONLY,   // 仅磁盘
    BOTH         // 内存 + 磁盘（默认）
}
```

### 占位图替换

支持 SVGA、PAG 和 Lottie：

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

### 生命周期管理

自动处理 Activity/Fragment 生命周期：

```kotlin
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 自动绑定 Fragment 生命周期
        AniFlux.with(this)
            .asGif()
            .load(url)
            .into(gifImageView)
    }
}
```

## 支持的格式

| 格式 | 模块 | 特性 |
|------|------|------|
| GIF | `aniflux-gif` | 兼容性好，文件体积较大 |
| Lottie | `aniflux-lottie` | 矢量动画，体积小，质量高，支持占位图 |
| SVGA | `aniflux-svga` | 高性能，支持音频，支持占位图 |
| PAG | `aniflux-pag` | Adobe AE 导出，高性能，支持占位图 |
| VAP | `aniflux-vap` | 视频格式，支持透明度 |

## 高级用法

### 请求管理

```kotlin
val requestManager = AniFlux.with(context)

requestManager.pauseAllRequests()
requestManager.resumeRequests()
requestManager.clearRequests()
```

## 最佳实践

1. **选择合适的依赖方式**：大多数场景使用一体化包，需要体积优化时使用模块化依赖
2. **模块化依赖需要注册插件**：使用模块化依赖时，`com.kernelflux.aniflux.register` 插件是必需的
3. **生命周期感知**：使用 Fragment/Activity context 实现自动清理
4. **缓存策略**：根据使用场景选择合适的策略
5. **占位图替换**：在 SVGA/PAG/Lottie 中使用动态内容

## 许可证

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

## 贡献

欢迎贡献！请随时提交 Pull Request。

---

**AniFlux** - 让动画加载变得简单统一 🎉
