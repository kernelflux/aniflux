# AniFlux

> **一个强大的 Android 动画加载框架，统一多种动画格式的加载和管理**

[![Maven Central](https://img.shields.io/maven-central/v/com.kernelflux.mobile/aniflux.svg)](https://search.maven.org/artifact/com.kernelflux.mobile/aniflux)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21-green.svg)](https://developer.android.com/about/versions/android-5.0)

**Languages / 语言**: [English](README.md) | [中文](README_cn.md)

## 📖 简介

AniFlux 是一个专为 Android 设计的动画加载框架，灵感来源于 [Glide](https://github.com/bumptech/glide) 的设计理念，提供统一、简洁的 API 来加载和管理多种动画格式，让动画集成变得简单高效。

### 🎯 核心价值

- **🎨 多格式支持**: 统一管理 GIF、Lottie、SVGA、PAG、VAP 五种主流动画格式
- **🔌 统一 API**: 一套链式 API，适配所有动画格式，降低学习成本
- **🔄 自动生命周期管理**: 自动处理 Activity/Fragment 生命周期，避免内存泄漏
- **⏸️ 智能暂停/恢复**: 页面不可见时自动暂停动画，节省 CPU 和电池
- **💾 智能缓存**: 内存缓存 + 磁盘缓存，提升加载性能
- **📡 多种数据源**: 支持网络 URL、本地文件、Asset、Resource、ByteArray 等
- **🎵 统一回调接口**: 统一的播放监听器，兼容不同动画库的回调语义
- **🖼️ 占位图替换**: 支持 SVGA、PAG、Lottie 动画的动态图片替换

## 🚀 快速开始

### 添加依赖

在 `build.gradle` 中添加 AniFlux 依赖。核心库已包含所有动画格式支持（GIF、Lottie、SVGA、PAG、VAP）。

**Kotlin DSL (build.gradle.kts)**:
```kotlin
dependencies {
    implementation("com.kernelflux.mobile:aniflux:1.0.6")
}
```

**Groovy DSL (build.gradle)**:
```groovy
dependencies {
    implementation 'com.kernelflux.mobile:aniflux:1.0.6'
}
```

> **注意**: `aniflux` 库已包含所有动画格式库，无需单独添加。

**查找最新版本**: [Maven Central](https://search.maven.org/search?q=g:com.kernelflux.mobile)

### 初始化

在 `Application` 中初始化:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 基础初始化
        AniFlux.init(this)
        
        // 或带配置初始化（例如：占位图加载器）
        AniFlux.init(this) {
            setPlaceholderImageLoader(GlidePlaceholderImageLoader())
        }
    }
}
```

### 基础用法

```kotlin
// 加载 GIF 动画
AniFlux.with(context)
    .asGif()
    .load("https://example.com/animation.gif")
    .into(gifImageView)

// 加载 Lottie 动画
AniFlux.with(context)
    .asLottie()
    .load("https://example.com/animation.json")
    .into(lottieAnimationView)

// 加载 SVGA 动画
AniFlux.with(context)
    .asSVGA()
    .load("https://example.com/animation.svga")
    .into(svgaImageView)

// 加载 PAG 动画
AniFlux.with(context)
    .asPAG()
    .load("https://example.com/animation.pag")
    .into(pagImageView)

// 加载 VAP 动画
AniFlux.with(context)
    .asFile()
    .load("https://example.com/animation.mp4")
    .into(vapImageView)
```

## 📚 核心功能

### 1. 统一的链式 API

AniFlux 提供简洁的链式 API，所有动画格式使用相同的调用方式:

```kotlin
AniFlux.with(context)
    .asGif()                          // 指定动画格式
    .load(url)                        // 加载资源
    .size(200, 200)                   // 设置尺寸（可选）
    .cacheStrategy(AnimationCacheStrategy.BOTH)   // 缓存策略
    .repeatCount(3)                   // 循环次数
    .retainLastFrame(true)            // 保留最后一帧
    .autoPlay(true)                   // 自动播放
    .placeholderReplacements {        // 占位图替换（SVGA/PAG/Lottie）
        add("user_1", "https://example.com/user1.jpg")
        add("user_2", File("/sdcard/user2.jpg"))
    }
    .playListener(playListener)       // 播放监听
    .into(imageView)                  // 加载到 View
```

### 2. 多种数据源支持

```kotlin
// 网络 URL
.load("https://example.com/animation.gif")

// 本地文件
.load(File("/sdcard/animation.gif"))

// Asset 资源
.load("asset://animations/loading.gif")

// Resource ID
.load(R.raw.animation)

// ByteArray
.load(byteArray)

// Uri
.load(Uri.parse("content://..."))
```

### 3. 智能缓存策略

AniFlux 为不同场景提供灵活的缓存策略:

```kotlin
enum class AnimationCacheStrategy {
    NONE,           // 不缓存（内存和磁盘都不缓存）
    MEMORY_ONLY,    // 仅内存缓存
    DISK_ONLY,      // 仅磁盘缓存（内存不缓存）
    BOTH            // 内存 + 磁盘缓存（默认）
}

// 使用
.cacheStrategy(AnimationCacheStrategy.BOTH)  // 默认
```

**缓存流程**:
1. **内存缓存检查**: 检查 `activeResources` 和 `memoryCache`
2. **磁盘缓存检查**: 如果启用，检查磁盘缓存
3. **网络下载**: 缓存未命中时，下载并缓存

### 4. 占位图替换

AniFlux 支持 SVGA、PAG、Lottie 动画的动态图片替换功能。此功能允许您在运行时将动画中的占位图片替换为自定义内容。

**设置步骤**:

1. 实现 `PlaceholderImageLoader` 接口（例如：使用 Glide、Coil 等）:
```kotlin
class GlidePlaceholderImageLoader : PlaceholderImageLoader {
    override fun load(
        context: Context,
        source: Any,
        width: Int,
        height: Int,
        callback: PlaceholderImageLoadCallback
    ): PlaceholderImageLoadRequest {
        // 实现图片加载逻辑
        // 支持：String (URL)、File、Uri、Int (Resource ID)、"asset://xxx.jpg"
    }
    
    override fun cancel(request: PlaceholderImageLoadRequest) {
        // 取消加载请求
    }
}
```

2. 初始化时设置占位图加载器:
```kotlin
AniFlux.init(this) {
    setPlaceholderImageLoader(GlidePlaceholderImageLoader())
}
```

3. 使用占位图替换:
```kotlin
AniFlux.with(context)
    .asSVGA()
    .load("https://example.com/animation.svga")
    .placeholderReplacements {
        add("user_1", "https://example.com/user1.jpg")  // 远程图片
        add("user_2", File("/sdcard/user2.jpg"))         // 本地文件
        add("logo", R.drawable.logo)                     // Resource ID
        add("avatar", "asset://avatar.jpg")              // Asset 资源
    }
    .into(svgaImageView)
```

**支持的格式**:
- ✅ **SVGA**: 使用 `SVGADynamicEntity` 设置动态图片
- ✅ **PAG**: 使用 `PAGFile.replaceImage()` 替换图片图层
- ✅ **Lottie**: 使用 `ImageAssetDelegate` 动态提供图片

**特性**:
- 异步加载（非阻塞）
- 生命周期感知（自动清理）
- 支持请求取消
- 批量更新以提升性能
- 安全的错误处理（优雅降级）

### 5. 统一的播放监听器

AniFlux 提供统一的 `AnimationPlayListener` 接口，兼容所有动画格式:

```kotlin
AniFlux.with(context)
    .asGif()
    .load(url)
    .playListener(object : AnimationPlayListener {
        override fun onAnimationStart() {
            // 动画开始播放
        }
        
        override fun onAnimationEnd() {
            // 动画播放结束
        }
        
        override fun onAnimationRepeat() {
            // 动画循环重复
        }
        
        override fun onAnimationCancel() {
            // 动画被取消
        }
        
        override fun onAnimationUpdate(currentFrame: Int, totalFrames: Int) {
            // 动画帧更新（每帧回调）
        }
        
        override fun onAnimationFailed(error: Throwable?) {
            // 动画加载/播放失败
        }
    })
    .into(imageView)
```

### 6. 自动生命周期管理

AniFlux 自动处理 Activity/Fragment 的生命周期:

- **onStart()**: 自动恢复动画请求和播放
- **onStop()**: 自动暂停动画请求和播放
- **onDestroy()**: 自动清理所有资源，避免内存泄漏

```kotlin
// 在 Fragment 中使用
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // AniFlux 会自动绑定 Fragment 的生命周期
        AniFlux.with(this)
            .asGif()
            .load(url)
            .into(gifImageView)
        // 无需手动管理生命周期！
    }
}
```

### 7. 自动暂停/恢复机制

AniFlux 实现了智能的自动暂停/恢复机制:

- **页面不可见时**: 自动暂停动画，停止渲染和回调
- **页面可见时**: 自动恢复动画，从暂停位置继续播放
- **Fragment 可见性支持**: 在 ViewPager2 + Fragment 场景下也能正确工作

```kotlin
// 对于特殊场景，可以手动控制 Fragment 可见性
svgaImageView.setFragmentVisible(false)  // 暂停
svgaImageView.setFragmentVisible(true)    // 恢复
```

### 8. 保留最后一帧配置

AniFlux 支持控制动画结束后是否保留最后一帧:

```kotlin
AniFlux.with(context)
    .asGif()
    .load(url)
    .retainLastFrame(true)   // 保留动画停止时的帧（默认：true）
    .into(gifImageView)

