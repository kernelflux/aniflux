package com.kernelflux.aniflux.request.target

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory
import com.kernelflux.aniflux.log.AniFluxLogLevel
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.request.listener.AnimationPlayListenerSetupHelper
import com.kernelflux.aniflux.util.AnimationOptions
import com.kernelflux.aniflux.util.ViewDetachScenarioDetector
import androidx.lifecycle.LifecycleEventObserver
import java.lang.ref.WeakReference
import kotlin.math.max
import androidx.core.view.isGone
import androidx.core.view.isInvisible

/**
 * @author: kerneflux
 * @date: 2025/10/12
 *
 */
@SuppressLint("LongLogTag")
abstract class CustomViewAnimationTarget<T : View, Z>(protected val view: T) : AnimationTarget<Z> {
    /**
     * Get associated View (returns View type for visibility check)
     */
    fun getViewForVisibilityCheck(): View = view
    private val sizeDeterminer: SizeDeterminer = SizeDeterminer(view)
    private var attachStateListener: OnAttachStateChangeListener? = null
    private var isClearedByUs = false
    private var isAttachStateListenerAdded = false
    
    // Animation play listener (directly held, no Manager wrapper needed)
    @Volatile
    var playListener: AnimationPlayListener? = null
        private set
    
    // Animation configuration options (for playback settings)
    @Volatile
    var animationOptions: AnimationOptions? = null
        internal set

    // Memory leak protection: thread-safe state flags
    @Volatile
    private var isResourceReleased = false
    
    @Volatile
    private var isAnimationPaused = false
    
    // Whether auto cleanup is enabled (default: true)
    private var autoCleanupEnabled = true
    
    // Cache whether View is in reusable container (RecyclerView, ListView, ViewPager, etc.)
    // This is set when View is attached and cached to avoid detection failure after View is recycled
    // Important: After sliding many pages, View may be recycled and parent hierarchy is lost
    @Volatile
    private var isInReusableContainer: Boolean? = null
    
    // Lifecycle and view attach listeners for memory leak protection
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var memoryLeakProtectionAttachListener: OnAttachStateChangeListener? = null

    companion object {
        const val TAG: String = "CustomViewAnimationTarget"
    }
    
    init {
        // Pre-detect and cache reusable container status if View is already attached (e.g., in XML)
        updateReusableContainerCache()
        
        // Setup memory leak protection listeners
        setupLifecycleObserver()
        if (autoCleanupEnabled) {
            setupMemoryLeakProtectionAttachListener()
        }
    }

    protected abstract fun onResourceCleared(placeholder: Drawable?)

    protected fun onResourceLoading(placeholder: Drawable?) {
        // Default empty.
    }
    
    /**
     * Called when resource is ready
     * Subclasses should call this method at the beginning of their onResourceReady() implementation
     * to ensure reusable container cache is set
     */
    protected fun onResourceReadyInternal() {
        // Ensure reusable container cache is set when resource is ready
        updateReusableContainerCache()
    }
    
    /**
     * Update reusable container cache (called when View is attached)
     * This ensures we can detect RecyclerView even after View is detached
     */
    private fun updateReusableContainerCache() {
        if (view.isAttachedToWindow) {
            ViewDetachScenarioDetector.preDetectDialogAndPopup(view)
            if (isInReusableContainer == null) {
                isInReusableContainer = ViewDetachScenarioDetector.isInReusableContainer(view)
            }
        }
    }

    override fun onStart() {
        //
    }

    override fun onStop() {
        //
    }

    override fun onDestroy() {
        performFullCleanup("TargetOnDestroy")
    }
    
