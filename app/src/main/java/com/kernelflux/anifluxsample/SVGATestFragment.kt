package com.kernelflux.anifluxsample

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.aniflux.into
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.svgaplayer.SVGAImageView

/**
 * SVGA 动画测试 Fragment
 * 用于测试 Tab 切换时动画是否自动暂停
 */
class SVGATestFragment : Fragment() {

    private lateinit var svgaImageView: SVGAImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvVisibility: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var visibilityCheckRunnable: Runnable? = null

    private val tabName: String by lazy {
        arguments?.getString(ARG_TAB_NAME) ?: "Tab"
    }

    private val svgaUrl: String by lazy {
        arguments?.getString(ARG_SVGA_URL)
            ?: "https://peanut-oss.wemogu.net/client/animation/common/live_room_seat_boy.svga"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_svga_test, container, false)

        svgaImageView = view.findViewById(R.id.svga_image_view)
        tvStatus = view.findViewById(R.id.tv_status)
        tvVisibility = view.findViewById(R.id.tv_visibility)

        // 设置标题
        view.findViewById<TextView>(R.id.tv_title).text = tabName

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AniFluxLogger.i("[$tabName] onViewCreated")

        // 加载 SVGA 动画
        loadSVGAAnimation()

        // 启动可见性监控
        startVisibilityMonitoring()
    }

    override fun onResume() {
        super.onResume()
        AniFluxLogger.i("[$tabName] Fragment onResume - isAttachedToWindow: ${svgaImageView.isAttachedToWindow}, isShown: ${svgaImageView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onPause() {
        super.onPause()
        AniFluxLogger.i("[$tabName] Fragment onPause - isAttachedToWindow: ${svgaImageView.isAttachedToWindow}, isShown: ${svgaImageView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        AniFluxLogger.i("[$tabName] Fragment onHiddenChanged: hidden=$hidden - isAttachedToWindow: ${svgaImageView.isAttachedToWindow}, isShown: ${svgaImageView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AniFluxLogger.i("[$tabName] Fragment onDestroyView")
        stopVisibilityMonitoring()
    }

    private fun loadSVGAAnimation() {
        AniFluxLogger.i("[$tabName] 开始加载 SVGA 动画: $svgaUrl")
        tvStatus.text = "状态：加载中..."

        AniFlux.with(requireContext())
            .asSVGA()
            .load(svgaUrl)
            .repeatCount(3)
            .cacheStrategy(AnimationCacheStrategy.BOTH)
            .playListener(object : AnimationPlayListener {
                override fun onAnimationStart() {
                    AniFluxLogger.i("[$tabName] SVGA动画开始播放")
                    handler.post {
                        tvStatus.text = "状态：播放中"
                    }
                }

                override fun onAnimationEnd() {
                    AniFluxLogger.i("[$tabName] SVGA动画播放结束")
                    handler.post {
                        tvStatus.text = "状态：播放结束"
                    }
                }

                override fun onAnimationCancel() {
                    AniFluxLogger.i("[$tabName] SVGA动画播放取消")
                    handler.post {
                        tvStatus.text = "状态：已取消"
                    }
                }

                override fun onAnimationRepeat() {
                    AniFluxLogger.i("[$tabName] SVGA动画重复播放 ⚠️")
                    handler.post {
                        tvStatus.text = "状态：重复播放中"
                    }
                }

                override fun onAnimationFailed(error: Throwable?) {
                    AniFluxLogger.i("[$tabName] SVGA动画播放失败: ${error?.message}")
                    handler.post {
                        tvStatus.text = "状态：加载失败"
                    }
                }
            })
            .into(svgaImageView)
    }

    private fun startVisibilityMonitoring() {
        visibilityCheckRunnable = object : Runnable {
            override fun run() {
                updateVisibilityStatus()
                handler.postDelayed(this, 1000) // 每秒更新一次
            }
        }
        handler.post(visibilityCheckRunnable!!)
    }

    private fun stopVisibilityMonitoring() {
        visibilityCheckRunnable?.let {
            handler.removeCallbacks(it)
        }
        visibilityCheckRunnable = null
    }

    private fun updateVisibilityStatus() {
        if (!::svgaImageView.isInitialized || !::tvVisibility.isInitialized) {
            return
        }

        val isAttached = svgaImageView.isAttachedToWindow
        val isShown = svgaImageView.isShown()
        val visibility = when (svgaImageView.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }

        val status = "可见性：attached=$isAttached, shown=$isShown, visibility=$visibility"
        tvVisibility.text = status

        // 如果不可见，记录日志
        if (!isAttached || !isShown) {
         //   AniFluxLogger.i("[$tabName] View不可见: $status")
        }
    }

    companion object {
        private const val ARG_TAB_NAME = "tab_name"
        private const val ARG_SVGA_URL = "svga_url"

        fun newInstance(tabName: String, pagUrl: String): SVGATestFragment {
            return SVGATestFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TAB_NAME, tabName)
                    putString(ARG_SVGA_URL, pagUrl)
                }
            }
        }
    }
}

