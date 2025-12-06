package com.kernelflux.anifluxsample.test.memoryleak

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.kernelflux.anifluxsample.databinding.ActivityMemoryLeakTestBinding

/**
 * Memory Leak Test Activity
 * Contains test scenarios for memory leak prevention:
 * 1. RecyclerView scroll scenario
 * 2. Activity/Fragment destroy scenario
 * 3. Dialog/PopupWindow scenario
 * 4. Exception handling scenario
 */
class MemoryLeakTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryLeakTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryLeakTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
    }

    private fun setupTabs() {
        val fragments = listOf<Fragment>(
            com.kernelflux.anifluxsample.test.memoryleak.recyclerview.RecyclerViewTestFragment(),
            com.kernelflux.anifluxsample.test.memoryleak.destroy.ActivityDestroyTestFragment(),
            com.kernelflux.anifluxsample.test.memoryleak.dialog.DialogTestFragment(),
            com.kernelflux.anifluxsample.test.memoryleak.exception.ExceptionTestFragment()
        )

        val titles = listOf(
            "RecyclerView",
            "Activity/Fragment",
            "Dialog/Popup",
            "Exception"
        )

        val adapter = MemoryLeakTestPagerAdapter(this, fragments)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }
}