    override fun onLoadCleared(placeholder: Drawable?) {
        // Strategy:
        // 1. If host is destroyed, always release resources (even if in RecyclerView)
        // 2. If host is alive and View is in RecyclerView, keep resources (recycling scenario)
        // 3. If host is alive but can't detect RecyclerView, be conservative and keep resources
        val hostLifecycle = getHostLifecycle()
        val isHostDestroyed = hostLifecycle?.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED
        
        // Priority 1: If host is destroyed, always release resources
        if (isHostDestroyed) {
            AniFluxLog.i(AniFluxLogCategory.TARGET, "onLoadCleared - Host destroyed, performing full cleanup")
            performFullCleanup("RequestCleared_HostDestroyed")
            // performFullCleanup already handles all cleanup, but we need to call onResourceCleared for placeholder
            sizeDeterminer.clearCallbacksAndListener()
            onResourceCleared(placeholder)
            if (!isClearedByUs) {
                maybeRemoveAttachStateListener()
            }
            return
        }
        
        // Priority 2: Check if View is in RecyclerView (use cache first, then detection)
        val isInReusable = isInReusableContainer ?: ViewDetachScenarioDetector.isInReusableContainer(view)
        
        AniFluxLog.i(AniFluxLogCategory.TARGET, "onLoadCleared - isInReusable (cached: ${isInReusableContainer != null}): $isInReusable, hostDestroyed: $isHostDestroyed")
        
        // Priority 3: If in RecyclerView or can't detect but host is alive, keep resources
        if (isInReusable || (!view.isAttachedToWindow && (isAnimationPaused || isInReusableContainer == null))) {
            AniFluxLog.i(AniFluxLogCategory.TARGET, "onLoadCleared - RecyclerView recycling detected, keeping animation resources (not releasing)")
            // Only clear Request-related state, not animation resources
            sizeDeterminer.clearCallbacksAndListener()
            cleanupPlayListeners()
            onResourceCleared(placeholder)
            if (!isClearedByUs) {
                maybeRemoveAttachStateListener()
            }
            return
        }
        
        // Real destruction scenario: perform full cleanup
        AniFluxLog.i(AniFluxLogCategory.TARGET, "onLoadCleared - Real destruction scenario detected, performing full cleanup")
        performFullCleanup("RequestCleared")
        // performFullCleanup already handles all cleanup, but we need to call onResourceCleared for placeholder
        sizeDeterminer.clearCallbacksAndListener()
        onResourceCleared(placeholder)
        if (!isClearedByUs) {
            maybeRemoveAttachStateListener()
        }
    }
    
    /**
     * Add animation play listener (replace old one)
     * 
     * @param listener Listener instance
     * @return Whether addition was successful
     */
    fun addPlayListener(listener: AnimationPlayListener?): Boolean {
        if (listener == null) return false
        playListener = listener
        return true
    }
    
    /**
     * Remove animation play listener
     * 
     * @param listener Listener instance (used to verify if it's the current listener)
     * @return Whether removal was successful
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
     * Clear listener
     */
    fun clearPlayListener() {
        playListener = null
    }
    
    /**
     * Setup animation play listener on resource
     * Call this method after setting resource in onResourceReady, will automatically set listener to corresponding animation object
     * 
     * Note: Specific setup logic is in AnimationPlayListenerSetupHelper in aniflux module
     * This provides interface, format-specific setup is handled by format modules
     * 
     * @param resource Animation resource (PAGFile, LottieDrawable, SVGADrawable, GifDrawable, etc.)
     * @param view View displaying animation (optional, for animation types like PAG/Lottie that need View)
     */
    open fun setupPlayListeners(resource: Any, view: View? = null) {
        // Use AnimationPlayListenerSetupHelper for format-specific setup
        AnimationPlayListenerSetupHelper.setupListeners(this, resource, view)
    }
    
    /**
     * Cleanup listener setup
     * Automatically called in onLoadCleared, also called in onDestroy
     * 
     * Note: Specific cleanup logic is in AnimationPlayListenerSetupHelper in aniflux module
     * This provides interface, format-specific cleanup is handled by format modules
     */
    internal fun cleanupPlayListeners() {
        // Use AnimationPlayListenerSetupHelper for format-specific cleanup
        AnimationPlayListenerSetupHelper.cleanup(this)
        playListener = null
    }

    fun waitForLayout(): CustomViewAnimationTarget<T, Z> {
        sizeDeterminer.waitForLayout = true
        return this
    }


    fun clearOnDetach(): CustomViewAnimationTarget<T, Z> {
        // Disable auto cleanup when user explicitly calls clearOnDetach()
        autoCleanupEnabled = false
        
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
        // Use setTag method without key, directly store object
        view.tag = tag
    }

