package com.kernelflux.anifluxsample

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

/**
 * Fragment 懒加载基类
 * 确保只有在 Fragment 真正可见时才加载数据（动画）
 *
 * ViewPager2 + Fragment 的可见性判断：
 * 1. onResume() + isVisible() + isAdded() + !isHidden()
 * 2. ViewPager2 会预加载相邻页面，但不可见的页面不应该加载动画
 * 3. 通过 setUserVisibleHint() 和 onHiddenChanged() 来控制
 *
 * @author: kerneflux
 * @date: 2025/11/03
 */
abstract class BaseLazyFragment : Fragment() {

    /**
     * Fragment 是否已经初始化视图
     */
    private var isViewCreated = false

    /**
     * Fragment 当前是否可见（对用户可见）
     * 综合判断：isVisible + isResumed + !isHidden
     */
    private var isVisibleToUser = false

    /**
     * 是否已经加载过数据（动画）
     */
    private var hasLoadedData = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewCreated = true
        // 视图创建后，如果已经可见，则触发懒加载
        if (userVisibleHint || (!isHidden && isResumed)) {
            tryLoadData()
        }
    }

    override fun onResume() {
        super.onResume()
        // Fragment 恢复时，如果可见，触发懒加载
        if (isViewCreated && !isHidden && isVisible) {
            if (!isVisibleToUser) {
                isVisibleToUser = true
                tryLoadData()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Fragment 暂停时，标记为不可见
        if (isVisibleToUser) {
            isVisibleToUser = false
            onInvisible()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ 修复：View 被销毁时，重置状态
        // 当 Tab 切换跨度大时，ViewPager2 会销毁 Fragment 的 View（onDestroyView）
        // 但 Fragment 实例可能还在，切回来时会重新创建 View（onViewCreated）
        // 如果不重置 hasLoadedData，动画不会被重新加载
        isViewCreated = false
        hasLoadedData = false  // 重置加载状态，下次可见时重新加载
        isVisibleToUser = false
    }

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        // ViewPager2 会调用这个方法（虽然已废弃，但仍可用）
        if (isViewCreated) {
            if (isVisibleToUser && !this.isVisibleToUser) {
                // 从不可见变为可见
                this.isVisibleToUser = true
                tryLoadData()
            } else if (!isVisibleToUser && this.isVisibleToUser) {
                // 从可见变为不可见
                this.isVisibleToUser = false
                onInvisible()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isViewCreated) {
            if (!hidden && !isVisibleToUser) {
                // Fragment 从隐藏变为显示
                isVisibleToUser = true
                tryLoadData()
            } else if (hidden && isVisibleToUser) {
                // Fragment 从显示变为隐藏
                isVisibleToUser = false
                onInvisible()
            }
        }
    }

    /**
     * 尝试加载数据（动画）
     * 只有在满足条件时才真正加载：
     * 1. 视图已创建
     * 2. Fragment 可见
     * 3. 还没有加载过数据
     */
    private fun tryLoadData() {
        if (isViewCreated && isFragmentVisible() && !hasLoadedData) {
            hasLoadedData = true
            onLoadData()
        }
    }

    /**
     * 判断 Fragment 是否真正可见
     */
    private fun isFragmentVisible(): Boolean {
        return isAdded && !isHidden && isResumed && isVisible && userVisibleHint
    }

    /**
     * Fragment 从可见变为不可见时的回调
     * 子类可以重写此方法来处理资源释放等
     */
    protected open fun onInvisible() {
        // 默认不处理，子类可以重写
    }

    /**
     * 懒加载数据（动画）
     * 子类必须实现此方法，在这里加载动画
     * 此方法只会在 Fragment 真正可见时调用一次
     */
    protected abstract fun onLoadData()

    /**
     * 重置加载状态（如果需要重新加载）
     */
    protected fun resetLoadState() {
        hasLoadedData = false
        if (isFragmentVisible()) {
            tryLoadData()
        }
    }

    /**
     * 检查当前是否可见
     */
    protected fun isCurrentlyVisible(): Boolean {
        return isFragmentVisible()
    }
}