// 或设置为 false 清空帧
.retainLastFrame(false)  // 动画结束后清空帧
```

**支持的格式**:
- ✅ **GIF**: 保留当前停止位置的帧
- ✅ **Lottie**: 保留当前停止位置的帧
- ✅ **SVGA**: 通过 `fillMode` 控制（Forward = 保留，Clear = 清空）
- ✅ **PAG**: 保留当前停止位置的帧
- ✅ **VAP**: 通过 `retainLastFrame` 属性控制

> **注意**: `retainLastFrame(true)` 保留的是**当前停止位置的帧**，不一定是动画的最后一帧。如果动画在中间暂停或停止，将保留该帧。

### 9. 统一的循环次数语义

AniFlux 统一了所有动画格式的循环次数语义:

| 用户设置 | 语义 | GIF | Lottie | SVGA | PAG | VAP |
|---------|------|-----|--------|------|-----|-----|
| `repeatCount(-1)` | 无限循环 | ✅ | ✅ | ✅ | ✅ | ✅ |
| `repeatCount(0)` | 播放1次 | ✅ | ❌ | ✅ | ✅ | ✅ |
| `repeatCount(1)` | 播放1次 | ✅ | ✅ | ✅ | ✅ | ✅ |
| `repeatCount(3)` | 播放3次 | ✅ | ✅ | ✅ | ✅ | ✅ |

> **注意**: 不同动画库的底层实现不同，AniFlux 会自动处理转换，确保行为一致。

### 10. 类型推断支持

AniFlux 支持加载动画时的类型推断:

```kotlin
// 根据 View 类型自动推断
AniFlux.with(context)
    .load("https://example.com/animation.svga")
    .into(svgaImageView)  // 自动推断为 SVGA

