package com.kernelflux.aniflux.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory

/**
 * Animation compatibility helper
 * Provides solutions to ensure animations work correctly when system animations are disabled
 * 
 * Based on industry best practices:
 * 1. Force ValueAnimator duration scale to 1.0f (for SVGA, PAG, etc.)
 * 2. Use Lottie's ignoreDisabledSystemAnimations API
 * 3. Provide fallback mechanisms
 * 
 * @author: kernelflux
 * @date: 2025/12/06
 */
object AnimationCompatibilityHelper {
    
    private var isValueAnimatorScaleFixed = false
    private var settingsObserver: AnimationSettingsObserver? = null
    
    /**
     * Fix ValueAnimator duration scale to 1.0f
     * This ensures ValueAnimator-based animations (SVGA, PAG, etc.) work correctly
     * even when system animations are disabled
     * 
     * This is a global fix that affects all ValueAnimators in the app
     * 
     * @return true if fix was applied successfully, false otherwise
     */
    @SuppressLint("PrivateApi")
    @JvmStatic
    fun fixValueAnimatorDurationScale(): Boolean {
        if (isValueAnimatorScaleFixed) {
            return true
        }
        
        return try {
            val clazz = Class.forName("android.animation.ValueAnimator")
            val method = clazz.getDeclaredMethod("setDurationScale", Float::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(null, 1.0f)
            isValueAnimatorScaleFixed = true
            
            AniFluxLog.i(
                AniFluxLogCategory.GENERAL,
                "ValueAnimator duration scale fixed to 1.0f to ensure animations work when system animations are disabled"
            )
            true
        } catch (e: Exception) {
            AniFluxLog.w(
                AniFluxLogCategory.GENERAL,
                "Failed to fix ValueAnimator duration scale (may not be available on this Android version)",
                e
            )
            false
        }
    }
    
    /**
     * Check if ValueAnimator duration scale fix is needed and apply it
     * 
     * @param contentResolver ContentResolver to check system settings
     * @return true if fix was applied or not needed, false if fix failed
     */
    @JvmStatic
    fun ensureValueAnimatorCompatibility(contentResolver: ContentResolver): Boolean {
        if (AnimationSystemSettingsDetector.isAnimatorDurationScaleDisabled(contentResolver)) {
            return fixValueAnimatorDurationScale()
        }
        return true
    }
    
    /**
     * Initialize animation compatibility
     * Should be called during framework initialization
     * 
     * @param contentResolver ContentResolver to check system settings
     * @param enableRuntimeMonitoring Whether to monitor runtime changes (default: true)
     * @param onAnimationSettingsChanged Callback when animation settings change (e.g., to restart animations)
     */
    @JvmStatic
    fun initialize(
        contentResolver: ContentResolver,
        enableRuntimeMonitoring: Boolean = true,
        onAnimationSettingsChanged: (() -> Unit)? = null
    ) {
        val animationsEnabled = AnimationSystemSettingsDetector.areAnimationsEnabled(contentResolver)
        
        if (!animationsEnabled) {
            AniFluxLog.w(
                AniFluxLogCategory.GENERAL,
                "System animations are disabled. Applying compatibility fixes..."
            )
            
            // Fix ValueAnimator duration scale for SVGA, PAG, etc.
            ensureValueAnimatorCompatibility(contentResolver)
        } else {
            AniFluxLog.d(
                AniFluxLogCategory.GENERAL,
                "System animations are enabled. No compatibility fixes needed."
            )
        }
        
        // Register observer to monitor runtime changes
        if (enableRuntimeMonitoring) {
            registerSettingsObserver(contentResolver, onAnimationSettingsChanged)
        }
    }
    
    /**
     * Register ContentObserver to monitor system animation settings changes at runtime
     * This handles the case when user disables animations during app usage
     * 
     * @param contentResolver ContentResolver to register observer
     * @param onAnimationSettingsChanged Callback when animation settings change (e.g., to restart animations)
     */
    @JvmStatic
    fun registerSettingsObserver(
        contentResolver: ContentResolver,
        onAnimationSettingsChanged: (() -> Unit)? = null
    ) {
        // Unregister existing observer if any
        unregisterSettingsObserver()
        
        settingsObserver = AnimationSettingsObserver(contentResolver) {
            // Callback when settings change - compatibility fixes are already applied in observer
            AniFluxLog.i(
                AniFluxLogCategory.GENERAL,
                "Animation settings changed, compatibility fixes applied"
            )
            // Call the callback to restart animations
            onAnimationSettingsChanged?.invoke()
        }
        
        settingsObserver?.register()
    }
    
    /**
     * Unregister ContentObserver
     */
    @JvmStatic
    fun unregisterSettingsObserver() {
        settingsObserver?.unregister()
        settingsObserver = null
    }
    
    /**
     * Reset ValueAnimator duration scale to system default
     * This should only be used for testing or if you want to respect system settings
     * 
     * @param contentResolver ContentResolver to get system default
     * @return true if reset was successful
     */
    @SuppressLint("PrivateApi")
    @JvmStatic
    fun resetValueAnimatorDurationScale(contentResolver: ContentResolver): Boolean {
        return try {
            val systemScale = AnimationSystemSettingsDetector.getAnimatorDurationScale(contentResolver)
            val clazz = Class.forName("android.animation.ValueAnimator")
            val method = clazz.getDeclaredMethod("setDurationScale", Float::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(null, systemScale)
            isValueAnimatorScaleFixed = false
            
            AniFluxLog.i(
                AniFluxLogCategory.GENERAL,
                "ValueAnimator duration scale reset to system default: $systemScale"
            )
            true
        } catch (e: Exception) {
            AniFluxLog.w(
                AniFluxLogCategory.GENERAL,
                "Failed to reset ValueAnimator duration scale",
                e
            )
            false
        }
    }
}

