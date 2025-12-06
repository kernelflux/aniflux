package com.kernelflux.pag;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.kernelflux.pag.extra.tools.LibraryLoadUtils;

import java.lang.ref.WeakReference;

/**
 * PAGAnimator provides a simple timing engine for running animations.
 */
class PAGAnimator {
    /**
     * This interface is used to receive notifications when the status of the associated PAGAnimator
     * changes, and to flush the animation frame.
     */
    public interface Listener {
        /**
         * Notifies the start of the animation. This may be called from any thread that invokes the
         * start() method.
         */
        void onAnimationStart(PAGAnimator animator);

        /**
         * Notifies the end of the animation. This will only be called from the UI thread.
         */
        void onAnimationEnd(PAGAnimator animator);

        /**
         * Notifies the cancellation of the animation. This may be called from any thread that
         * invokes the cancel() method.
         */
        void onAnimationCancel(PAGAnimator animator);

        /**
         * Notifies the repetition of the animation. This will only be called from the UI thread.
         */
        void onAnimationRepeat(PAGAnimator animator);

        /**
         * Notifies another frame of the animation has occurred. This may be called from an
         * arbitrary thread if the animation is running asynchronously.
         */
        void onAnimationUpdate(PAGAnimator animator);
    }


    private WeakReference<Listener> weakListener = null;
    private float animationScale = 1.0f;

    /**
     * Creates a new PAGAnimator with the specified listener. PAGAnimator only holds a weak
     * reference to the listener.
     */
    public static PAGAnimator MakeFrom(Context context, Listener listener) {
        if (listener == null) {
            return null;
        }
        return new PAGAnimator(context, listener);
    }

