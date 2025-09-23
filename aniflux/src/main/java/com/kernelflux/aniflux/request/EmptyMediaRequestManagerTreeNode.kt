package com.kernelflux.aniflux.request

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 空请求管理器树节点 - 用于没有FragmentManager的情况
 */
class EmptyMediaRequestManagerTreeNode : MediaRequestManagerTreeNode {
    override fun getDescendants(): Set<MediaRequestManager> {
        return emptySet()
    }
}