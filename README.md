# AniFlux

> **A powerful Android animation loading framework that unifies loading and management of multiple animation formats**  
> **一个强大的 Android 动画加载框架，统一多种动画格式的加载和管理**

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21-green.svg)](https://developer.android.com/about/versions/android-5.0)

## 📖 Introduction / 简介

**English**: AniFlux is an Android animation loading framework inspired by [Glide](https://github.com/bumptech/glide)'s design philosophy. It provides a unified and concise API for loading and managing multiple animation formats.

**中文**: AniFlux 是一个专为 Android 设计的动画加载框架，灵感来源于 [Glide](https://github.com/bumptech/glide) 的设计理念，提供统一、简洁的 API 来加载和管理多种动画格式。

### 🎯 Core Values / 核心价值

- **🎨 Multi-format Support / 多格式支持**: Unified management of five mainstream animation formats: GIF, Lottie, SVGA, PAG, and VAP  
  统一管理 GIF、Lottie、SVGA、PAG、VAP 五种主流动画格式

- **🔌 Unified API / 统一 API**: One chain API for all animation formats, reducing learning curve  
  一套链式 API，适配所有动画格式，降低学习成本

- **🔄 Automatic Lifecycle Management / 自动生命周期管理**: Automatically handles Activity/Fragment lifecycle to prevent memory leaks  
  自动处理 Activity/Fragment 生命周期，避免内存泄漏

- **⏸️ Smart Pause/Resume / 智能暂停/恢复**: Automatically pauses animations when pages are invisible, saving CPU and battery  
  页面不可见时自动暂停动画，节省 CPU 和电池

- **💾 Smart Caching / 智能缓存**: Memory cache + disk cache for better loading performance  
  内存缓存 + 磁盘缓存，提升加载性能

- **📡 Multiple Data Sources / 多种数据源**: Supports network URL, local file, Asset, Resource, ByteArray, etc.  
  支持网络 URL、本地文件、Asset、Resource、ByteArray 等

- **🎵 Unified Callback Interface / 统一回调接口**: Unified playback listener compatible with different animation library callback semantics  
  统一的播放监听器，兼容不同动画库的回调语义

## 🚀 Quick Start / 快速开始

### Add Dependencies / 添加依赖

**Current Version / 当前版本** (All animation formats integrated / 所有动画格式已集成):

```gradle
dependencies {
    implementation 'com.kernelflux:aniflux:1.0.0'
}
```

> **Note / 注意**: The current version packages all animation formats (GIF, Lottie, SVGA, PAG, VAP) in one module. Simply add `aniflux` to use all formats.  
> If you need on-demand loading to reduce package size, modular refactoring is required (see below).  
> 当前版本将所有动画格式（GIF、Lottie、SVGA、PAG、VAP）打包在一个模块中，引入 `aniflux` 即可使用所有格式。  
> 如果需要按需引入以减少包体积，需要模块化改造（见下方说明）。

**Future Version / 未来版本** (Planned on-demand loading support / 计划支持按需引入):

```gradle
dependencies {
    // Core framework (required) / 核心框架（必需）
    implementation 'com.kernelflux:aniflux-core:1.0.0'
    
    // Add dependencies for animation formats as needed (optional) / 根据需要的动画格式添加对应依赖（可选）
    implementation 'com.kernelflux:aniflux-gif:1.0.0'      // GIF
    implementation 'com.kernelflux:aniflux-lottie:1.0.0'  // Lottie
    implementation 'com.kernelflux:aniflux-svga:1.0.0'    // SVGA
    implementation 'com.kernelflux:aniflux-pag:1.0.0'      // PAG
    implementation 'com.kernelflux:aniflux-vap:1.0.0'      // VAP
}
```

### Initialize / 初始化

Initialize in `Application` / 在 `Application` 中初始化:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AniFlux.init(this)
    }
}
```

### Basic Usage / 基础用法

```kotlin
// Load GIF animation / 加载 GIF 动画
AniFlux.with(context)
    .asGif()
    .load("https://example.com/animation.gif")
    .into(gifImageView)

// Load Lottie animation / 加载 Lottie 动画
AniFlux.with(context)
    .asLottie()
    .load("https://example.com/animation.json")
    .into(lottieAnimationView)

// Load SVGA animation / 加载 SVGA 动画
AniFlux.with(context)
    .asSVGA()
    .load("https://example.com/animation.svga")
    .into(svgaImageView)

// Load PAG animation / 加载 PAG 动画
AniFlux.with(context)
    .asPAG()
    .load("https://example.com/animation.pag")
    .into(pagImageView)

// Load VAP animation / 加载 VAP 动画
AniFlux.with(context)
    .asFile()  // VAP uses File type / VAP 使用 File 类型
    .load("https://example.com/animation.mp4")
    .into(vapImageView)
```

## 📚 Core Features / 核心功能

### 1. Unified Chain API / 统一的链式 API

AniFlux provides a concise chain API with the same calling method for all animation formats / AniFlux 提供简洁的链式 API，所有动画格式使用相同的调用方式:

```kotlin
AniFlux.with(context)
    .asGif()                          // Specify animation format / 指定动画格式
    .load(url)                        // Load resource / 加载资源
    .size(200, 200)                   // Set size / 设置尺寸
    .cacheStrategy(CacheStrategy.ALL) // Cache strategy / 缓存策略
    .repeatCount(3)                   // Loop count / 循环次数
    .autoPlay(true)                   // Auto play / 自动播放
    .playListener(playListener)       // Play listener / 播放监听
    .into(imageView)                  // Load into View / 加载到 View
```

### 2. Multiple Data Source Support / 多种数据源支持

```kotlin
// Network URL / 网络 URL
.load("https://example.com/animation.gif")

// Local file / 本地文件
.load(File("/sdcard/animation.gif"))

// Asset resource / Asset 资源
.load("asset://animations/loading.gif")

// Resource ID / Resource ID
.load(R.raw.animation)

// ByteArray / ByteArray
.load(byteArray)
```

### 3. Smart Caching Strategy / 智能缓存策略

```kotlin
enum class CacheStrategy {
    ALL,        // Cache all (memory + disk) / 缓存所有（内存 + 磁盘）
    NONE,       // No cache / 不缓存
    SOURCE,     // Cache source data only / 只缓存源数据
    RESULT      // Cache processed result only / 只缓存处理后的结果
}

// Usage example / 使用示例
.cacheStrategy(CacheStrategy.ALL)
.useDiskCache(true)  // Use disk cache? / 是否使用磁盘缓存
```

### 4. Unified Playback Listener / 统一的播放监听器

AniFlux provides a unified `AnimationPlayListener` interface compatible with all animation formats / AniFlux 提供统一的 `AnimationPlayListener` 接口，兼容所有动画格式:

```kotlin
AniFlux.with(context)
    .asGif()
    .load(url)
    .playListener(object : AnimationPlayListener {
        override fun onAnimationStart() {
            // Animation starts playing / 动画开始播放
        }
        
        override fun onAnimationEnd() {
            // Animation ends / 动画播放结束
        }
        
        override fun onAnimationRepeat() {
            // Animation loop repeats / 动画循环重复
        }
        
        override fun onAnimationCancel() {
            // Animation cancelled / 动画被取消
        }
        
        override fun onAnimationUpdate(currentFrame: Int, totalFrames: Int) {
            // Animation frame update (called every frame) / 动画帧更新（每帧回调）
        }
        
        override fun onAnimationFailed(error: Throwable?) {
            // Animation loading/playback failed / 动画加载/播放失败
        }
    })
    .into(imageView)
```

### 5. Automatic Lifecycle Management / 自动生命周期管理

AniFlux automatically handles Activity/Fragment lifecycle / AniFlux 自动处理 Activity/Fragment 的生命周期:

- **onStart()**: Automatically resume animation requests and playback / 自动恢复动画请求和播放
- **onStop()**: Automatically pause animation requests and playback / 自动暂停动画请求和播放
- **onDestroy()**: Automatically clean up all resources to prevent memory leaks / 自动清理所有资源，避免内存泄漏

```kotlin
// Usage in Fragment / 在 Fragment 中使用
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // AniFlux automatically binds Fragment lifecycle / AniFlux 会自动绑定 Fragment 的生命周期
        AniFlux.with(this)
            .asGif()
            .load(url)
            .into(gifImageView)
        // No manual lifecycle management needed! / 无需手动管理生命周期！
    }
}
```

### 6. Automatic Pause/Resume Mechanism / 自动暂停/恢复机制

AniFlux implements intelligent automatic pause/resume mechanism / AniFlux 实现了智能的自动暂停/恢复机制:

- **When page is invisible / 页面不可见时**: Automatically pause animation, stop rendering and callbacks / 自动暂停动画，停止渲染和回调
- **When page is visible / 页面可见时**: Automatically resume animation from paused position / 自动恢复动画，从暂停位置继续播放
- **Fragment visibility support / Fragment 可见性支持**: Works correctly in ViewPager2 + Fragment scenarios / 在 ViewPager2 + Fragment 场景下也能正确工作

```kotlin
// For special scenarios, manually control Fragment visibility / 对于特殊场景，可以手动控制 Fragment 可见性
svgaImageView.setFragmentVisible(false)  // Pause / 暂停
svgaImageView.setFragmentVisible(true)    // Resume / 恢复
```

### 7. Unified Repeat Count Semantics / 统一的循环次数语义

AniFlux unifies repeat count semantics for all animation formats / AniFlux 统一了所有动画格式的循环次数语义:

| User Setting / 用户设置 | Semantics / 语义 | GIF | Lottie | SVGA | PAG | VAP |
|---------|------|-----|--------|------|-----|-----|
| `repeatCount(-1)` | Infinite loop / 无限循环 | ✅ | ✅ | ✅ | ✅ | ✅ |
| `repeatCount(0)` | Infinite loop / 无限循环 | ✅ | ❌ | ✅ | ✅ | ✅ |
| `repeatCount(1)` | Play once / 播放1次 | ✅ | ✅ | ✅ | ✅ | ✅ |
| `repeatCount(3)` | Play 3 times / 播放3次 | ✅ | ✅ | ✅ | ✅ | ✅ |

> **Note / 注意**: Different animation libraries have different underlying implementations. AniFlux automatically handles conversions to ensure consistent behavior.  
> 不同动画库的底层实现不同，AniFlux 会自动处理转换，确保行为一致。

### 8. Request Priority and Timeout Control / 请求优先级和超时控制

```kotlin
.priority(Priority.HIGH)  // High priority / 高优先级
.timeout(30000L)          // 30 seconds timeout / 30秒超时
```

## 🏗️ Architecture Design / 架构设计

### Core Components / 核心组件

```
AniFlux (Singleton / 单例)
  ├── AnimationRequestManager (Request Manager / 请求管理器)
  │   ├── AnimationEngine (Loading Engine / 加载引擎)
  │   │   ├── AnimationJob (Loading Task / 加载任务)
  │   │   └── MemoryAnimationCache (Memory Cache / 内存缓存)
  │   ├── AnimationRequestTracker (Request Tracker / 请求跟踪器)
  │   └── AnimationLifecycle (Lifecycle Management / 生命周期管理)
  │
  ├── AnimationRequestBuilder (Request Builder / 请求构建器)
  │   ├── AnimationLoader (Loader / 加载器)
  │   │   ├── GifAnimationLoader
  │   │   ├── LottieAnimationLoader
  │   │   ├── SVGAAnimationLoader
  │   │   ├── PAGAnimationLoader
  │   │   └── VAPAnimationLoader
  │   └── AnimationDownloader (Downloader / 下载器)
  │
  └── AnimationTarget (Target View / 目标视图)
      ├── GifViewTarget
      ├── LottieViewTarget
      ├── SVGAViewTarget
      ├── PAGImageViewTarget
      └── VAPViewTarget
