package com.kernelflux.anifluxsample

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Tab + ViewPager2 适配器
 */
class TabPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        PAGTestFragment.newInstance(
            "Tab 1",
            "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        ),
        PAGTestFragment.newInstance(
            "Tab 2",
            "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        ),
        SVGATestFragment.newInstance(
            "Tab 3",
            "http://peanut-oss.weli010.cn/img/gift/29_ani.svga"
        ),
        GIFTestFragment.newInstance(
            "Tab 4",
            "http://imgcom.static.suishenyun.net/c6a3e39be73229d8a2ca2be5662b5a49.gif"
        ),
        VAPTestFragment.newInstance(
            "Tab 5",
            "https://github.com/user-attachments/assets/19308e3b-23d3-46e5-a7a8-cc184301caca"
        )
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getTabTitle(position: Int): String {
        return when (position) {
            0 -> "Tab 1"
            1 -> "Tab 2"
            2 -> "Tab 3"
            3 -> "Tab 4"
            4 -> "Tab 5"
            else -> "Tab ${position + 1}"
        }
    }
}

