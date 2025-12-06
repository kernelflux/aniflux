package com.kernelflux.anifluxsample.test.memoryleak.exception

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.into
import com.kernelflux.anifluxsample.R
import com.kernelflux.pag.PAGImageView
import com.kernelflux.anifluxsample.util.AniFluxLogger

/**
 * Exception handling test scenario
 * Tests memory leak prevention in exception scenarios
 */
class ExceptionTestFragment : Fragment() {

    private lateinit var pagImageView: PAGImageView
    private lateinit var tvInfo: TextView
    private lateinit var btnInvalidUrl: Button
    private lateinit var btnNullView: Button
    private lateinit var btnRapidLoad: Button
    private lateinit var btnClearDuringLoad: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_exception_test, container, false)
        
        pagImageView = view.findViewById(R.id.pag_image_view)
        tvInfo = view.findViewById(R.id.tv_info)
        btnInvalidUrl = view.findViewById(R.id.btn_invalid_url)
        btnNullView = view.findViewById(R.id.btn_null_view)
        btnRapidLoad = view.findViewById(R.id.btn_rapid_load)
        btnClearDuringLoad = view.findViewById(R.id.btn_clear_during_load)
        
        setupViews()
        
        return view
    }

    private fun setupViews() {
        tvInfo.text = """
            Exception Handling Test
            ======================
            • Test invalid URL loading
            • Test null view handling
            • Test rapid load/unload cycles
            • Test clearing during loading
            • Verify no memory leaks in exception cases
        """.trimIndent()

        btnInvalidUrl.setOnClickListener {
            testInvalidUrl()
        }

        btnNullView.setOnClickListener {
            testNullView()
        }

        btnRapidLoad.setOnClickListener {
            testRapidLoad()
        }

        btnClearDuringLoad.setOnClickListener {
            testClearDuringLoad()
        }
    }

    private fun testInvalidUrl() {
        AniFluxLogger.i("Testing invalid URL...")
        try {
            AniFlux.with(requireContext())
                .asPAG()
                .load("https://invalid-url-that-does-not-exist.pag")
                .into(pagImageView)
        } catch (e: Exception) {
            AniFluxLogger.i("Invalid URL test exception: ${e.message}")
        }
    }

    private fun testNullView() {
        AniFluxLogger.i("Testing null view...")
//        try {
//            // This should be handled gracefully
//            val nullView: PAGImageView? = null
//            AniFlux.with(requireContext())
//                .asPAG()
//                .load("https://peanut-oss.wemogu.net/client/test/anim_linglu.pag")
//                .into(nullView)
//        } catch (e: Exception) {
//            AniFluxLogger.i("Null view test exception: ${e.message}")
//        }
    }

    private fun testRapidLoad() {
        AniFluxLogger.i("Testing rapid load/unload cycles...")
        val urls = listOf(
            "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag",
            "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag",
            "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        )
        
        urls.forEachIndexed { index, url ->
            pagImageView.postDelayed({
                AniFluxLogger.i("Rapid load cycle $index")
                AniFlux.with(requireContext())
                    .asPAG()
                    .load(url)
                    .into(pagImageView)
            }, index * 100L)
        }
    }

    private fun testClearDuringLoad() {
        AniFluxLogger.i("Testing clear during loading...")
        val request = AniFlux.with(requireContext())
            .asPAG()
            .load("https://peanut-oss.wemogu.net/client/test/anim_linglu.pag")
            .into(pagImageView)
        
        // Clear immediately while loading
        pagImageView.postDelayed({
            AniFluxLogger.i("Clearing request during load")
            // The view might be cleared, but resources should still be released
            pagImageView.composition = null
        }, 50L)
    }
}