```

### Loading Flow / 加载流程

```
User calls into() / 用户调用 into()
    ↓
AnimationRequestBuilder builds request / AnimationRequestBuilder 构建请求
    ↓
AnimationEngine checks cache / AnimationEngine 检查缓存
    ├── Memory cache hit / 内存缓存命中 → Direct return / 直接返回
    ├── Disk cache hit / 磁盘缓存命中 → Load and return / 加载并返回
    └── Cache miss / 缓存未命中 → Create AnimationJob / 创建 AnimationJob
        ↓
    AnimationJob executes loading / AnimationJob 执行加载
        ├── Detect animation type / 检测动画类型
        ├── Select corresponding Loader / 选择对应的 Loader
        ├── Load from network/file/resource / 从网络/文件/资源加载
        ├── Parse animation data / 解析动画数据
        └── Return AnimationResource / 返回 AnimationResource
            ↓
    Set to Target View / 设置到 Target View
        ↓
    Automatically bind lifecycle listener / 自动绑定生命周期监听
        ↓
    Start playing animation / 开始播放动画
```

### Design Patterns / 设计模式

- **Builder Pattern / Builder 模式**: `AnimationRequestBuilder` provides chain API / `AnimationRequestBuilder` 提供链式 API
- **Strategy Pattern / Strategy 模式**: Different `AnimationLoader` implementations use different loading strategies / 不同的 `AnimationLoader` 实现不同的加载策略
- **Adapter Pattern / Adapter 模式**: `AnimationPlayListenerAdapter` adapts callback interfaces of different animation libraries / `AnimationPlayListenerAdapter` 适配不同动画库的回调接口
- **Observer Pattern / Observer 模式**: Lifecycle management and playback listening / 生命周期管理和播放监听
- **Factory Pattern / Factory 模式**: `AnimationRequestManagerRetriever` manages RequestManager creation / `AnimationRequestManagerRetriever` 管理 RequestManager 的创建

## 🔧 Advanced Usage / 高级用法

### Custom Configuration / 自定义配置

```kotlin
val options = AnimationOptions.create()
    .cacheStrategy(CacheStrategy.ALL)
    .useDiskCache(true)
    .repeatCount(3)
    .autoPlay(true)
    .priority(Priority.HIGH)
    .timeout(30000L)

