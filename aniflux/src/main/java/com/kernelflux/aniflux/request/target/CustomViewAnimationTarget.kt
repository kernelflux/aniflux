package com.kernelflux.aniflux.request.target

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.annotation.IdRes
import androidx.core.util.Preconditions
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.kernelflux.aniflux.R
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.listener.AnimationPlayListenerSetupHelper
import com.kernelflux.aniflux.util.AnimationOptions
import java.lang.ref.WeakReference
import kotlin.math.max
import androidx.core.view.isGone
import androidx.core.view.isInvisible

/**
 * @author: kerneflux
 * @date: 2025/10/12
 *
 */
abstract class CustomViewAnimationTarget<T : View, Z>(protected val view: T) : AnimationTarget<Z> {
    /**
     * 获取关联的View（返回View类型，用于可见性检查）
     */
    fun getViewForVisibilityCheck(): View = view
    private val sizeDeterminer: SizeDeterminer = SizeDeterminer(view)
    private var attachStateListener: OnAttachStateChangeListener? = null
    private var isClearedByUs = false
    private var isAttachStateListenerAdded = false
    
    // 动画播放监听器（直接持有，无需Manager包装）
    @Volatile
    var playListener: AnimationPlayListener? = null
        private set
    
    // 动画配置选项（用于播放设置）
    @Volatile
    var animationOptions: AnimationOptions? = null
        internal set


    companion object {
        const val TAG: String = "CustomViewAnimationTarget"

        @IdRes
        private val VIEW_TAG_ID: Int = R.id.aniflux_custom_view_target_tag
    }

    protected abstract fun onResourceCleared(placeholder: Drawable?)

    protected fun onResourceLoading(placeholder: Drawable?) {
        // Default empty.
    }

    override fun onStart() {
        //
    }

    override fun onStop() {
        //
    }

    override fun onDestroy() {
        // 清理监听器
        cleanupPlayListeners()
    }
    
    override fun onLoadCleared(placeholder: Drawable?) {
        sizeDeterminer.clearCallbacksAndListener()
        onResourceCleared(placeholder)
        // 清理监听器设置
        cleanupPlayListeners()
        if (!isClearedByUs) {
            maybeRemoveAttachStateListener()
        }
    }
    
    /**
     * 添加动画播放监听器（替换旧的）
     * 
     * @param listener 监听器实例
     * @return 是否添加成功
     */
    fun addPlayListener(listener: AnimationPlayListener?): Boolean {
        if (listener == null) return false
        playListener = listener
        return true
    }
    
    /**
     * 移除动画播放监听器
     * 
     * @param listener 监听器实例（用于验证是否为当前监听器）
     * @return 是否移除成功
     */
    fun removePlayListener(listener: AnimationPlayListener?): Boolean {
        if (listener == null) return false
        if (playListener === listener) {
            playListener = null
            return true
        }
        return false
    }
    
    /**
     * 清除监听器
     */
    fun clearPlayListener() {
        playListener = null
    }
    
    /**
     * 设置动画播放监听器到资源
     * 在onResourceReady中设置资源后调用此方法，会自动将监听器设置到对应的动画对象
     * 
     * @param resource 动画资源（PAGFile, LottieDrawable, SVGADrawable, GifDrawable等）
     * @param view 显示动画的View（可选，用于PAG/Lottie等需要View的动画类型）
     */
    fun setupPlayListeners(resource: Any, view: View? = null) {
        AnimationPlayListenerSetupHelper.setupListeners(this, resource, view)
    }
    
    /**
     * 清理监听器设置
     * 在onLoadCleared时自动调用，也会在onDestroy时调用
     */
    internal fun cleanupPlayListeners() {
        AnimationPlayListenerSetupHelper.cleanup(this)
        playListener = null
    }

    fun waitForLayout(): CustomViewAnimationTarget<T, Z> {
        sizeDeterminer.waitForLayout = true
        return this
    }


    fun clearOnDetach(): CustomViewAnimationTarget<T, Z> {
        if (attachStateListener != null) {
            return this
        }
        attachStateListener =
            object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    resumeMyRequest()
                }

