package com.kernelflux.aniflux.manager

import com.kernelflux.aniflux.AnimationRequestManager
import java.util.Collections

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
class EmptyAnimationRequestManagerTreeNode : AnimationRequestManagerTreeNode {
    override fun getDescendants(): Set<AnimationRequestManager> {
        return Collections.emptySet()
    }
}

