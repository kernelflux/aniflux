package com.kernelflux.aniflux

import android.content.Context
import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.target.CustomAnimationTarget

/**
 * @author: kerneflux
 * @date: 2025/10/13
 *
 */
class AnimationRequestBuilder<T>(
    private val aniFlux: AniFlux,
    private val requestManager: AnimationRequestManager,
    private val context: Context
) {


    private fun isSkipMemoryCacheWithCompletePreviousRequest(previous: AnimationRequest): Boolean {
        return previous.isComplete()
    }


    private fun <Y : CustomAnimationTarget<T>> into(
        target: Y,
        targetListener: AnimationRequestListener<T>?
    ): Y {
        val request = buildRequest(target, targetListener)
        val previous = target.getRequest()
        if (previous != null &&
            request.isEquivalentTo(previous) &&
            !isSkipMemoryCacheWithCompletePreviousRequest(previous)
        ) {
            if (!previous.isRunning()) {
                previous.begin()
            }
            return target
        }
        requestManager.clear(target)
        target.setRequest(request)
        requestManager.track(target, request)

        return target
    }

    private fun buildRequest(
        target: CustomAnimationTarget<T>,
        targetListener: AnimationRequestListener<T>?
    ): AnimationRequest {
        throw IllegalStateException("")
    }

}