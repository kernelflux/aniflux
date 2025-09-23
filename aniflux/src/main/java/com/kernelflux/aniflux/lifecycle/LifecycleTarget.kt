package com.kernelflux.aniflux.lifecycle

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * @author: kerneflux
 * @date: 2025/9/21
 * 定义生命周期目标的密封类
 */
sealed class LifecycleTarget {
    data class ContextTarget(val context: Context) : LifecycleTarget()
    data class ActivityTarget(val activity: FragmentActivity) : LifecycleTarget()
    data class FragmentTarget(val fragment: Fragment) : LifecycleTarget()
    data class ViewTarget(val view: View) : LifecycleTarget()
}