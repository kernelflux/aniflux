package com.kernelflux.anifluxsample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * MainActivity with Tab + Fragment 切换测试场景
 * 用于验证 PAG 动画在 Fragment 切换时是否可以自动暂停
 */
class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 使用带 Tab 的布局
        setContentView(R.layout.activity_main_with_tabs)
        
        AniFluxLogger.i("MainActivity onCreate")

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        
        // 创建适配器
        val adapter = TabPagerAdapter(this)
        viewPager.adapter = adapter
        
        // 绑定 TabLayout 和 ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()
        
        // 监听页面切换，观察动画行为
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                AniFluxLogger.i("MainActivity ViewPager 切换到页面: $position")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        AniFluxLogger.i("MainActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        AniFluxLogger.i("MainActivity onPause")
    }
}