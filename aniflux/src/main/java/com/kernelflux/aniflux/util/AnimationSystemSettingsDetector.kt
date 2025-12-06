package com.kernelflux.aniflux.util

import android.content.ContentResolver
import android.provider.Settings
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory

/**
 * System animation settings detector
 * Detects whether system animations are disabled in developer options
 * 
 * When users disable animations in developer options, it can cause:
 * - Animations not playing (duration scale = 0)
 * - App lagging or freezing
 * - Poor user experience
 * 
 * @author: kernelflux
 * @date: 2025/12/06
 */
object AnimationSystemSettingsDetector {
    
    /**
     * Check if system animations are enabled
     * 
     * @param contentResolver ContentResolver to access system settings
     * @return true if animations are enabled, false if disabled
     */
    @JvmStatic
    fun areAnimationsEnabled(contentResolver: ContentResolver): Boolean {
        return try {
            val animatorDurationScale = Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val windowAnimationScale = Settings.Global.getFloat(
                contentResolver,
                Settings.Global.WINDOW_ANIMATION_SCALE,
                1.0f
            )
            val transitionAnimationScale = Settings.Global.getFloat(
                contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            
            // If any animation scale is 0, animations are disabled
            val enabled = animatorDurationScale > 0f && 
                        windowAnimationScale > 0f && 
                        transitionAnimationScale > 0f
            
            if (!enabled) {
                AniFluxLog.w(
                    AniFluxLogCategory.GENERAL,
                    "System animations are disabled: animator=$animatorDurationScale, window=$windowAnimationScale, transition=$transitionAnimationScale"
                )
            }
            
            enabled
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.GENERAL,
                "Failed to check system animation settings",
                e
            )
            // Default to enabled if we can't check
            true
        }
    }
    
    /**
     * Get animator duration scale
     * This is the most critical setting for ValueAnimator-based animations
     * 
     * @param contentResolver ContentResolver to access system settings
     * @return Animator duration scale (0.0f if disabled, 1.0f if normal, etc.)
     */
    @JvmStatic
    fun getAnimatorDurationScale(contentResolver: ContentResolver): Float {
        return try {
            Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.GENERAL,
                "Failed to get animator duration scale",
                e
            )
            1.0f
        }
    }
    
    /**
     * Check if animator duration scale is disabled (0.0f)
     * This affects ValueAnimator, ObjectAnimator, and animations based on them
     * 
     * @param contentResolver ContentResolver to access system settings
     * @return true if animator duration scale is 0 (disabled)
     */
    @JvmStatic
    fun isAnimatorDurationScaleDisabled(contentResolver: ContentResolver): Boolean {
        return getAnimatorDurationScale(contentResolver) == 0f
    }
}

