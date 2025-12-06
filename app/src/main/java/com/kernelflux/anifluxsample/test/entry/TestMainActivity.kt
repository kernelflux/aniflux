package com.kernelflux.anifluxsample.test.entry

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kernelflux.anifluxsample.databinding.ActivityTestMainBinding

/**
 * Test Main Activity - Entry point for testing
 * Links to:
 * 1. MainActivity - Current functional page
 * 2. MemoryLeakTestActivity - Memory leak prevention test scenarios
 */
class TestMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Link to current functional page
        binding.btnMainActivity.setOnClickListener {
            startActivity(Intent(this, com.kernelflux.anifluxsample.main.MainActivity::class.java))
        }

        // Link to memory leak test scenarios
        binding.btnMemoryLeakTest.setOnClickListener {
            startActivity(Intent(this, com.kernelflux.anifluxsample.test.memoryleak.MemoryLeakTestActivity::class.java))
        }
    }
}

