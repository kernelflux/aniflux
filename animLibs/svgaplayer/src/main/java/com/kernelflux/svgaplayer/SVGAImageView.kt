package com.kernelflux.svgaplayer


import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.kernelflux.svgaplayer.utils.SVGARange
import com.kernelflux.svgaplayer.utils.log.LogUtils
import java.lang.ref.WeakReference
import java.net.URL

/**
 * Created by PonyCui on 2017/3/29.
 */
@SuppressLint("ObsoleteSdkInt")
open class SVGAImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private val TAG = "SVGAImageView"

    enum class FillMode {
        Backward,
        Forward,
        Clear,
    }

    var isAnimating = false
        private set
    private var mOriginalRepeatCount = 0  // 初始设置的 repeatCount
    private var mCompletedRepeatCount = 0  // 已完成的重复次数

    @Deprecated(
        "It is recommended to use clearAfterDetached, or manually call to SVGAVideoEntity#clear." +
                "If you just consider cleaning up the canvas after playing, you can use FillMode#Clear.",
        level = DeprecationLevel.WARNING
    )
    var clearsAfterStop = false
    var clearsAfterDetached = false
    var fillMode: FillMode = FillMode.Forward
    var callback: SVGACallback? = null

    private var mAnimator: ValueAnimator? = null
    private var mItemClickAreaListener: SVGAClickAreaListener? = null
    private var mAntiAlias = true
    private var mAutoPlay = true
    private val mAnimatorListener = AnimatorListener(this)
    private val mAnimatorUpdateListener = AnimatorUpdateListener(this)
    private var mStartFrame = 0
    private var mEndFrame = 0

    // 当前动画的播放方向（正向或反向）
    private var mCurrentReverse = false

    // Auto-pause feature (referenced from LibPAG implementation)
    private var isAttachedToWindow = false
    private var isVisible = false

    // 窗口可见性：用于检测 Activity 进入后台的情况
    private var windowVisibility = View.VISIBLE

    // Saved animation state when paused due to visibility
    private var mPausedAnimationState: PausedAnimationState? = null

    /**
     * Saved animation state for resuming after visibility change
     */
    private data class PausedAnimationState(
        val range: SVGARange?,
        val reverse: Boolean,
        val currentFrame: Int,
        val repeatCount: Int,  // ✅ 剩余的 repeatCount（恢复时使用）
        val completedRepeatCount: Int,  // ✅ 已完成的重复次数
        val originalRepeatCount: Int  // ✅ 原始的 repeatCount（用于校验）
    )

    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        attrs?.let { loadAttrs(it) }
    }

    private fun loadAttrs(attrs: AttributeSet) {
        val typedArray =
            context.theme.obtainStyledAttributes(attrs, R.styleable.SVGAImageView, 0, 0)
        mOriginalRepeatCount = typedArray.getInt(R.styleable.SVGAImageView_repeatCount, 0)
        clearsAfterStop = typedArray.getBoolean(R.styleable.SVGAImageView_clearsAfterStop, false)
        clearsAfterDetached =
            typedArray.getBoolean(R.styleable.SVGAImageView_clearsAfterDetached, false)
        mAntiAlias = typedArray.getBoolean(R.styleable.SVGAImageView_antiAlias, true)
        mAutoPlay = typedArray.getBoolean(R.styleable.SVGAImageView_autoPlay, true)
        typedArray.getString(R.styleable.SVGAImageView_fillMode)?.let {
            when (it) {
                "0" -> {
                    fillMode = FillMode.Backward
                }

                "1" -> {
                    fillMode = FillMode.Forward
                }

                "2" -> {
                    fillMode = FillMode.Clear
                }
            }
        }
        typedArray.getString(R.styleable.SVGAImageView_source)?.let {
            parserSource(it)
        }
        typedArray.recycle()
    }

    private fun parserSource(source: String) {
        val refImgView = WeakReference<SVGAImageView>(this)
        val parser = SVGAParser(context)
        if (source.startsWith("http://") || source.startsWith("https://")) {
            parser.decodeFromURL(URL(source), createParseCompletion(refImgView))
        } else {
            parser.decodeFromAssets(source, createParseCompletion(refImgView))
        }
    }

    private fun createParseCompletion(ref: WeakReference<SVGAImageView>): SVGAParser.ParseCompletion {
        return object : SVGAParser.ParseCompletion {
            override fun onComplete(videoItem: SVGAVideoEntity) {
                ref.get()?.startAnimation(videoItem)
            }

            override fun onError() {}
        }
    }

    private fun startAnimation(videoItem: SVGAVideoEntity) {
        this@SVGAImageView.post {
            videoItem.antiAlias = mAntiAlias
            setVideoItem(videoItem)
            getSVGADrawable()?.scaleType = scaleType
            if (mAutoPlay) {
                startAnimation()
            }
        }
    }

    fun startAnimation() {
        startAnimation(null, false)
    }

    fun startAnimation(range: SVGARange?, reverse: Boolean = false) {
        stopAnimation(false)
        // Clear paused state when manually starting animation
        mPausedAnimationState = null
        play(range, reverse)
    }

    private fun play(range: SVGARange?, reverse: Boolean, repeatCount: Int? = null) {
        LogUtils.info(TAG, "================ start animation ================")
        val drawable = getSVGADrawable() ?: return
        setupDrawable()

        // ✅ 保存当前的播放方向
        mCurrentReverse = reverse

        // ✅ 设置播放状态（在创建 animator 前设置，确保 callback 检查时能正确判断）
        isAnimating = true
        drawable.videoItem.isPlaying = true

        mStartFrame = 0.coerceAtLeast(range?.location ?: 0)
        val videoItem = drawable.videoItem
        mEndFrame = (videoItem.frames - 1).coerceAtMost(
            ((range?.location ?: 0) + (range?.length ?: Int.MAX_VALUE) - 1)
        )
        val animator = ValueAnimator.ofInt(mStartFrame, mEndFrame)
        animator.interpolator = LinearInterpolator()
        animator.duration =
            ((mEndFrame - mStartFrame + 1) * (1000 / videoItem.FPS) / generateScale()).toLong()


        val calculatedRepeatCount = repeatCount ?: if (mOriginalRepeatCount <= 0) {
            if (mOriginalRepeatCount == ValueAnimator.INFINITE) ValueAnimator.INFINITE else 99999
        } else {
            mOriginalRepeatCount - 1
        }

        if (repeatCount == null) {
            mOriginalRepeatCount = calculatedRepeatCount
            mCompletedRepeatCount = 0
        }
        animator.repeatCount = calculatedRepeatCount
        animator.addUpdateListener(mAnimatorUpdateListener)
        animator.addListener(mAnimatorListener)
        if (reverse) {
            animator.reverse()
        } else {
            animator.start()
        }
        mAnimator = animator
    }

    private fun setupDrawable() {
        val drawable = getSVGADrawable() ?: return
        drawable.cleared = false
        drawable.scaleType = scaleType
    }

    private fun getSVGADrawable(): SVGADrawable? {
        return drawable as? SVGADrawable
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    private fun generateScale(): Double {
        var scale = 1.0
        try {
            val animatorClass = Class.forName("android.animation.ValueAnimator") ?: return scale
            val getMethod = animatorClass.getDeclaredMethod("getDurationScale") ?: return scale
            scale = (getMethod.invoke(animatorClass) as Float).toDouble()
            if (scale == 0.0) {
                val setMethod =
                    animatorClass.getDeclaredMethod("setDurationScale", Float::class.java)
                        ?: return scale
                setMethod.isAccessible = true
                setMethod.invoke(animatorClass, 1.0f)
                scale = 1.0
                LogUtils.info(
                    TAG,
                    "The animation duration scale has been reset to" +
                            " 1.0x, because you closed it on developer options."
                )
            }
        } catch (ignore: Exception) {
            ignore.printStackTrace()
        }
        return scale
    }

    private fun onAnimatorUpdate(animator: ValueAnimator?) {
        // 1. 先检查 isAttachedToWindow（同步检查）
        synchronized(this) {
            if (!isAttachedToWindow) {
                return
            }
        }

        // 2. 检查 isVisible，不可见时直接返回（不更新、不触发 callback）
        // PAG 的做法：不可见时也会执行一些逻辑（如 flush），但我们为了彻底阻止 callback，直接返回
        if (!isVisible) {
            return
        }

        val drawable = getSVGADrawable() ?: return

        // 3. ✅ 额外的安全检查：确保动画还在播放且处于播放状态
        // 这可以防止在暂停过程中（isVisible 可能还未及时更新）的 callback
        // 注意：这个检查比 PAG 更严格，因为 ValueAnimator.cancel() 后可能还有排队的回调
        if (!isAnimating || !drawable.videoItem.isPlaying) {
            return
        }

        // 4. ✅ 只有在所有条件都满足时才更新帧和触发 callback
        drawable.currentFrame = animator?.animatedValue as Int
        val percentage =
            (drawable.currentFrame + 1).toDouble() / drawable.videoItem.frames.toDouble()
        callback?.onStep(drawable.currentFrame, percentage)
    }

    private fun onAnimationEnd(animation: Animator?) {
        isAnimating = false
        getSVGADrawable()?.videoItem?.isPlaying = false

        val drawable = getSVGADrawable()


        // ✅ 先根据 fillMode 设置最后一帧，确保保留最后一帧
        // 注意：必须在 stopAnimation() 之前设置，因为 stopAnimation() 可能会影响显示
        if (drawable != null) {
            when (fillMode) {
                FillMode.Backward -> {
                    // ✅ 先确保 cleared = false，否则 draw() 不会绘制
                    drawable.cleared = false
                    drawable.currentFrame = mStartFrame
                }

                FillMode.Forward -> {
                    // ✅ 先确保 cleared = false，否则 draw() 不会绘制
                    // ✅ 保留最后一帧（FillMode.Forward 的默认行为）
                    drawable.cleared = false
                    drawable.currentFrame = mEndFrame
                }

                FillMode.Clear -> {
                    drawable.cleared = true
                }
            }
        }

        // ✅ 然后停止动画（停止音频等，但不清除 drawable）
        // 注意：clear 参数为 false，确保 drawable 不会被清除，最后一帧可以显示
        stopAnimation(clear = false)
        callback?.onFinished()
    }

    fun clear() {
        getSVGADrawable()?.cleared = true
        getSVGADrawable()?.clear()
        // 清除对 drawable 的引用
        setImageDrawable(null)
    }


    fun setPlayRepeatCount(repeatCount: Int) {
        mOriginalRepeatCount = repeatCount
    }

    fun pauseAnimation() {
        // 停止动画器（但不清除状态）
        mAnimator?.cancel()
        mAnimator?.removeAllListeners()
        mAnimator?.removeAllUpdateListeners()

        // ✅ 使用 pause() 而不是 stop()，这样可以恢复音频
        getSVGADrawable()?.pause()

        callback?.onPause()
    }

    fun stopAnimation() {
        stopAnimation(clear = clearsAfterStop)
    }

    fun stopAnimation(clear: Boolean) {
        mAnimator?.cancel()
        mAnimator?.removeAllListeners()
        mAnimator?.removeAllUpdateListeners()
        getSVGADrawable()?.stop()
        getSVGADrawable()?.cleared = clear
    }

    fun setVideoItem(videoItem: SVGAVideoEntity?) {
        setVideoItem(videoItem, SVGADynamicEntity())
    }

    fun setVideoItem(videoItem: SVGAVideoEntity?, dynamicItem: SVGADynamicEntity?) {
        if (videoItem == null) {
            setImageDrawable(null)
        } else {
            val drawable = SVGADrawable(videoItem, dynamicItem ?: SVGADynamicEntity())
            drawable.cleared = true
            setImageDrawable(drawable)
        }
    }

    fun stepToFrame(frame: Int, andPlay: Boolean) {
        pauseAnimation()
        val drawable = getSVGADrawable() ?: return
        drawable.currentFrame = frame
        if (andPlay) {
            startAnimation()
            mAnimator?.let {
                it.currentPlayTime = (0.0f.coerceAtLeast(
                    1.0f.coerceAtMost((frame.toFloat() / drawable.videoItem.frames.toFloat()))
                ) * it.duration).toLong()
            }
        }
    }

    fun stepToPercentage(percentage: Double, andPlay: Boolean) {
        val drawable = drawable as? SVGADrawable ?: return
        var frame = (drawable.videoItem.frames * percentage).toInt()
        if (frame >= drawable.videoItem.frames && frame > 0) {
            frame = drawable.videoItem.frames - 1
        }
        stepToFrame(frame, andPlay)
    }

    fun setOnAnimKeyClickListener(clickListener: SVGAClickAreaListener) {
        mItemClickAreaListener = clickListener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event)
        }
        val drawable = getSVGADrawable() ?: return super.onTouchEvent(event)
        for ((key, value) in drawable.dynamicItem.mClickMap) {
            if (event.x >= value[0] && event.x <= value[2] && event.y >= value[1] && event.y <= value[3]) {
                mItemClickAreaListener?.let {
                    it.onClick(key)
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttachedToWindow = true
        checkVisible()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAttachedToWindow = false
        checkVisible()
        stopAnimation(clearsAfterDetached)
        if (clearsAfterDetached) {
            clear()
        }
    }

    override fun onVisibilityAggregated(visibility: Boolean) {
        super.onVisibilityAggregated(visibility)
        checkVisible()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        checkVisible()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        windowVisibility = visibility
        checkVisible()
    }


    private fun checkVisible() {
        val visible = isAttachedToWindow && isShown() && windowVisibility == View.VISIBLE
        if (isVisible == visible) {
            return
        }
        isVisible = visible
        if (isVisible) {
            // Become visible: resume animation if it was paused due to visibility
            if (mPausedAnimationState != null) {
                val state = mPausedAnimationState
                mPausedAnimationState = null
                val drawable = getSVGADrawable()
                if (drawable != null) {
                    // Restore to the frame where it was paused
                    drawable.currentFrame = state?.currentFrame ?: 0
                    // ✅ 恢复音频（如果是暂停状态）
                    drawable.resume()
                    state?.apply {
                        mCompletedRepeatCount = completedRepeatCount
                        mOriginalRepeatCount = originalRepeatCount
                    }
                    play(state?.range, state?.reverse ?: false, state?.repeatCount)
                    LogUtils.info(TAG, "Auto-resume animation at frame ${state?.currentFrame}")
                }
            }
        } else {
            // Become invisible: pause animation and save state
            if (isAnimating && mAnimator != null) {
                val drawable = getSVGADrawable()
                if (drawable != null) {
                    val currentFrame = drawable.currentFrame
                    val currentRepeatCount = mOriginalRepeatCount - mCompletedRepeatCount
                    mPausedAnimationState = PausedAnimationState(
                        range = if (mStartFrame >= 0 && mEndFrame > mStartFrame) {
                            SVGARange(mStartFrame, mEndFrame - mStartFrame + 1)
                        } else {
                            null
                        },
                        reverse = mCurrentReverse,
                        currentFrame = currentFrame,
                        repeatCount = currentRepeatCount,
                        completedRepeatCount = mCompletedRepeatCount,
                        originalRepeatCount = mOriginalRepeatCount
                    )
                    drawable.videoItem.isPlaying = false
                    isAnimating = false
                    mAnimator?.cancel()
                    drawable.pause()
                    LogUtils.info(
                        TAG,
                        "Auto-pause animation at frame $currentFrame, reverse=$mCurrentReverse"
                    )
                }
            }
        }
    }

    private class AnimatorListener(view: SVGAImageView) : Animator.AnimatorListener {
        private val weakReference = WeakReference<SVGAImageView>(view)

        override fun onAnimationRepeat(animation: Animator) {
            weakReference.get()?.apply {
                mCompletedRepeatCount++
                callback?.onRepeat()
            }
        }

        override fun onAnimationEnd(animation: Animator) {
            weakReference.get()?.onAnimationEnd(animation)
        }

        override fun onAnimationCancel(animation: Animator) {
            weakReference.get()?.apply {
                isAnimating = false
                getSVGADrawable()?.videoItem?.isPlaying = false
            }
        }

        override fun onAnimationStart(animation: Animator) {
            weakReference.get()?.apply {
                if (mCompletedRepeatCount <= 0) {
                    callback?.onStart()
                }
                isAnimating = true
            }
        }
    }


    private class AnimatorUpdateListener(view: SVGAImageView) :
        ValueAnimator.AnimatorUpdateListener {
        private val weakReference = WeakReference<SVGAImageView>(view)

        override fun onAnimationUpdate(animation: ValueAnimator) {
            weakReference.get()?.onAnimatorUpdate(animation)
        }
    }
}
