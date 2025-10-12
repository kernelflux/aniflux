package com.kernelflux.aniflux.manager

import com.kernelflux.aniflux.AnimationRequestManager

/**
 * @author: kernelflux
 * @date: 2025/10/8
 */
interface AnimationRequestManagerTreeNode {
    /**
     * 获取所有子RequestManager
     */
    fun getDescendants(): Set<AnimationRequestManager>

}