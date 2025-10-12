package com.kernelflux.aniflux.request.target

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.annotation.IdRes
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Synthetic
import com.kernelflux.aniflux.R
import com.kernelflux.aniflux.request.AnimationRequest
import java.lang.ref.WeakReference
import kotlin.math.max

/**
 * @author: kerneflux
 * @date: 2025/10/12
 *
 */
abstract class CustomViewAnimationTarget<T : View, Z>(protected val view: T) : AnimationTarget<Z> {
    private val sizeDeterminer: SizeDeterminer = SizeDeterminer(view)
    private var attachStateListener: OnAttachStateChangeListener? = null
    private var isClearedByUs = false
    private var isAttachStateListenerAdded = false


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
        //
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

    override fun onLoadCleared(placeholder: Drawable?) {
        sizeDeterminer.clearCallbacksAndListener()
        onResourceCleared(placeholder)
        if (!isClearedByUs) {
            maybeRemoveAttachStateListener()
        }
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

        @Synthetic
        fun checkCurrentDimens() {
            if (cbs.isEmpty()) {
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
                val observer = view.getViewTreeObserver()
                layoutListener = SizeDeterminerLayoutListener(this)
                observer.addOnPreDrawListener(layoutListener)
            }
        }

        fun removeCallback(cb: AnimationSizeReadyCallback) {
            cbs.remove(cb)
        }

        fun clearCallbacksAndListener() {
            val observer = view.getViewTreeObserver()
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
                val horizontalPadding = view.getPaddingLeft() + view.getPaddingRight()
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
            return size > 0 || size == Target.SIZE_ORIGINAL
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
                        val display = Preconditions.checkNotNull<WindowManager>(it).defaultDisplay
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