AniFlux.with(context)
    .load("https://example.com/animation.pag")
    .into(pagImageView)  // 自动推断为 PAG
```

## 🎨 支持的动画格式

所有动画格式都已包含在 `aniflux` 库中，无需额外依赖。

### GIF
- **基于**: android-gif-drawable
- **格式**: `.gif`
- **特点**: 兼容性好，文件体积较大

### Lottie
- **基于**: lottie-android
- **格式**: `.json`
- **特点**: 矢量动画，文件小，质量高
- **占位图支持**: ✅ 是

### SVGA
- **基于**: SVGAPlayer-Android（已增强自动暂停功能）
- **格式**: `.svga`
- **特点**: 高性能，支持音频，文件小
- **占位图支持**: ✅ 是

### PAG
- **基于**: libpag
- **格式**: `.pag`
- **特点**: Adobe After Effects 导出，高性能，功能强大
- **占位图支持**: ✅ 是

### VAP
- **基于**: vap
- **格式**: `.mp4`（特殊格式）
- **特点**: 视频格式，支持透明度，文件小
- **占位图支持**: ❌ 否

## 🔧 高级用法

### 自定义配置

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

### 手动管理请求

```kotlin
val requestManager = AniFlux.with(context)

// 暂停所有请求
requestManager.pauseAllRequests()