    private PAGAnimator(Context context, Listener listener) {
        this.weakListener = new WeakReference<>(listener);
        if (context != null) {
            float systemScale = Settings.Global.getFloat(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            // Check if ValueAnimator duration scale has been fixed (e.g., by AniFlux compatibility helper)
            // If system scale is 0 but ValueAnimator scale is 1.0f, use 1.0f to allow animation to play
            float actualValueAnimatorScale = getValueAnimatorDurationScale();
            if (systemScale == 0.0f && actualValueAnimatorScale > 0.0f) {
                // System animations are disabled but ValueAnimator has been fixed, use the fixed value
                animationScale = actualValueAnimatorScale;
            } else {
                animationScale = systemScale;
            }
        }
        nativeSetup();
    }
    
    /**
     * Get the actual ValueAnimator duration scale using reflection
     * This checks if ValueAnimator.durationScale has been fixed (e.g., by AniFlux compatibility helper)
     * 
     * @return The actual ValueAnimator duration scale, or 1.0f if cannot be determined
     */
    @SuppressLint("PrivateApi")
    private float getValueAnimatorDurationScale() {
        try {
            // Try to get ValueAnimator's actual duration scale using reflection
            // This allows us to detect if it has been fixed by AniFlux compatibility helper
            java.lang.reflect.Method method = ValueAnimator.class.getDeclaredMethod("getDurationScale");
            method.setAccessible(true);
            Object result = method.invoke(null);
            if (result instanceof Float) {
                return (Float) result;
            }
        } catch (Exception e) {
            // Reflection failed, fall back to default
        }
        // If reflection fails, return 1.0f as default
        return 1.0f;
    }

    /**
     * Indicates whether the animation is allowed to run in the UI thread. The default value is
     * false.
     */
    public native boolean isSync();

    /**
     * Set whether the animation is allowed to run in the UI thread.
     */
    public native void setSync(boolean value);

    /**
     * Returns the length of the animation in microseconds.
     */
    public native long duration();

    /**
     * Sets the length of the animation in microseconds.
     */
    public native void setDuration(long duration);

    /**
     * The total number of times the animation is set to play. The default is 1, which means the
     * animation will play only once. If the repeat count is set to 0 or a negative value, the
     * animation will play infinity times.
     */
    public native int repeatCount();

    /**
     * Set the number of times the animation to play.
     */
    public native void setRepeatCount(int repeatCount);

    /**
     * Returns the current position of the animation, which is a number between 0.0 and 1.0.
     */
    public native double progress();

    /**
     * Set the current progress of the animation.
     */
    public native void setProgress(double value);

    /**
     * Indicates whether the animation is running.
     */
    public native boolean isRunning();

    /**
     * Starts the animation from the current position. Calling the start() method when the animation
     * is already started has no effect. The start() method does not alter the animation's current
     * position. However, if the animation previously reached its end, it will restart from the
     * beginning.
     */
    public void start() {
        // Re-check ValueAnimator duration scale at start time
        // This is important because ValueAnimator.durationScale might be fixed after PAGAnimator creation
        // (e.g., by AniFlux compatibility helper), so we need to update animationScale accordingly
        if (animationScale == 0.0f) {
            // Try to get the actual ValueAnimator duration scale
            float actualValueAnimatorScale = getValueAnimatorDurationScale();
            
            // Note: getDurationScale() may return the system setting value (0.0f) even if setDurationScale(1.0f) was called
            // This is because getDurationScale() might read from Settings.Global, not the actual runtime value.
            // However, if AniFlux compatibility helper has been called, ValueAnimator.setDurationScale(1.0f) was invoked,
            // which means animations should work. So we trust that if system scale is 0, but we're trying to start,
            // it's likely that the fix has been applied (even if getDurationScale() still returns 0.0f).
            
            // If system animations are disabled (animationScale == 0.0f), but we're trying to start the animation,
            // it's likely that AniFlux compatibility helper has fixed ValueAnimator.durationScale to 1.0f.
            // In this case, we should allow the animation to play by setting animationScale to 1.0f.
            if (actualValueAnimatorScale > 0.0f) {
                // ValueAnimator has been fixed, update animationScale to allow animation to play
                animationScale = actualValueAnimatorScale;
            } else {
                // getDurationScale() returned 0.0f, but this might be because it reads from system settings.
                // Since AniFlux compatibility helper calls setDurationScale(1.0f), we trust that the fix has been applied
                // and allow the animation to play by setting animationScale to 1.0f.
                // This is safe because setDurationScale(1.0f) actually fixes the runtime behavior, even if getDurationScale() doesn't reflect it.
                animationScale = 1.0f;
            }
        }
        doStart();
    }

    private native void doStart();

    /**
     * Cancels the animation at the current position. Calling the start() method can resume the
     * animation from the last canceled position.
     */
    public native void cancel();

    /**
     * Manually update the animation to the current progress without altering its playing status. If
     * isSync is set to false, the calling thread won't be blocked. Please note that if the
     * animation already has an ongoing asynchronous flushing task, this action won't have any
     * effect.
     */
    public native void update();

    private void onAnimationStart() {
        Listener listener = weakListener.get();
        if (listener != null) {
            listener.onAnimationStart(this);
        }
    }

    private void onAnimationEnd() {
        Listener listener = weakListener.get();
        if (listener != null) {
            listener.onAnimationEnd(this);
        }
    }

    private void onAnimationCancel() {
        Listener listener = weakListener.get();
        if (listener != null) {
            listener.onAnimationCancel(this);
        }
    }

    private void onAnimationRepeat() {
        Listener listener = weakListener.get();
        if (listener != null) {
            listener.onAnimationRepeat(this);
        }
    }

    private void onAnimationUpdate() {
        Listener listener = weakListener.get();
        if (listener != null) {
            listener.onAnimationUpdate(this);
        }
    }

    /**
     * Free up resources used by the PAGAnimator instance immediately instead of relying on the
     * garbage collector to do this for you at some point in the future.
     */
    public void release() {
        nativeRelease();
    }

    protected void finalize() {
        nativeFinalize();
    }

    private native void nativeRelease();

    private native void nativeFinalize();

    private native void nativeSetup();

    private static native void nativeInit();

    static {
        LibraryLoadUtils.loadLibrary("anifluxPag");
        nativeInit();
    }

    private long nativeContext = 0;
}