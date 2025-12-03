package com.kernelflux.aniflux.annotation

/**
 * 自动注册Loader注解
 *
 * 使用此注解标记Loader类，编译时会自动生成注册代码
 *
 * 示例：
 * ```kotlin
 * @AutoRegisterLoader(animationType = "GIF")
 * class GifAnimationLoader : AnimationLoader<GifDrawable> {
 *     // ...
 * }
 * ```
 *
 * @param animationType 动画类型名称（AnimationTypeDetector.AnimationType的枚举值）
 *
 * @author: kernelflux
 * @date:  2025/11/30
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class AutoRegisterLoader(
    /**
     * 动画类型名称
     * 必须是 AnimationTypeDetector.AnimationType 的枚举值之一：
     * - "GIF"
     * - "LOTTIE"
     * - "SVGA"
     * - "PAG"
     * - "VAP"
     */
    val animationType: String
)


