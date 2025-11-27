package com.kernelflux.anifluxsample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.cache.AnimationCacheStrategy
import com.kernelflux.aniflux.into
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.vap.AnimView

/**
 * VAP 动画测试 Fragment
 * 用于测试 Tab 切换时动画是否自动暂停
 */
class VAPTestFragment : BaseLazyFragment() {

    private lateinit var vapImageView: AnimView
    private lateinit var tvStatus: TextView
    private lateinit var tvVisibility: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var visibilityCheckRunnable: Runnable? = null

    private val tabName: String by lazy {
        arguments?.getString(ARG_TAB_NAME) ?: "Tab"
    }

    private val vapUrl: String by lazy {
        arguments?.getString(ARG_VAP_URL)
            ?: "http://imgcom.static.suishenyun.net/c6a3e39be73229d8a2ca2be5662b5a49.gif"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_vap_test, container, false)

        vapImageView = view.findViewById(R.id.vap_image_view)
        tvStatus = view.findViewById(R.id.tv_status)
        tvVisibility = view.findViewById(R.id.tv_visibility)

        // 设置标题
        view.findViewById<TextView>(R.id.tv_title).text = tabName

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AniFluxLogger.i("[$tabName] onViewCreated")

        // 启动可见性监控（不在这里加载动画，等懒加载）
        startVisibilityMonitoring()
    }

    override fun onLoadData() {
        // ✅ 懒加载：只有在 Fragment 真正可见时才加载动画
        AniFluxLogger.i("[$tabName] onLoadData - Fragment 可见，开始加载动画")
        loadVapAnimation()
    }

    override fun onInvisible() {
        // Fragment 变为不可见时的处理
        AniFluxLogger.i("[$tabName] onInvisible - Fragment 不可见")
    }

    override fun onResume() {
        super.onResume()
        AniFluxLogger.i("[$tabName] Fragment onResume - isAttachedToWindow: ${vapImageView.isAttachedToWindow}, isShown: ${vapImageView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onPause() {
        super.onPause()
        AniFluxLogger.i("[$tabName] Fragment onPause - isAttachedToWindow: ${vapImageView.isAttachedToWindow}, isShown: ${vapImageView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        AniFluxLogger.i("[$tabName] Fragment onHiddenChanged: hidden=$hidden - isAttachedToWindow: ${vapImageView.isAttachedToWindow}, isShown: ${vapImageView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AniFluxLogger.i("[$tabName] Fragment onDestroyView")
        stopVisibilityMonitoring()
    }

    private fun loadVapAnimation() {
        AniFluxLogger.i("[$tabName] 开始加载 VAP 动画: $vapUrl")
        tvStatus.text = "状态：加载中..."

        AniFlux.with(requireContext())
            .asFile()
            .load("asset://vap1.mp4")
            .cacheStrategy(AnimationCacheStrategy.BOTH)
            .retainLastFrame(false)
            .playListener(object : AnimationPlayListener {
                override fun onAnimationStart() {
                    AniFluxLogger.i("[$tabName] VAP动画开始播放")
                    handler.post {
                        tvStatus.text = "状态：播放中"
                    }
                }

                override fun onAnimationEnd() {
                    AniFluxLogger.i("[$tabName] VAP动画播放结束")
                    handler.post {
                        tvStatus.text = "状态：播放结束"
                    }
                }

                override fun onAnimationCancel() {
                    AniFluxLogger.i("[$tabName] VAP动画播放取消")
                    handler.post {
                        tvStatus.text = "状态：已取消"
                    }
                }

                override fun onAnimationRepeat() {
                    AniFluxLogger.i("[$tabName] VAP动画重复播放 ⚠️")
                    handler.post {
                        tvStatus.text = "状态：重复播放中"
                    }
                }

                override fun onAnimationFailed(error: Throwable?) {
                    AniFluxLogger.i("[$tabName] VAP动画播放失败: ${error?.message}")
                    handler.post {
                        tvStatus.text = "状态：加载失败"
                    }
                }
            })
            .into(vapImageView)
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
        if (!::vapImageView.isInitialized || !::tvVisibility.isInitialized) {
            return
        }

        val isAttached = vapImageView.isAttachedToWindow
        val isShown = vapImageView.isShown()
        val visibility = when (vapImageView.visibility) {
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
        private const val ARG_VAP_URL = "vap_url"

        fun newInstance(tabName: String, pagUrl: String): VAPTestFragment {
            return VAPTestFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TAB_NAME, tabName)
                    putString(ARG_VAP_URL, pagUrl)
                }
            }
        }
    }
}