AniFlux.with(context)
    .asGif()
    .load(url)
    .apply(options)
    .into(imageView)
```

### Manual Request Management / 手动管理请求

```kotlin
val requestManager = AniFlux.with(context)

// Pause all requests / 暂停所有请求
requestManager.pauseAllRequests()

// Resume all requests / 恢复所有请求
requestManager.resumeRequests()

// Clear all requests / 清除所有请求
requestManager.clearRequests()
```

### Custom Downloader / 自定义下载器

```kotlin
class CustomAnimationDownloader : AnimationDownloader {
    override fun download(context: Context, url: String): File {
        // Custom download logic / 自定义下载逻辑
        return cachedFile
    }
}

// Use custom downloader (need to configure during initialization) / 使用自定义下载器（需要在初始化时配置）
```

## 🎨 Supported Animation Formats / 支持的动画格式

### GIF
- **Library / 库**: android-gif-drawable
- **Format / 格式**: `.gif`
- **Features / 特点**: Good compatibility, large file size / 兼容性好，文件体积较大

### Lottie
- **Library / 库**: lottie-android
- **Format / 格式**: `.json`
- **Features / 特点**: Vector animation, small file size, high quality / 矢量动画，文件小，质量高

### SVGA
- **Library / 库**: SVGAPlayer-Android (Enhanced auto-pause feature / 已增强自动暂停功能)
- **Format / 格式**: `.svga`
- **Features / 特点**: High performance, audio support, small file size / 高性能，支持音频，文件小

### PAG
- **Library / 库**: libpag
- **Format / 格式**: `.pag`
- **Features / 特点**: Adobe After Effects export, high performance, powerful / Adobe After Effects 导出，高性能，功能强大

### VAP
- **Library / 库**: vap
- **Format / 格式**: `.mp4` (Special format / 特殊格式)
- **Features / 特点**: Video format, transparency support, small file size / 视频格式，支持透明度，文件小

## 🔍 Core Features / 核心特性

### 1. Unified Event Callbacks / 统一的事件回调

AniFlux unifies event callback semantics for all animation formats / AniFlux 统一了所有动画格式的事件回调语义:

```kotlin
// Consistent callback timing for all formats / 所有格式的回调时机一致
onAnimationStart()    // First playback starts / 首次开始播放
onAnimationRepeat()   // Each loop repeats (for repeatCount(3), called 2 times) / 每次循环重复（对于 repeatCount(3)，会回调 2 次）
onAnimationEnd()      // All loops completed / 所有循环播放完成
onAnimationCancel()   // Animation cancelled / 动画被取消
onAnimationUpdate()   // Frame update / 每帧更新
onAnimationFailed()   // Loading/playback failed / 加载/播放失败
```

### 2. Automatic Pause/Resume Mechanism / 自动暂停/恢复机制

AniFlux implements intelligent automatic pause mechanism, referencing LibPAG's implementation / AniFlux 实现了智能的自动暂停机制，参考 LibPAG 的实现:

- **Visibility Detection / 可见性检测**: Based on `isAttachedToWindow`, `isShown()`, `windowVisibility` / 基于 `isAttachedToWindow`、`isShown()`、`windowVisibility`
- **Fragment Visibility / Fragment 可见性**: Supports ViewPager2 + Fragment scenarios / 支持 `ViewPager2 + Fragment` 场景
- **State Saving / 状态保存**: Saves current frame and loop state when pausing / 暂停时保存当前帧和循环状态
- **Seamless Resume / 无缝恢复**: Resumes from paused position when resuming / 恢复时从暂停位置继续播放

### 3. Unified Repeat Count Handling / 循环次数统一处理

AniFlux automatically handles repeat count semantic differences across animation libraries / AniFlux 自动处理不同动画库的循环次数语义差异:

- **GIF**: `loopCount = 0` (infinite) or `N` (play N times) / `loopCount = 0`（无限）或 `N`（播放 N 次）
- **Lottie**: `repeatCount = INFINITE` (infinite) or `N` (repeat N times, total N+1 plays) / `repeatCount = INFINITE`（无限）或 `N`（重复 N 次，总播放 N+1 次）
- **SVGA**: `loops = 0` (infinite) or `N` (play N times) / `loops = 0`（无限）或 `N`（播放 N 次）
- **PAG**: `repeatCount = 0` (infinite) or `N` (play N times) / `repeatCount = 0`（无限）或 `N`（播放 N 次）
- **VAP**: `playLoop = Int.MAX_VALUE` (infinite) or `N` (play N times) / `playLoop = Int.MAX_VALUE`（无限）或 `N`（播放 N 次）

### 4. Frame Calculation / 帧数计算

AniFlux provides unified frame number access / AniFlux 提供了统一的帧数获取方式:

```kotlin
// Get in callback / 在回调中获取
playListener = object : AnimationPlayListener {
    override fun onAnimationUpdate(currentFrame: Int, totalFrames: Int) {
        val progress = currentFrame.toFloat() / totalFrames
        // Update UI with progress bar / 使用进度条更新 UI
    }
}
```

## 🛠️ Thread Pool Management / 线程池管理

AniFlux uses multiple thread pools to optimize performance / AniFlux 使用多线程池来优化性能:

- **SourceExecutor**: Handles network downloads and IO operations / 处理网络下载和 IO 操作
- **DiskCacheExecutor**: Handles disk cache read/write / 处理磁盘缓存读写
- **AnimationExecutor**: Handles animation parsing and rendering / 处理动画解析和渲染

## 💡 Best Practices / 最佳实践

### 1. Usage in Fragment / 在 Fragment 中使用

```kotlin
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ✅ Usage in Fragment, automatically binds lifecycle / 在 Fragment 中使用，自动绑定生命周期
        AniFlux.with(this)
            .asGif()
            .load(url)
            .into(gifImageView)
    }
}
```

### 2. Handling Visibility Changes / 处理可见性变化

```kotlin
// ✅ In ViewPager2 + Fragment scenarios, manually control Fragment visibility / 在 ViewPager2 + Fragment 场景下，手动控制 Fragment 可见性
override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    svgaImageView.setFragmentVisible(!hidden)
}
```

### 3. Memory Optimization / 内存优化

```kotlin
// ✅ Use lightweight configuration for low-memory devices / 低内存设备使用轻量级配置
val options = AnimationOptions.lowMemoryOptions()

