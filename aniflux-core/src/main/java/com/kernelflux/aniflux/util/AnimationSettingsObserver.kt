package com.kernelflux.aniflux.util

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.kernelflux.aniflux.log.AniFluxLog
import com.kernelflux.aniflux.log.AniFluxLogCategory

/**
 * ContentObserver for monitoring system animation settings changes
 * Handles runtime changes when user disables animations during app usage
 * 
 * @author: kernelflux
 * @date: 2025/12/06
 */
internal class AnimationSettingsObserver(
    private val contentResolver: android.content.ContentResolver,
    private val onSettingsChanged: () -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {
    
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        
        // Check current animation state
        val isDisabled = AnimationSystemSettingsDetector.isAnimatorDurationScaleDisabled(contentResolver)
        
        if (isDisabled) {
            AniFluxLog.w(
                AniFluxLogCategory.GENERAL,
                "System animations disabled during runtime. Applying compatibility fixes..."
            )
            
            // Apply compatibility fixes immediately
            // This ensures animations continue to work even if user disables them while app is running
            AnimationCompatibilityHelper.ensureValueAnimatorCompatibility(contentResolver)
            
            // Notify callback
            onSettingsChanged()
        } else {
            // Animations were re-enabled
            // Note: We keep ValueAnimator.durationScale at 1.0f even when system animations are enabled
            // This is safe and ensures consistent behavior
            AniFluxLog.d(
                AniFluxLogCategory.GENERAL,
                "System animation settings changed during runtime (animations re-enabled)"
            )
        }
    }
    
    /**
     * Register this observer to monitor animation settings
     */
    fun register() {
        try {
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                this
            )
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.WINDOW_ANIMATION_SCALE),
                false,
                this
            )
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE),
                false,
                this
            )
            
            AniFluxLog.d(
                AniFluxLogCategory.GENERAL,
                "Animation settings observer registered"
            )
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.GENERAL,
                "Failed to register animation settings observer",
                e
            )
        }
    }
    
    /**
     * Unregister this observer
     */
    fun unregister() {
        try {
            contentResolver.unregisterContentObserver(this)
            AniFluxLog.d(
                AniFluxLogCategory.GENERAL,
                "Animation settings observer unregistered"
            )
        } catch (e: Exception) {
            AniFluxLog.e(
                AniFluxLogCategory.GENERAL,
                "Failed to unregister animation settings observer",
                e
            )
        }
    }
}

