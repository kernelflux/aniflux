package com.kernelflux.aniflux.request.target

import android.graphics.drawable.Drawable
import com.kernelflux.vapplayer.AnimView
import java.io.File

/**
 * Vap动画的专用ViewTarget
 * 自动处理GifDrawable资源到GifImageView的设置
 *
 * @author: kerneflux
 * @date: 2025/01/XX
 */
class VAPViewTarget(view: AnimView) : CustomViewAnimationTarget<AnimView, File>(view) {

    override fun onResourceReady(resource: File) {
        val repeatCount = animationOptions?.repeatCount ?: -1
        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)
        view.apply {
            // ✅ VAP 的 setLoop 语义分析（根据 HardDecoder.kt:253-277）：
            // playLoop = N，每次 EOS 时：loop = --playLoop，如果 loop > 0 则循环
            // playLoop = 2: 第1次结束 loop=1>0 循环，第2次结束 loop=0 结束 → 总播放 2 次
            // playLoop = 3: 第1次结束 loop=2>0 循环，第2次结束 loop=1>0 循环，第3次结束 loop=0 结束 → 总播放 3 次
            // 所以 setLoop(N) 表示总播放 N 次，而不是循环 N 次！
            // 统一 API 语义：repeatCount <= 0 = 无限循环，N = 总播放N次
            // 转换：repeatCount(3) → setLoop(3) → 总播放 3 次
            setLoop(
                when {
                    repeatCount <= 0 -> Int.MAX_VALUE  // 无限循环
                    else -> repeatCount  // 总播放N次 → setLoop(N)
                }
            )
            startPlay(resource)
        }
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {

    }

    override fun onResourceCleared(placeholder: Drawable?) {

    }
}

