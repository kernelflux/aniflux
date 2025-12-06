package com.kernelflux.aniflux.util

import android.view.View
import androidx.lifecycle.Lifecycle
import java.util.Collections
import java.util.WeakHashMap

/**
 * View detach scenario detector
 * Distinguishes between view recycling (e.g., RecyclerView) and real destruction
 * 
 * @author: kernelflux
 * @date: 2025/12/06
 */
object ViewDetachScenarioDetector {
    
    // Cache for container type detection to avoid repeated parent traversal
    // Use WeakHashMap to avoid memory leaks
    private val viewToContainerType = Collections.synchronizedMap(WeakHashMap<View, Boolean>())
    
    // Cache for Dialog/PopupWindow detection (detected when View is attached)
    private val viewToDialogType = Collections.synchronizedMap(WeakHashMap<View, Boolean>())
    private val viewToPopupType = Collections.synchronizedMap(WeakHashMap<View, Boolean>())
    
    /**
     * Detects if View is in RecyclerView
     */
    fun isInRecyclerView(view: View): Boolean {
        var parent = view.parent
        while (parent != null) {
            // Method 1: Check class name (fast)
            val className = parent.javaClass.name
            if (className.contains("RecyclerView")) {
                return true
            }
            
            // Method 2: Check interface (more reliable)
            // RecyclerView implements Recycler interface
            try {
                val recyclerInterface = Class.forName("androidx.recyclerview.widget.RecyclerView\$Recycler")
                if (recyclerInterface.isInstance(parent)) {
                    return true
                }
            } catch (e: Exception) {
                // Ignore
            }
            
            parent = parent.parent
        }
        return false
    }
    
    /**
     * Detects if View is in ListView/GridView (deprecated but may still be used)
     */
    fun isInAbsListView(view: View): Boolean {
        var parent = view.parent
        while (parent != null) {
            val className = parent.javaClass.name
            if (className.contains("AbsListView")) {
                return true
            }
            parent = parent.parent
        }
        return false
    }
    
    /**
     * Detects if View is in ViewPager (ViewPager2 is also RecyclerView)
     */
    fun isInViewPager(view: View): Boolean {
        var parent = view.parent
        while (parent != null) {
            val className = parent.javaClass.name
            if (className.contains("ViewPager")) {
                return true
            }
            parent = parent.parent
        }
        return false
    }
    
    /**
     * Detects if View is inside a Dialog
     * Uses cache if available, otherwise detects and caches the result
     * 
     * Detection method:
     * 1. Check if View's root view is a Dialog's decor view (Dialog uses Window)
     * 2. Check parent class names for Dialog-related classes
     */
    fun isInDialog(view: View): Boolean {
        // Check cache first
        viewToDialogType[view]?.let { return it }
        
        val result = detectDialogInternal(view)
        viewToDialogType[view] = result
        return result
    }
    
    private fun detectDialogInternal(view: View): Boolean {
        try {
            // Method 1: Check if root view's parent is a Window (Dialog uses Window)
            val rootView = view.rootView
            if (rootView != null) {
                // Dialog's decor view is added to Window
                // We can check if the root view is not the Activity's content view
                val context = view.context
                if (context is android.app.Activity) {
                    val activityRoot = context.findViewById<View>(android.R.id.content)
                    // If root view is not the Activity's content view, it might be in a Dialog
                    if (rootView != activityRoot && rootView.parent != null) {
                        // Check if parent is a Window (Dialog scenario)
                        val parentClass = rootView.parent.javaClass.name
                        if (parentClass.contains("Window") || parentClass.contains("PhoneWindow")) {
                            return true
                        }
                    }
                }
            }
            
            // Method 2: Check parent hierarchy for Dialog-related classes
            var parent = view.parent
            while (parent != null) {
                val className = parent.javaClass.name
                if (className.contains("Dialog") || 
                    className.contains("AlertDialog") ||
                    className.contains("AppCompatDialog")) {
                    return true
                }
                parent = parent.parent
            }
        } catch (e: Exception) {
            // Ignore exceptions during detection
        }
        return false
    }
    
