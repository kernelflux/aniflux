package com.kernelflux.aniflux.manager

import com.kernelflux.aniflux.request.AnimationRequest
import com.kernelflux.aniflux.util.Util
import java.util.Collections
import java.util.WeakHashMap

/**
 * @author: kerneflux
 * @date: 2025/10/12
 *
 */
class AnimationRequestTracker {
    private val requests = Collections.newSetFromMap(WeakHashMap<AnimationRequest, Boolean>())
    private val pendingRequests = HashSet<AnimationRequest>()

    private var isPaused = false

    fun runRequest(request: AnimationRequest) {
        requests.add(request)
        if (!isPaused) {
            request.begin()
        } else {
            request.clear()
            pendingRequests.add(request)
        }
    }

    fun addRequest(request: AnimationRequest) {
        requests.add(request)
    }

    fun clearAndRemove(request: AnimationRequest?): Boolean {
        val tmpRequest = request ?: return true
        var isOwnedByUs = requests.remove(tmpRequest)
        // Avoid short circuiting.
        isOwnedByUs = pendingRequests.remove(tmpRequest) || isOwnedByUs
        if (isOwnedByUs) {
            tmpRequest.clear()
        }
        return isOwnedByUs
    }

    fun isPaused(): Boolean {
        return isPaused
    }

    fun pauseRequests() {
        isPaused = true
        for (request in Util.getSnapshot(requests)) {
            if (request.isRunning()) {
                request.pause()
                pendingRequests.add(request)
            }
        }
    }

    fun pauseAllRequests() {
        isPaused = true
        for (request in Util.getSnapshot(requests)) {
            if (request.isRunning() || request.isComplete()) {
                request.clear()
                pendingRequests.add(request)
            }
        }
    }

    fun resumeRequests() {
        isPaused = false
        for (request in Util.getSnapshot(requests)) {
            if (!request.isComplete() && !request.isRunning()) {
                request.begin()
            }
        }
        pendingRequests.clear()
    }

    fun clearRequests() {
        for (request in Util.getSnapshot(requests)) {
            clearAndRemove(request)
        }
        pendingRequests.clear()
    }

    fun restartRequests() {
        for (request in Util.getSnapshot(requests)) {
            request.clear()
            if (!isPaused) {
                request.begin()
            } else {
                // Ensure the request will be restarted in onResume.
                pendingRequests.add(request)
            }
        }
    }

    override fun toString(): String {
        return super.toString() + "{numRequests=" + requests.size + ", isPaused=" + isPaused + "}"
    }

}
