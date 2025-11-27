package com.kernelflux.anifluxsample

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
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
import com.kernelflux.svgaplayer.SVGADrawable
import com.kernelflux.svgaplayer.SVGADynamicEntity
import com.kernelflux.svgaplayer.SVGAImageView

/**
 * SVGA 动画测试 Fragment
 * 用于测试 Tab 切换时动画是否自动暂停
 */
class SVGATestFragment : BaseLazyFragment() {

    private lateinit var svgaImageView: SVGAImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvVisibility: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var visibilityCheckRunnable: Runnable? = null
    private var placeholderSetupRetryCount = 0
    private val MAX_PLACEHOLDER_SETUP_RETRIES = 5

    private val tabName: String by lazy {
        arguments?.getString(ARG_TAB_NAME) ?: "Tab"
    }

    private val svgaUrl: String by lazy {
        arguments?.getString(ARG_SVGA_URL)
            ?: "asset://123.svga"
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

        // 启动可见性监控（不在这里加载动画，等懒加载）
        startVisibilityMonitoring()
    }

    override fun onLoadData() {
        // ✅ 懒加载：只有在 Fragment 真正可见时才加载动画
        AniFluxLogger.i("[$tabName] onLoadData - Fragment 可见，开始加载动画")
        loadSVGAAnimation()
    }

    override fun onInvisible() {
        // Fragment 变为不可见时的处理
        AniFluxLogger.i("[$tabName] onInvisible - Fragment 不可见")
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

        // 在动画加载完成后设置占位图
        // 使用post延迟执行，确保drawable已经设置
        svgaImageView.postDelayed({
            setupPlaceholderImages()
        }, 100) // 延迟100ms，确保drawable已经准备好
    }

    /**
     * 设置SVGA占位图
     * 从assets加载user1.jpg和user2.jpg，替换user_1和user_2占位符
     */
    private fun setupPlaceholderImages() {
        try {
            val drawable = svgaImageView.drawable as? SVGADrawable
            val videoItem = drawable?.videoItem ?: run {
                // 如果drawable还没准备好，重试（最多重试5次）
                if (placeholderSetupRetryCount < MAX_PLACEHOLDER_SETUP_RETRIES) {
                    placeholderSetupRetryCount++
                    AniFluxLogger.i("[$tabName] 无法获取videoItem，重试设置占位图 ($placeholderSetupRetryCount/$MAX_PLACEHOLDER_SETUP_RETRIES)")
                    svgaImageView.postDelayed({
                        setupPlaceholderImages()
                    }, 100)
                } else {
                    AniFluxLogger.i("[$tabName] 无法获取videoItem，占位图设置失败（已达到最大重试次数）")
                }
                return
            }

            // 重置重试计数器
            placeholderSetupRetryCount = 0

            // 创建动态实体
            val dynamicEntity = SVGADynamicEntity()

            // 从assets加载user1.jpg
            try {
                val inputStream1 = requireContext().assets.open("user1.jpg")
                val bitmap1 = BitmapFactory.decodeStream(inputStream1)
                inputStream1.close()
                if (bitmap1 != null) {
                    dynamicEntity.setDynamicImage(bitmap1, "user_1")
                    AniFluxLogger.i("[$tabName] 成功设置占位图 user_1")
                } else {
                    AniFluxLogger.i("[$tabName] 无法解码 user1.jpg")
                }
            } catch (e: Exception) {
                AniFluxLogger.i("[$tabName] 加载 user1.jpg 失败:${e.message}")
            }

            // 从assets加载user2.jpg
            try {
                val inputStream2 = requireContext().assets.open("user2.jpg")
                val bitmap2 = BitmapFactory.decodeStream(inputStream2)
                inputStream2.close()
                if (bitmap2 != null) {
                    dynamicEntity.setDynamicImage(bitmap2, "user_2")
                    AniFluxLogger.i("[$tabName] 成功设置占位图 user_2")
                } else {
                    AniFluxLogger.i("[$tabName] 无法解码 user2.jpg")
                }
            } catch (e: Exception) {
                AniFluxLogger.i("[$tabName] 加载 user2.jpg 失败:${e.message}")
            }

            // 保存当前播放状态
            val wasAnimating = svgaImageView.isAnimating

            // 重新设置videoItem，应用动态实体
            svgaImageView.setVideoItem(videoItem, dynamicEntity)

            // 如果之前正在播放，重新启动动画
            if (wasAnimating) {
                svgaImageView.startAnimation()
            }

            AniFluxLogger.i("[$tabName] 占位图设置完成")
        } catch (e: Exception) {
            AniFluxLogger.i("[$tabName] 设置占位图时发生错误:${e.message}")
        }
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
        val isShown = svgaImageView.isShown
        val visibility = when (svgaImageView.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }

        val status = "可见性：attached=$isAttached, shown=$isShown, visibility=$visibility"
        tvVisibility.text = status
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

