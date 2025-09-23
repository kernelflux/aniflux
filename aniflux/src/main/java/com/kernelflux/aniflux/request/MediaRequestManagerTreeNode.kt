package com.kernelflux.aniflux.request

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 请求管理器树节点 - 用于管理Fragment层级关系
 */
interface MediaRequestManagerTreeNode {
    fun getDescendants(): Set<MediaRequestManager>
}