    /**
     * Detects if View is inside a PopupWindow
     * Uses cache if available, otherwise detects and caches the result
     * 
     * Detection method:
     * 1. Check parent class names for PopupWindow-related classes
     * 2. PopupWindow's content view is typically wrapped in a PopupWindowContainer
     */
    fun isInPopupWindow(view: View): Boolean {
        // Check cache first
        viewToPopupType[view]?.let { return it }
        
        val result = detectPopupWindowInternal(view)
        viewToPopupType[view] = result
        return result
    }
    
    private fun detectPopupWindowInternal(view: View): Boolean {
        try {
            var parent = view.parent
            while (parent != null) {
                val className = parent.javaClass.name
                // PopupWindow uses PopupWindowContainer or similar wrapper
                if (className.contains("PopupWindow") || 
                    className.contains("PopupWindowContainer") ||
                    className.contains("PopupDecorView")) {
                    return true
                }
                parent = parent.parent
            }
        } catch (e: Exception) {
            // Ignore exceptions during detection
        }
        return false
    }
    
    /**
     * Determines if View is in a reusable container (RecyclerView, ListView, ViewPager, etc.)
     * Uses cache to avoid repeated parent traversal
     */
    fun isInReusableContainer(view: View): Boolean {
        return viewToContainerType.getOrPut(view) {
            isInRecyclerView(view) || 
            isInAbsListView(view) || 
            isInViewPager(view)
        }
    }
    
    /**
     * Determines if detach is likely real destruction (not recycling)
     * 
     * Judgment criteria:
     * 1. Lifecycle is DESTROYED - definitely real destruction
     * 2. In Dialog/PopupWindow - when detach, it's real destruction (Dialog/PopupWindow dismissed)
     * 3. Not in reusable container - likely real destruction
     * 4. Other cases - considered recycling
     */
    fun isLikelyRealDestroy(
        view: View,
        lifecycle: Lifecycle?
    ): Boolean {
        // 1. If lifecycle is DESTROYED, definitely real destruction
        if (lifecycle?.currentState == Lifecycle.State.DESTROYED) {
            return true
        }
        
        // 2. If in Dialog/PopupWindow, when View detaches, it means Dialog/PopupWindow is dismissed
        // This is real destruction, not recycling
        if (isInDialog(view) || isInPopupWindow(view)) {
            return true
        }
        
        // 3. If not in reusable container, likely real destruction
        if (!isInReusableContainer(view)) {
            return true
        }
        
        // 4. Other cases, considered recycling
        return false
    }
    
    /**
     * Clears cache (can be called when View is destroyed to free memory)
     */
    fun clearCache() {
        viewToContainerType.clear()
    }
    
    /**
     * Removes specific View from cache
     */
    fun removeFromCache(view: View) {
        viewToContainerType.remove(view)
        viewToDialogType.remove(view)
        viewToPopupType.remove(view)
    }
    
    /**
     * Pre-detect and cache Dialog/PopupWindow status when View is attached
     * This ensures we can detect Dialog/PopupWindow even after View is detached
     */
    fun preDetectDialogAndPopup(view: View) {
        // Pre-detect and cache when View is attached (parent hierarchy is available)
        if (view.isAttachedToWindow) {
            isInDialog(view)  // This will cache the result
            isInPopupWindow(view)  // This will cache the result
        }
    }
    
    /**
     * Pre-detect and cache reusable container status when View is attached
     * This ensures we can detect RecyclerView/ListView/ViewPager even after View is detached
     * 
     * Important: This should be called when View is attached, because:
     * 1. When View is detached from RecyclerView, view.parent may become null or change
     * 2. After sliding many pages, View may be recycled to cache pool, parent hierarchy is lost
     * 3. We need to cache the result when View is still attached to RecyclerView
     */
    fun preDetectReusableContainer(view: View) {
        // Pre-detect and cache when View is attached (parent hierarchy is available)
        if (view.isAttachedToWindow) {
            isInReusableContainer(view)  // This will cache the result via getOrPut
        }
    }
}