AniFlux.with(context)
    .asGif()
    .load(url)
    .apply(options)
    .into(imageView)
```

### 4. Performance Optimization / 性能优化

```kotlin
// ✅ Use high-performance configuration for high-frequency scenarios / 高频场景使用高性能配置
val options = AnimationOptions.highPerformanceOptions()

AniFlux.with(context)
    .asGif()
    .load(url)
    .apply(options)
    .into(imageView)
```

## 📝 Notes / 注意事项

### 1. Repeat Count Semantics / 循环次数语义

Different animation libraries have different underlying implementations. AniFlux handles this uniformly / 不同动画库的底层实现不同，AniFlux 已统一处理:

- **GIF/Lottie/SVGA/PAG/VAP**: `repeatCount(N)` means total N plays / `repeatCount(N)` 都表示总播放 N 次
- **Callback Count / 回调次数**: For `repeatCount(3)`, `onAnimationRepeat()` is called 2 times / 对于 `repeatCount(3)`，`onAnimationRepeat()` 会回调 2 次

### 2. Memory Management / 内存管理

- AniFlux automatically manages memory cache, uses 1/8 of available memory by default / AniFlux 自动管理内存缓存，默认使用 1/8 的可用内存
- Adjust cache strategy via `AnimationOptions` / 可通过 `AnimationOptions` 调整缓存策略
- Automatically clears cache in `onTrimMemory()` / 在 `onTrimMemory()` 时会自动清理缓存

### 3. Lifecycle / 生命周期

- AniFlux automatically handles Activity/Fragment lifecycle / AniFlux 自动处理 Activity/Fragment 生命周期
- Automatically cleans up all resources in `onDestroy()` / 在 `onDestroy()` 时会自动清理所有资源
- No need to manually call `clear()` or `pause()` / 无需手动调用 `clear()` 或 `pause()`

## 🤝 Contributing / 贡献

Contributions are welcome! Please submit Issues and Pull Requests! / 欢迎提交 Issue 和 Pull Request！

## 📄 License / 许可证

This project is licensed under the Apache 2.0 License. / 本项目采用 Apache 2.0 许可证。

---

**AniFlux** - Making animation loading simple and unified 🎉  
**AniFlux** - 让动画加载变得简单统一 🎉
