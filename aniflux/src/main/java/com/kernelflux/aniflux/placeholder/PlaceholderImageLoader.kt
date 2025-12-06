package com.kernelflux.aniflux.placeholder

import android.content.Context
import android.graphics.Bitmap

/**
 * Placeholder image loader interface
 * Business logic needs to implement this interface, can use any image loading framework (Glide, Coil, Picasso, etc.)
 * 
 * Framework only calls this interface, doesn't care about specific implementation
 * 
 * @author: kerneflux
 * @date: 2025/11/27
 */
interface PlaceholderImageLoader {
    
    /**
     * Load image
     * 
     * @param context Context
     * @param source Image source (can be URL, File, Uri, ResourceId, etc., defined by business logic)
     * @param width Target width (0 means no limit, use original size)
     * @param height Target height (0 means no limit, use original size)
     * @param callback Load callback (called on main thread)
     * @return Load request (for cancellation)
     */
    fun load(
        context: Context,
        source: Any,
        width: Int = 0,
        height: Int = 0,
        callback: PlaceholderImageLoadCallback
    ): PlaceholderImageLoadRequest
    
    /**
     * Cancel load request
     * 
     * @param request Request to cancel
     */
    fun cancel(request: PlaceholderImageLoadRequest)
}

/**
 * Image load callback
 * All callbacks are executed on main thread
 */
interface PlaceholderImageLoadCallback {
    /**
     * Load success
     * 
     * @param bitmap Loaded Bitmap
     * Note: Framework will manage Bitmap lifecycle, business logic doesn't need to call recycle()
     * But if business logic needs to hold Bitmap long-term, should create a copy
     */
    fun onSuccess(bitmap: Bitmap)
    
    /**
     * Load failure
     * 
     * @param error Error information
     */
    fun onError(error: Throwable)
}

/**
 * Image load request (for cancellation)
 */
interface PlaceholderImageLoadRequest {
    /**
     * Whether cancelled
     */
    fun isCancelled(): Boolean
    
    /**
     * Cancel request
     * After cancellation, callbacks won't be called
     */
    fun cancel()
}

