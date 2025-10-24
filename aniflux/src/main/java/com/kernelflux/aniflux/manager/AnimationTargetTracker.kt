package com.kernelflux.aniflux.manager

import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.Util
import java.util.Collections
import java.util.WeakHashMap

/**
 * @author: kerneflux
 * @date: 2025/10/12
 *
 */
class AnimationTargetTracker : AnimationLifecycleListener {
    private val targets = Collections.newSetFromMap(WeakHashMap<AnimationTarget<*>, Boolean>())

    fun track(target: AnimationTarget<*>) {
        targets.add(target)
    }

    fun untrack(target: AnimationTarget<*>) {
        targets.remove(target)
    }

    override fun onStart() {
        for (target in Util.getSnapshot(targets)) {
            target.onStart()
        }
    }

    override fun onStop() {
        for (target in Util.getSnapshot(targets)) {
            target.onStop()
        }
    }

    override fun onDestroy() {
        for (target in Util.getSnapshot(targets)) {
            target.onDestroy()
        }
    }

    fun getAll(): List<AnimationTarget<*>> {
        return Util.getSnapshot(targets)
    }

    fun clear() {
        targets.clear()
    }
}
