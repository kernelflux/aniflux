package com.kernelflux.aniflux.manager

import com.kernelflux.aniflux.AnimationRequestManager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
interface AnimationRequestManagerTreeNode {
    /**
     * Get all child RequestManagers
     */
    fun getDescendants(): Set<AnimationRequestManager>

}