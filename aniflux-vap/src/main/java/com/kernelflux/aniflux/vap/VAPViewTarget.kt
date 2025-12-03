package com.kernelflux.aniflux.vap

import android.graphics.drawable.Drawable
import android.view.View
import com.kernelflux.aniflux.request.target.CustomViewAnimationTarget
import com.kernelflux.vap.AnimView
import com.kernelflux.vap.inter.IAnimListener
import java.io.File

/**
 * Vap动画的专用ViewTarget
 * 自动处理File资源到AnimView的设置
 *
 * @author: kerneflux
 * @date: 2025/12/01
 */
class VAPViewTarget(view: AnimView) : CustomViewAnimationTarget<AnimView, File>(view) {

    private var currentAdapter: VapPlayListenerAdapter? = null
    private var currentListener: IAnimListener? = null

    override fun setupPlayListeners(resource: Any, view: View?) {
        val animView = view as? AnimView ?: return
        val listener = playListener ?: return

        // 移除旧的监听器
        currentListener?.let { oldListener ->
            try {
                animView.setAnimListener(null)
            } catch (e: Exception) {
                // 忽略移除时的异常
            }
        }

        // 获取 retainLastFrame 配置
        val retainLastFrame = animationOptions?.retainLastFrame ?: true

        // 创建新的适配器
        val adapter = VapPlayListenerAdapter(listener, animView, retainLastFrame)
        val animListener = adapter.createAnimatorListener()
        animView.setAnimListener(animListener)

        // 保存引用以便清理
        currentAdapter = adapter
        currentListener = animListener
    }

    override fun onResourceReady(resource: File) {
        val repeatCount = animationOptions?.repeatCount ?: -1
        val retainLastFrame = animationOptions?.retainLastFrame ?: true

        // 先设置监听器（避免错过 onAnimationStart）
        setupPlayListeners(resource, view)
        view.apply {
            // ✅ 设置 retainLastFrame 配置
            this.retainLastFrame = retainLastFrame

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
        // VAP 加载失败的处理
    }

    override fun onResourceCleared(placeholder: Drawable?) {
        // 清理监听器
        currentListener?.let { listener ->
            try {
                view.setAnimListener(null)
            } catch (e: Exception) {
                // 忽略清理时的异常
            }
        }
        currentAdapter?.onClear()
        currentAdapter = null
        currentListener = null
    }
}

