package com.kernelflux.anifluxsample.test.memoryleak.destroy

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.pag.asPAG
import com.kernelflux.aniflux.pag.into
import com.kernelflux.anifluxsample.R
import com.kernelflux.pag.PAGImageView
import com.kernelflux.anifluxsample.util.AniFluxLogger

/**
 * Activity/Fragment destroy test scenario
 * Tests memory leak prevention when Activity/Fragment is destroyed
 */
class ActivityDestroyTestFragment : Fragment() {

    private lateinit var pagImageView: PAGImageView
    private lateinit var tvInfo: TextView
    private lateinit var btnStartActivity: Button
    private lateinit var btnShowFragment: Button
    private lateinit var btnDismissFragment: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_activity_destroy_test, container, false)

        pagImageView = view.findViewById(R.id.pag_image_view)
        tvInfo = view.findViewById(R.id.tv_info)
        btnStartActivity = view.findViewById(R.id.btn_start_activity)
        btnShowFragment = view.findViewById(R.id.btn_show_fragment)
        btnDismissFragment = view.findViewById(R.id.btn_dismiss_fragment)

        setupViews()
        loadAnimation()

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        tvInfo.text = """
            Activity/Fragment Destroy Test
            ==============================
            • Click buttons to start new Activity/Fragment
            • Press back to destroy and check resource cleanup
            • Verify animations are properly released
            • Monitor memory usage before and after destroy
        """.trimIndent()

        btnStartActivity.setOnClickListener {
            startActivity(Intent(requireContext(), TestDestroyActivity::class.java))
        }

        btnShowFragment.setOnClickListener {
            val existedFragment = childFragmentManager.findFragmentByTag("test_destroy_fragment")
            existedFragment?.also {
                childFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            }
            val fragment = TestDestroyFragment()
            childFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment, "test_destroy_fragment")
                .commitAllowingStateLoss()
        }

        btnDismissFragment.setOnClickListener {
            val existedFragment = childFragmentManager.findFragmentByTag("test_destroy_fragment")
            existedFragment?.also {
                childFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            }
        }
    }

    private fun loadAnimation() {
        val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        AniFlux.with(requireContext())
            .asPAG()
            .load(pagUrl)
            .into(pagImageView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AniFluxLogger.i("ActivityDestroyTestFragment onDestroyView - resources should be released")
    }
}

/**
 * Test Activity for destroy scenario
 */
class TestDestroyActivity : androidx.appcompat.app.AppCompatActivity() {

    private lateinit var pagImageView: PAGImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_destroy)

        pagImageView = findViewById(R.id.pag_image_view)

        val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        AniFlux.with(this)
            .asPAG()
            .load(pagUrl)
            .into(pagImageView)
    }

    override fun onDestroy() {
        super.onDestroy()
        AniFluxLogger.i("TestDestroyActivity onDestroy - resources should be released")
    }
}

/**
 * Test Fragment for destroy scenario
 */
class TestDestroyFragment : Fragment() {

    private lateinit var pagImageView: PAGImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_test_destroy, container, false)
        pagImageView = view.findViewById(R.id.pag_image_view)
        loadAnimation()

        return view
    }

    private fun loadAnimation() {
        val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        AniFlux.with(this)
            .asPAG()
            .load(pagUrl)
            .into(pagImageView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AniFluxLogger.i("TestDestroyFragment onDestroyView - resources should be released")
    }
}