    private fun getTag(): Any? {
        // Use getTag method without key
        return view.tag
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
            throw IllegalArgumentException("Invalid tag type for CustomViewAnimationTarget")
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
     * Get associated Lifecycle (if exists)
     * Find corresponding Activity or Fragment's Lifecycle through View
     */
    protected fun getLifecycle(): androidx.lifecycle.Lifecycle? {
        val context = view.context ?: return null
        
        // Try to get Activity from Context
        val activity = findActivity(context) ?: return null
        
        // If FragmentActivity, try to find Fragment
        if (activity is androidx.fragment.app.FragmentActivity) {
            val fragment = findSupportFragment(view, activity)
            if (fragment != null) {
                return fragment.lifecycle
            }
            return activity.lifecycle
        }
        
        // Standard Activity (requires AndroidX Activity)
        if (activity is androidx.lifecycle.LifecycleOwner) {
            return activity.lifecycle
        }
        
        return null
    }
    
    /**
     * Get host Activity/Fragment Lifecycle (for checking host destruction)
     * This is used to check if the host container is destroyed, even if View is in a RecyclerView
     */
    private fun getHostLifecycle(): androidx.lifecycle.Lifecycle? {
        val context = view.context ?: return null
        val activity = findActivity(context) ?: return null
        
        // Always return Activity's lifecycle as host lifecycle
        // Fragment's lifecycle is already handled by getLifecycle()
        if (activity is androidx.lifecycle.LifecycleOwner) {
            return activity.lifecycle
        }
        
        return null
    }
    
    /**
     * Find Fragment that owns the View
     * Uses the same logic as AnimationRequestManagerRetriever: traverse Fragment's view hierarchy
     */
    private fun findSupportFragment(view: View, activity: androidx.fragment.app.FragmentActivity): androidx.fragment.app.Fragment? {
        // Use the same logic as AnimationRequestManagerRetriever
        val tempViewToFragment = mutableMapOf<View, androidx.fragment.app.Fragment>()
        findAllSupportFragmentsWithViews(activity.supportFragmentManager.fragments, tempViewToFragment)
        
        var result: androidx.fragment.app.Fragment? = null
        val activityRoot = activity.findViewById<View>(android.R.id.content)
        var current: View? = view
        
        while (current != null && current != activityRoot) {
            result = tempViewToFragment[current]
            if (result != null) break
            
            current = if (current.parent is View) {
                current.parent as View
            } else {
                break
            }
        }
        return result
    }
    
    /**
     * Find all support Fragments and their child Fragments (same as AnimationRequestManagerRetriever)
     */
    private fun findAllSupportFragmentsWithViews(
        topLevelFragments: Collection<androidx.fragment.app.Fragment>?,
        result: MutableMap<View, androidx.fragment.app.Fragment>
    ) {
        if (topLevelFragments == null) return
        
        for (fragment in topLevelFragments) {
            val fmView = fragment.view ?: continue
            result[fmView] = fragment
            findAllSupportFragmentsWithViews(
                fragment.childFragmentManager.fragments,
                result
            )
        }
    }
    
    /**
     * Find Activity from Context
     */
    private fun findActivity(context: Context): android.app.Activity? {
        return when (context) {
            is android.app.Activity -> context
            is android.content.ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
    }
    
    // ========== Memory Leak Protection Methods ==========
    
    /**
     * Setup lifecycle observer (primary judgment basis)
     * When lifecycle is destroyed, release resources
     */
    @SuppressLint("LongLogTag")
    private fun setupLifecycleObserver() {
        val lifecycle = getLifecycle()
        if (lifecycle == null) {
            // No lifecycle, use View detach as judgment (but need stricter check)
            if (autoCleanupEnabled && !ViewDetachScenarioDetector.isInReusableContainer(view)) {
                setupMemoryLeakProtectionAttachListenerForNoLifecycle()
            }
            return
        }
        
        lifecycleObserver = LifecycleEventObserver { source, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    pauseAnimationOnly()
                }
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                    performFullCleanup("LifecycleDestroy")
                }
                else -> {
                    // Other events not handled
                }
            }
        }.also {
            try {
                lifecycle.addObserver(it)
            } catch (e: Exception) {
                AniFluxLog.e(AniFluxLogCategory.TARGET, "Failed to add lifecycle observer", e)
            }
        }
    }
    
    /**
     * Setup View attach/detach listener for memory leak protection
     * Used to handle RecyclerView and other recycling scenarios
     */
    @SuppressLint("LongLogTag")
    private fun setupMemoryLeakProtectionAttachListener() {
        memoryLeakProtectionAttachListener = object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                // Update cache when View is attached
                updateReusableContainerCache()
                
                // View re-attached, resume animation (if previously paused)
                if (isAnimationPaused && !isResourceReleased) {
                    resumeAnimationIfNeeded()
                }
            }
            
            override fun onViewDetachedFromWindow(v: View) {
                // View detached, need to judge if it's recycling or real destruction
                handleViewDetached()
            }
        }
        
        // Pre-detect if View is already attached
        updateReusableContainerCache()
        
        try {
            view.addOnAttachStateChangeListener(memoryLeakProtectionAttachListener)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "Failed to add memory leak protection attach listener", e)
        }
    }
    
    /**
     * Setup View attach listener for no lifecycle scenario
     * Handles Dialog/PopupWindow scenarios where Lifecycle is not available
     */
    @SuppressLint("LongLogTag")
    private fun setupMemoryLeakProtectionAttachListenerForNoLifecycle() {
        memoryLeakProtectionAttachListener = object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                // Update cache when View is attached
                updateReusableContainerCache()
                
                if (isAnimationPaused && !isResourceReleased) {
                    AniFluxLog.i(AniFluxLogCategory.TARGET, "ViewAttached - Resuming paused animation")
                    resumeAnimationIfNeeded()
                }
            }
            
            override fun onViewDetachedFromWindow(v: View) {
                // No lifecycle scenario:
                // 1. If in Dialog/PopupWindow, detach means dismiss = real destruction
                // 2. If not in reusable container, likely real destruction
                // 3. Otherwise, consider recycling
                val isInDialog = ViewDetachScenarioDetector.isInDialog(view)
                val isInPopup = ViewDetachScenarioDetector.isInPopupWindow(view)
                val isInReusable = ViewDetachScenarioDetector.isInReusableContainer(view)
                val isRealDestroy = isInDialog || isInPopup || !isInReusable
                
                val scenarioInfo = buildString {
                    append("ViewDetached_NoLifecycle - ")
                    append("InDialog: $isInDialog, ")
                    append("InPopup: $isInPopup, ")
                    append("InReusable: $isInReusable, ")
                    append("IsRealDestroy: $isRealDestroy")
                }
                AniFluxLog.i(AniFluxLogCategory.TARGET, scenarioInfo)
                
                if (isRealDestroy) {
                    val reason = when {
                        isInDialog -> "ViewDetached_NoLifecycle_DialogDismissed"
                        isInPopup -> "ViewDetached_NoLifecycle_PopupDismissed"
                        else -> "ViewDetached_NoLifecycle"
                    }
                    performFullCleanup(reason)
                } else {
                    AniFluxLog.i(AniFluxLogCategory.TARGET, "ViewDetached_NoLifecycle - Recycling scenario, pausing animation only")
                    pauseAnimationOnly()
                }
            }
        }
        
        try {
            view.addOnAttachStateChangeListener(memoryLeakProtectionAttachListener)
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "Failed to add memory leak protection attach listener", e)
        }
    }
    
    /**
     * Handle View detach event
     * Key: Distinguish between recycling scenario and real destruction
     * 
     * Priority:
     * 1. Check host Activity/Fragment Lifecycle - if destroyed, release resources even if View is in RecyclerView
     * 2. Check if in Dialog/PopupWindow - if yes, release resources
     * 3. Check if in RecyclerView - if yes and host is alive, only pause
     * 4. Otherwise, release resources
     */
    private fun handleViewDetached() {
        val lifecycle = getLifecycle()
        val hostLifecycle = getHostLifecycle()
        
        // Detect scenario for logging
        val isInDialog = ViewDetachScenarioDetector.isInDialog(view)
        val isInPopup = ViewDetachScenarioDetector.isInPopupWindow(view)
        val isInRecyclerView = ViewDetachScenarioDetector.isInRecyclerView(view)
        val isInReusable = ViewDetachScenarioDetector.isInReusableContainer(view)
        
        // ✅ Priority 1: Check host Lifecycle - if host is destroyed, release resources even if View is in RecyclerView
        if (hostLifecycle?.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) {
            AniFluxLog.i(AniFluxLogCategory.TARGET, "ViewDetached - Host Lifecycle DESTROYED, releasing resources (even if in RecyclerView)")
            performFullCleanup("HostLifecycleDestroyed")
            return
        }
        
        // ✅ Priority 2: Check if in Dialog/PopupWindow
        // For PopupWindow: always release immediately (even if has Activity lifecycle, PopupWindow dismiss should be handled immediately)
        // For Dialog: 
        //   - Normal Dialog (no Fragment lifecycle): release immediately
        //   - DialogFragment (has Fragment lifecycle): Lifecycle Observer will handle it, skip here to avoid duplicate
        if (isInPopup) {
            // PopupWindow: always release immediately when dismissed
            AniFluxLog.i(AniFluxLogCategory.TARGET, "ViewDetached - PopupWindow dismissed, releasing resources")
            performFullCleanup("ViewDetached_PopupDismissed")
            return
        }
        if (isInDialog) {
            // Check if it's DialogFragment (has Fragment lifecycle) or normal Dialog
            val fragment = if (lifecycle != null && view.context is androidx.fragment.app.FragmentActivity) {
                findSupportFragment(view, view.context as androidx.fragment.app.FragmentActivity)
            } else {
                null
            }
            if (fragment != null && fragment is androidx.fragment.app.DialogFragment) {
                // DialogFragment has lifecycle, will be handled by Lifecycle Observer
                // Skip here to avoid duplicate cleanup (Lifecycle Observer will call performFullCleanup)
                AniFluxLog.i(AniFluxLogCategory.TARGET, "ViewDetached - DialogFragment detected (has lifecycle), skipping (will be handled by Lifecycle Observer)")
                return
            }
            // Normal Dialog without DialogFragment, detach means dismiss
            AniFluxLog.i(AniFluxLogCategory.TARGET, "ViewDetached - Normal Dialog dismissed, releasing resources")
            performFullCleanup("ViewDetached_DialogDismissed")
            return
        }
        
        // Judge if it's real destruction (original logic)
        val isRealDestroy = ViewDetachScenarioDetector.isLikelyRealDestroy(view, lifecycle)
        
        // Always log scenario detection (INFO level for visibility)
        val scenarioInfo = buildString {
            append("ViewDetached - ")
            append("Lifecycle: ${lifecycle?.currentState ?: "null"}, ")
            append("HostLifecycle: ${hostLifecycle?.currentState ?: "null"}, ")
            append("InDialog: $isInDialog, ")
            append("InPopup: $isInPopup, ")
            append("InRecyclerView: $isInRecyclerView, ")
            append("InReusable: $isInReusable, ")
            append("IsRealDestroy: $isRealDestroy")
        }
        AniFluxLog.i(AniFluxLogCategory.TARGET, scenarioInfo)
        
        if (isRealDestroy) {
            // Real destruction: release resources
            val reason = when {
                lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED -> "ViewDetached_LifecycleDestroyed"
                else -> "ViewDetached_RealDestroy"
            }
            performFullCleanup(reason)
        } else {
            // RecyclerView recycling scenario: only pause animation, don't release resources
            AniFluxLog.i(AniFluxLogCategory.TARGET, "ViewDetached - RecyclerView recycling, pausing animation only (not releasing resources)")
            pauseAnimationOnly()
        }
    }
    
    /**
     * Only pause animation, don't release resources
     * Used for RecyclerView recycling scenarios
     */
    @SuppressLint("LongLogTag", "Range")
    private fun pauseAnimationOnly() {
        synchronized(this) {
            if (isAnimationPaused || isResourceReleased) {
                if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.DEBUG)) {
                    AniFluxLog.d(AniFluxLogCategory.TARGET, "Skipping pause (already paused or released)")
                }
                return
            }
            isAnimationPaused = true
        }
        
        AniFluxLog.i(AniFluxLogCategory.TARGET, "⏸️  Pausing animation only (RecyclerView recycling - resources NOT released)")
        
        try {
            stopAnimation()
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "Error pausing animation", e)
        }
    }
    
    /**
     * Resume animation (if previously paused)
     */
    @SuppressLint("LongLogTag")
    private fun resumeAnimationIfNeeded() {
        synchronized(this) {
            if (!isAnimationPaused || isResourceReleased) {
                return
            }
            isAnimationPaused = false
        }
        
        AniFluxLog.i(AniFluxLogCategory.TARGET, "▶️  Resuming animation (View re-attached)")
        
        try {
            resumeAnimation()
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "Error resuming animation", e)
        }
    }
    
    /**
     * Perform full cleanup (release resources)
     * Only called when truly destroyed
     */
    @SuppressLint("Range")
    private fun performFullCleanup(reason: String) {
        // Double-check to avoid duplicate cleanup
        synchronized(this) {
            if (isResourceReleased) {
                if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.DEBUG)) {
                    AniFluxLog.d(AniFluxLogCategory.TARGET, "Skipping cleanup (already released): $reason")
                }
                return
            }
            isResourceReleased = true
        }
        
        if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.INFO)) {
            AniFluxLog.i(AniFluxLogCategory.TARGET, "═══════════════════════════════════════════════════════════")
            AniFluxLog.i(AniFluxLogCategory.TARGET, "🔴 Performing FULL CLEANUP: $reason")
            AniFluxLog.i(AniFluxLogCategory.TARGET, "   View: ${view.javaClass.simpleName}@${Integer.toHexString(view.hashCode())}")
        }
        
        try {
            // 1. Stop animation
            if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "   Step 1: Stopping animation...")
            }
            stopAnimation()
            
            // 2. Clear View resources
            if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "   Step 2: Clearing animation resources from view...")
            }
            clearAnimationFromView()
            
            // 3. Cleanup listeners
            if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "   Step 3: Cleaning up play listeners...")
            }
            cleanupPlayListeners()
            
            // 4. Check if Request has been cleared (avoid duplicate cleanup)
            val request = getRequest()
            if (request != null && !request.isCleared()) {
                if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.DEBUG)) {
                    AniFluxLog.d(AniFluxLogCategory.TARGET, "   Step 4: Clearing AnimationRequest...")
                }
                // Request still exists and not cleared, clear it
                request.clear()
            } else {
                if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.DEBUG)) {
                    AniFluxLog.d(AniFluxLogCategory.TARGET, "   Step 4: AnimationRequest already cleared (skipped)")
                }
                // Request already cleared (possibly by AnimationRequestManager)
                // Only clear View resources
            }
            
            // 5. Remove all listeners
            if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.DEBUG)) {
                AniFluxLog.d(AniFluxLogCategory.TARGET, "   Step 5: Removing all memory leak protection listeners...")
            }
            removeMemoryLeakProtectionListeners()
            
            if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.INFO)) {
                AniFluxLog.i(AniFluxLogCategory.TARGET, "✅ FULL CLEANUP completed successfully: $reason")
                AniFluxLog.i(AniFluxLogCategory.TARGET, "═══════════════════════════════════════════════════════════")
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "❌ Error during cleanup: $reason", e)
        }
    }
    
    /**
     * Remove all memory leak protection listeners
     */
    private fun removeMemoryLeakProtectionListeners() {
        try {
            lifecycleObserver?.let { observer ->
                getLifecycle()?.removeObserver(observer)
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "Error removing lifecycle observer", e)
        }
        lifecycleObserver = null
        
        try {
            memoryLeakProtectionAttachListener?.let { listener ->
                view.removeOnAttachStateChangeListener(listener)
            }
        } catch (e: Exception) {
            AniFluxLog.e(AniFluxLogCategory.TARGET, "Error removing memory leak protection attach listener", e)
        }
        memoryLeakProtectionAttachListener = null
        
        // Clear ViewDetachScenarioDetector cache for this view
        ViewDetachScenarioDetector.removeFromCache(view)
    }
    
    // ========== Abstract Methods (to be implemented by subclasses) ==========
    
    /**
     * Stop animation (to be implemented by subclasses)
     * Note: Only stop playback, don't release resources
     */
    protected abstract fun stopAnimation()
    
    /**
     * Resume animation (to be implemented by subclasses, optional)
     * Default empty implementation, subclasses can override
     */
    protected open fun resumeAnimation() {
        // Default empty implementation
    }
    
    /**
     * Restart animation if needed (called when system animation settings change)
     * This method checks if animation should be restarted and calls resumeAnimation()
     * 
     * @return true if animation was restarted, false otherwise
     */
    @JvmName("restartAnimationIfNeeded")
    fun restartAnimationIfNeeded(): Boolean {
        // Check if view is attached and resource is not released
        if (isResourceReleased || !view.isAttachedToWindow) {
            return false
        }
        
        // Check if request exists and is not cleared
        val request = getRequest()
        if (request == null || request.isCleared()) {
            return false
        }
        
        // Check if animation should auto-play (default: true)
        val autoPlay = animationOptions?.autoPlay ?: true
        if (!autoPlay) {
            return false
        }
        
        // Restart animation
        try {
            AniFluxLog.d(
                AniFluxLogCategory.TARGET,
                "Restarting animation after system animation settings change"
            )
            resumeAnimation()
            return true
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.TARGET,
                "Error restarting animation after system animation settings change",
                e
            )
            return false
        }
    }
    
    /**
     * Clear animation resources from View (to be implemented by subclasses)
     * Note: This is the key method for releasing resources
     */
    protected abstract fun clearAnimationFromView()


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
                // View is not visible, add to callback list, wait for View to become visible
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
                if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.INFO)) {
                    AniFluxLog.i(
                        AniFluxLogCategory.TARGET,
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
                if (AniFluxLog.isLoggable(TAG, AniFluxLogLevel.VERBOSE)) {
                    AniFluxLog.v(AniFluxLogCategory.TARGET, "OnGlobalLayoutListener called attachStateListener=$this")
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
