package com.kernelflux.anifluxsample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.into
import com.kernelflux.aniflux.request.listener.AnimationPlayListener
import com.kernelflux.aniflux.util.CacheStrategy
import org.libpag.PAGView

/**
 * PAG 动画测试 Fragment
 * 用于测试 Tab 切换时动画是否自动暂停
 */
class PAGTestFragment : Fragment() {

    private lateinit var pagView: PAGView
    private lateinit var tvStatus: TextView
    private lateinit var tvVisibility: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var visibilityCheckRunnable: Runnable? = null

    private val tabName: String by lazy {
        arguments?.getString(ARG_TAB_NAME) ?: "Tab"
    }

    private val pagUrl: String by lazy {
        arguments?.getString(ARG_PAG_URL)
            ?: "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_pag_test, container, false)

        pagView = view.findViewById(R.id.pag_view)
        tvStatus = view.findViewById(R.id.tv_status)
        tvVisibility = view.findViewById(R.id.tv_visibility)

        // 设置标题
        view.findViewById<TextView>(R.id.tv_title).text = tabName

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AniFluxLogger.i("[$tabName] onViewCreated")

        // 加载 PAG 动画
        loadPAGAnimation()

        // 启动可见性监控
        startVisibilityMonitoring()
    }

    override fun onResume() {
        super.onResume()
        AniFluxLogger.i("[$tabName] Fragment onResume - isAttachedToWindow: ${pagView.isAttachedToWindow}, isShown: ${pagView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onPause() {
        super.onPause()
        AniFluxLogger.i("[$tabName] Fragment onPause - isAttachedToWindow: ${pagView.isAttachedToWindow}, isShown: ${pagView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        AniFluxLogger.i("[$tabName] Fragment onHiddenChanged: hidden=$hidden - isAttachedToWindow: ${pagView.isAttachedToWindow}, isShown: ${pagView.isShown()}")
        updateVisibilityStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AniFluxLogger.i("[$tabName] Fragment onDestroyView")
        stopVisibilityMonitoring()
    }

    private fun loadPAGAnimation() {
        AniFluxLogger.i("[$tabName] 开始加载 PAG 动画: $pagUrl")
        tvStatus.text = "状态：加载中..."

        AniFlux.with(requireContext())
            .asPAG()
            .load(pagUrl)
            .repeatCount(3)
            .cacheStrategy(CacheStrategy.ALL)
            .retainLastFrame(false)
            .playListener(object : AnimationPlayListener {
                override fun onAnimationStart() {
                    AniFluxLogger.i("[$tabName] PAG动画开始播放")
                    handler.post {
                        tvStatus.text = "状态：播放中"
                    }
                }

                override fun onAnimationEnd() {
                    AniFluxLogger.i("[$tabName] PAG动画播放结束")
                    handler.post {
                        tvStatus.text = "状态：播放结束"
                    }
                }

                override fun onAnimationCancel() {
                    AniFluxLogger.i("[$tabName] PAG动画播放取消")
                    handler.post {
                        tvStatus.text = "状态：已取消"
                    }
                }

                override fun onAnimationRepeat() {
                    AniFluxLogger.i("[$tabName] PAG动画重复播放 ⚠️")
                    handler.post {
                        tvStatus.text = "状态：重复播放中"
                    }
                }

                override fun onAnimationFailed(error: Throwable?) {
                    AniFluxLogger.i("[$tabName] PAG动画播放失败: ${error?.message}")
                    handler.post {
                        tvStatus.text = "状态：加载失败"
                    }
                }
            })
            .into(pagView)
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
        if (!::pagView.isInitialized || !::tvVisibility.isInitialized) {
            return
        }

        val isAttached = pagView.isAttachedToWindow
        val isShown = pagView.isShown()
        val visibility = when (pagView.visibility) {
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
        private const val ARG_PAG_URL = "pag_url"

        fun newInstance(tabName: String, pagUrl: String): PAGTestFragment {
            return PAGTestFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TAB_NAME, tabName)
                    putString(ARG_PAG_URL, pagUrl)
                }
            }
        }
    }
}

