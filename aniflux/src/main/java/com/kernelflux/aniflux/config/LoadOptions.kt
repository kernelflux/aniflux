package com.kernelflux.aniflux.config

import com.kernelflux.aniflux.listener.AnimationPlaybackListener
import com.kernelflux.aniflux.listener.MediaRequestListener

/**
 * @author: kernelflux
 * @date: 2025/9/21
 * 媒体资源加载配置选项数据类
 */
data class LoadOptions(
    var loadResource: LoadResource? = null,
    var placeholderResId: Int? = null,
    var errorResId: Int? = null,
    val crossFade: Int = 0,
    val overrideWidth: Int? = null,
    val overrideHeight: Int? = null,
    val cacheStrategy: CacheStrategy = CacheStrategy.ALL,
    val priority: Priority = Priority.NORMAL,
    val listener: MediaRequestListener? = null,
    val animationPlaybackListener: AnimationPlaybackListener? = null,
    // 动画播放控制选项
    val autoPlay: Boolean = true,           // 是否自动播放
    val repeatCount: Int = -1,              // 重复次数，-1表示无限循环，0表示不重复，>0表示重复指定次数
    val startFrame: Int = 0,                // 起始帧（可选）
    val endFrame: Int = -1,                  // 结束帧，-1表示到最后一帧
    var skipMemory: Boolean = false,
    val loadFirstFrameMode: Boolean = false  // 是否为首帧加载模式
)