                override fun onViewDetachedFromWindow(v: View) {
                    pauseMyRequest()
                }
            }
        maybeAddAttachStateListener()
        return this
    }


    fun resumeMyRequest() {
        val request = getRequest()
        if (request != null && request.isCleared()) {
            request.begin()
        }
    }

    fun pauseMyRequest() {
        val request = getRequest()
        if (request != null) {
            isClearedByUs = true
            request.clear()
            isClearedByUs = false
        }
    }


    private fun setTag(tag: Any?) {
        view.setTag(VIEW_TAG_ID, tag)
    }

    private fun getTag(): Any? {
        return view.getTag(VIEW_TAG_ID)
    }

    private fun maybeAddAttachStateListener() {
        if (attachStateListener == null || isAttachStateListenerAdded) {
            return
        }

        view.addOnAttachStateChangeListener(attachStateListener)
        isAttachStateListenerAdded = true
    }

    private fun maybeRemoveAttachStateListener() {
        if (attachStateListener == null || !isAttachStateListenerAdded) {
            return
        }

        view.removeOnAttachStateChangeListener(attachStateListener)
        isAttachStateListenerAdded = false
    }


    override fun onLoadStarted(placeholder: Drawable?) {
        maybeAddAttachStateListener()
        onResourceLoading(placeholder)
    }


    override fun setRequest(request: AnimationRequest?) {
        setTag(request)
    }


    override fun getRequest(): AnimationRequest? {
        val tag = getTag() ?: return null
        if (tag !is AnimationRequest) {
            throw IllegalArgumentException("You must not pass non-R.id ids to setTag(id)")
        }
        return tag
    }

    override fun toString(): String {
        return "Target for: $view"
    }


    override fun getSize(cb: AnimationSizeReadyCallback) {
        sizeDeterminer.getSize(cb)
    }

    override fun removeCallback(cb: AnimationSizeReadyCallback) {
        sizeDeterminer.removeCallback(cb)
    }
    
    /**
     * 获取关联的Lifecycle（如果存在）
     * 通过View找到对应的Activity或Fragment的Lifecycle
     */
    protected fun getLifecycle(): androidx.lifecycle.Lifecycle? {
        val context = view.context ?: return null
        
        // 尝试从Context获取Activity
        val activity = findActivity(context) ?: return null
        
        // 如果是FragmentActivity，尝试找到Fragment
        if (activity is androidx.fragment.app.FragmentActivity) {
            val fragment = findSupportFragment(view, activity)
            if (fragment != null) {
                return fragment.lifecycle
            }
            return activity.lifecycle
        }
        
        // 标准Activity（需要AndroidX Activity）
        if (activity is androidx.lifecycle.LifecycleOwner) {
            return activity.lifecycle
        }
        
        return null
    }
    
    /**
     * 查找View所属的Fragment
     */
    private fun findSupportFragment(view: View, activity: androidx.fragment.app.FragmentActivity): androidx.fragment.app.Fragment? {
        var current: View? = view
        while (current != null) {
            val fragment = activity.supportFragmentManager.findFragmentByTag(current.tag?.toString())
            if (fragment != null) {
                return fragment
            }
            current = current.parent as? View
        }
        return null
    }
    
    /**
     * 从Context查找Activity
     */
    private fun findActivity(context: Context): android.app.Activity? {
        return when (context) {
            is android.app.Activity -> context
            is android.content.ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
    }


    class SizeDeterminer internal constructor(private val view: View) {
        private val cbs: MutableList<AnimationSizeReadyCallback> =
            ArrayList<AnimationSizeReadyCallback>()
        var waitForLayout: Boolean = false
        private var layoutListener: SizeDeterminerLayoutListener? = null

        private fun notifyCbs(width: Int, height: Int) {
            for (cb in ArrayList<AnimationSizeReadyCallback>(cbs)) {
                cb.onSizeReady(width, height)
            }
        }

        fun checkCurrentDimens() {
            if (cbs.isEmpty()) {
                return
            }

            if (view.isGone || view.isInvisible) {
                return
            }
            
            val currentWidth = this.targetWidth
            val currentHeight = this.targetHeight
            if (!isViewStateAndSizeValid(currentWidth, currentHeight)) {
                return
            }

            notifyCbs(currentWidth, currentHeight)
            clearCallbacksAndListener()
        }

        fun getSize(cb: AnimationSizeReadyCallback) {
            if (view.isGone || view.isInvisible) {
                // View不可见，添加到回调列表，等待View变为可见
                if (!cbs.contains(cb)) {
                    cbs.add(cb)
                }
                if (layoutListener == null) {
                    val observer = view.viewTreeObserver
                    layoutListener = SizeDeterminerLayoutListener(this)
                    observer.addOnPreDrawListener(layoutListener)
                }
                return
            }
            
            val currentWidth = this.targetWidth
            val currentHeight = this.targetHeight
            if (isViewStateAndSizeValid(currentWidth, currentHeight)) {
                cb.onSizeReady(currentWidth, currentHeight)
                return
            }
            // We want to notify callbacks in the order they were added and we only expect one or two
            // callbacks to be added a time, so a List is a reasonable choice.
            if (!cbs.contains(cb)) {
                cbs.add(cb)
            }
            if (layoutListener == null) {
                val observer = view.viewTreeObserver
                layoutListener = SizeDeterminerLayoutListener(this)
                observer.addOnPreDrawListener(layoutListener)
            }
        }

        fun removeCallback(cb: AnimationSizeReadyCallback) {
            cbs.remove(cb)
        }

        fun clearCallbacksAndListener() {
            val observer = view.viewTreeObserver
            if (observer.isAlive) {
                observer.removeOnPreDrawListener(layoutListener)
            }
            layoutListener = null
            cbs.clear()
        }

        private fun isViewStateAndSizeValid(width: Int, height: Int): Boolean {
            return isDimensionValid(width) && isDimensionValid(height)
        }

        private val targetHeight: Int
            get() {
                val verticalPadding = view.paddingTop + view.paddingBottom
                val layoutParams = view.layoutParams
                val layoutParamSize = layoutParams?.height ?: PENDING_SIZE
                return getTargetDimen(view.height, layoutParamSize, verticalPadding)
            }

        private val targetWidth: Int
            get() {
                val horizontalPadding = view.paddingLeft + view.paddingRight
                val layoutParams = view.layoutParams
                val layoutParamSize = layoutParams?.width ?: PENDING_SIZE
                return getTargetDimen(view.width, layoutParamSize, horizontalPadding)
            }

        @SuppressLint("LongLogTag", "Range")
        private fun getTargetDimen(viewSize: Int, paramSize: Int, paddingSize: Int): Int {
            val adjustedParamSize = paramSize - paddingSize
            if (adjustedParamSize > 0) {
                return adjustedParamSize
            }
            if (waitForLayout && view.isLayoutRequested) {
                return PENDING_SIZE
            }
            val adjustedViewSize = viewSize - paddingSize
            if (adjustedViewSize > 0) {
                return adjustedViewSize
            }
            if (!view.isLayoutRequested && paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
                if (Log.isLoggable(TAG, Log.INFO)) {
                    Log.i(
                        TAG,
                        ("AniFlux treats LayoutParams.WRAP_CONTENT as a request for an image the size of"
                                + " this device's screen dimensions. If you want to load the original image and"
                                + " are ok with the corresponding memory cost and OOMs (depending on the input"
                                + " size), use .override(Target.SIZE_ORIGINAL). Otherwise, use"
                                + " LayoutParams.MATCH_PARENT, set layout_width and layout_height to fixed"
                                + " dimension, or use .override() with fixed dimensions.")
                    )
                }
                return getMaxDisplayLength(view.context)
            }
            return PENDING_SIZE
        }

        private fun isDimensionValid(size: Int): Boolean {
            return size > 0 || size == AnimationTarget.SIZE_ORIGINAL
        }

        private class SizeDeterminerLayoutListener(sizeDeterminer: SizeDeterminer) :
            ViewTreeObserver.OnPreDrawListener {
            private val sizeDeterminerRef: WeakReference<SizeDeterminer> =
                WeakReference<SizeDeterminer>(sizeDeterminer)

            @SuppressLint("LogTagMismatch", "LongLogTag", "Range")
            override fun onPreDraw(): Boolean {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "OnGlobalLayoutListener called attachStateListener=$this")
                }
                val sizeDeterminer = sizeDeterminerRef.get()
                sizeDeterminer?.checkCurrentDimens()
                return true
            }
        }

        companion object {
            private const val PENDING_SIZE = 0
            var maxDisplayLength: Int? = null

            @JvmStatic
            private fun getMaxDisplayLength(context: Context): Int {
                return maxDisplayLength
                    ?: (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).let {
                        val display = it.defaultDisplay
                        val displayDimensions = Point()
                        display.getSize(displayDimensions)
                        max(displayDimensions.x, displayDimensions.y)
                    }.also {
                        maxDisplayLength = it
                    }
            }
        }
    }
}