// 恢复所有请求
requestManager.resumeRequests()

// 清除所有请求
requestManager.clearRequests()
```

### 自定义占位图加载器

```kotlin
// 实现 PlaceholderImageLoader 接口
class MyPlaceholderImageLoader : PlaceholderImageLoader {
    override fun load(
        context: Context,
        source: Any,
        width: Int,
        height: Int,
        callback: PlaceholderImageLoadCallback
    ): PlaceholderImageLoadRequest {
        // 您的图片加载逻辑（例如：使用 Glide、Coil 等）
        // 支持：String (URL)、File、Uri、Int (Resource ID)、"asset://xxx.jpg"
    }
    
    override fun cancel(request: PlaceholderImageLoadRequest) {
        // 取消加载请求
    }
}

// 使用自定义加载器初始化
AniFlux.init(this) {
    setPlaceholderImageLoader(MyPlaceholderImageLoader())
}
```

## 💡 最佳实践

### 1. 在 Fragment 中使用

```kotlin
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ✅ 在 Fragment 中使用，自动绑定生命周期
        AniFlux.with(this)
            .asGif()
            .load(url)
            .into(gifImageView)
    }
}
```

### 2. 处理可见性变化

```kotlin
// ✅ 在 ViewPager2 + Fragment 场景下，手动控制 Fragment 可见性
override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    svgaImageView.setFragmentVisible(!hidden)
}
```

### 3. 内存优化

```kotlin
// ✅ 低内存设备使用合适的缓存策略
.cacheStrategy(AnimationCacheStrategy.DISK_ONLY)
```

### 4. 性能优化

- 使用占位图替换实现动态内容
- 为频繁访问的动画启用缓存
- 根据使用模式选择合适的缓存策略

## 📝 注意事项

### 1. 循环次数语义

不同动画库的底层实现不同，AniFlux 已统一处理:

- **GIF/Lottie/SVGA/PAG/VAP**: `repeatCount(N)` 都表示总播放 N 次
- **回调次数**: 对于 `repeatCount(3)`，`onAnimationRepeat()` 会回调 2 次

### 2. 内存管理

- AniFlux 自动管理内存缓存，默认使用 1/8 的可用内存
- 可通过 `AnimationOptions` 调整缓存策略
- 在 `onTrimMemory()` 时会自动清理缓存

### 3. 生命周期

- AniFlux 自动处理 Activity/Fragment 生命周期
- 在 `onDestroy()` 时会自动清理所有资源
- 无需手动调用 `clear()` 或 `pause()`

### 4. 占位图替换

- 仅支持 SVGA、PAG、Lottie 格式
- 需要实现 `PlaceholderImageLoader` 接口
- 异步加载，自动生命周期管理
- 安全的错误处理（优雅降级）

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 Apache 2.0 许可证。

---

**AniFlux** - 让动画加载变得简单统一 🎉

