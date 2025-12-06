/*
 * Tencent is pleased to support the open source community by making vap available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kernelflux.vap

import android.content.Context
import android.content.res.AssetManager
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import com.kernelflux.vap.file.AssetsFileContainer
import com.kernelflux.vap.file.FileContainer
import com.kernelflux.vap.file.IFileContainer
import com.kernelflux.vap.inter.IAnimListener
import com.kernelflux.vap.inter.IFetchResource
import com.kernelflux.vap.inter.OnResourceClickListener
import com.kernelflux.vap.mask.MaskConfig
import com.kernelflux.vap.textureview.InnerTextureView
import com.kernelflux.vap.util.ALog
import com.kernelflux.vap.util.IScaleType
import com.kernelflux.vap.util.ScaleType
import com.kernelflux.vap.util.ScaleTypeUtil
import java.io.File

open class AnimView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    IAnimView,
    FrameLayout(context, attrs, defStyleAttr),
    TextureView.SurfaceTextureListener {

    companion object {
        private const val TAG = "${Constant.TAG}.AnimView"
    }

    private lateinit var player: AnimPlayer

    private val uiHandler by lazy { Handler(Looper.getMainLooper()) }
    private var surface: SurfaceTexture? = null
    private var animListener: IAnimListener? = null
    private var innerTextureView: InnerTextureView? = null
    private var lastFile: IFileContainer? = null
    private val scaleTypeUtil = ScaleTypeUtil()

    /**
     * 是否保留最后一帧（动画结束时）
     * true: 保留最后一帧，不清空视图和文件
     * false: 清空最后一帧，移除视图并关闭文件（默认行为）
     */
    var retainLastFrame: Boolean = true
        set(value) {
            field = value
            // ✅ 如果 player 已初始化，同步 retainLastFrame
            if (::player.isInitialized) {
                player.retainLastFrame = value
            }
        }

    // 代理监听
    private val animProxyListener by lazy {
        object : IAnimListener {

            override fun onVideoConfigReady(config: AnimConfig): Boolean {
                scaleTypeUtil.setVideoSize(config.width, config.height)
                return animListener?.onVideoConfigReady(config) ?: super.onVideoConfigReady(config)
            }

            override fun onVideoStart() {
                animListener?.onVideoStart()
            }

            override fun onVideoRender(frameIndex: Int, config: AnimConfig?) {
                animListener?.onVideoRender(frameIndex, config)
            }

            override fun onVideoComplete() {
                // ✅ 根据 retainLastFrame 配置决定是否调用 hide()
                if (!retainLastFrame) {
                    hide()
                }
                animListener?.onVideoComplete()
            }

            override fun onVideoDestroy() {
                // ✅ onVideoDestroy 是资源销毁回调，无论 retainLastFrame 都应该清理
                hide()
                animListener?.onVideoDestroy()
            }

            override fun onFailed(errorType: Int, errorMsg: String?) {
                animListener?.onFailed(errorType, errorMsg)
            }

        }
    }

    // 保证AnimView已经布局完成才加入TextureView
    private var onSizeChangedCalled = false
    private var needPrepareTextureView = false
    private val prepareTextureViewRunnable = Runnable {
        removeAllViews()
        innerTextureView = InnerTextureView(context).apply {
            player = this@AnimView.player
            isOpaque = false
            surfaceTextureListener = this@AnimView
            layoutParams = scaleTypeUtil.getLayoutParam(this)
        }
        addView(innerTextureView)
    }


    init {
        hide()
        player = AnimPlayer(this)
        player.retainLastFrame = retainLastFrame
        player.animListener = animProxyListener
    }


    override fun prepareTextureView() {
        if (onSizeChangedCalled) {
            uiHandler.post(prepareTextureViewRunnable)
        } else {
            ALog.e(TAG, "onSizeChanged not called")
            needPrepareTextureView = true
        }
    }

    override fun getSurfaceTexture(): SurfaceTexture? {
        return innerTextureView?.surfaceTexture ?: surface
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        ALog.i(TAG, "onSurfaceTextureSizeChanged $width x $height")
        player.onSurfaceTextureSizeChanged(width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        ALog.i(TAG, "onSurfaceTextureDestroyed")
        this.surface = null
        player.onSurfaceTextureDestroyed()
        uiHandler.post {
            innerTextureView?.surfaceTextureListener = null
            innerTextureView = null
            removeAllViews()
        }
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        ALog.i(TAG, "onSurfaceTextureAvailable width=$width height=$height")
        this.surface = surface
        player.onSurfaceTextureAvailable(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ALog.i(TAG, "onSizeChanged w=$w, h=$h")
        scaleTypeUtil.setLayoutSize(w, h)
        onSizeChangedCalled = true
        // 需要保证onSizeChanged被调用
        if (needPrepareTextureView) {
            needPrepareTextureView = false
            prepareTextureView()
        }
    }

    override fun onAttachedToWindow() {
        ALog.i(TAG, "onAttachedToWindow")
        super.onAttachedToWindow()
        player.isDetachedFromWindow = false
        // 自动恢复播放
        if (player.playLoop > 0) {
            lastFile?.apply {
                startPlay(this)
            }
        }
    }

    override fun onDetachedFromWindow() {
        ALog.i(TAG, "onDetachedFromWindow")
        super.onDetachedFromWindow()
        player.isDetachedFromWindow = true
        player.onSurfaceTextureDestroyed()
    }


    override fun setAnimListener(animListener: IAnimListener?) {
        this.animListener = animListener
    }

    override fun setFetchResource(fetchResource: IFetchResource?) {
        player.pluginManager.getMixAnimPlugin()?.resourceRequest = fetchResource
    }

    override fun setOnResourceClickListener(resourceClickListener: OnResourceClickListener?) {
        player.pluginManager.getMixAnimPlugin()?.resourceClickListener = resourceClickListener
    }

    /**
     * 兼容方案，优先保证表情显示
     */
    open fun enableAutoTxtColorFill(enable: Boolean) {
        player.pluginManager.getMixAnimPlugin()?.autoTxtColorFill = enable
    }

    override fun setLoop(playLoop: Int) {
        player.playLoop = playLoop
    }

    override fun supportMask(isSupport: Boolean, isEdgeBlur: Boolean) {
        player.supportMaskBoolean = isSupport
        player.maskEdgeBlurBoolean = isEdgeBlur
    }

    override fun updateMaskConfig(maskConfig: MaskConfig?) {
        player.updateMaskConfig(maskConfig)
    }


    @Deprecated("Compatible older version mp4, default false")
    fun enableVersion1(enable: Boolean) {
        player.enableVersion1 = enable
    }

    // 兼容老版本视频模式
    @Deprecated("Compatible older version mp4")
    fun setVideoMode(mode: Int) {
        player.videoMode = mode
    }

    override fun setFps(fps: Int) {
        ALog.i(TAG, "setFps=$fps")
        player.defaultFps = fps
    }

    override fun setScaleType(type: ScaleType) {
        scaleTypeUtil.currentScaleType = type
    }

    override fun setScaleType(scaleType: IScaleType) {
        scaleTypeUtil.scaleTypeImpl = scaleType
    }

    /**
     * @param isMute true 静音
     */
    override fun setMute(isMute: Boolean) {
        ALog.e(TAG, "set mute=$isMute")
        player.isMute = isMute
    }

    override fun startPlay(file: File) {
        try {
            val fileContainer = FileContainer(file)
            startPlay(fileContainer)
        } catch (e: Throwable) {
            animProxyListener.onFailed(
                Constant.REPORT_ERROR_TYPE_FILE_ERROR,
                Constant.ERROR_MSG_FILE_ERROR
            )
            animProxyListener.onVideoComplete()
        }
    }

    override fun startPlay(assetManager: AssetManager, assetsPath: String) {
        try {
            val fileContainer = AssetsFileContainer(assetManager, assetsPath)
            startPlay(fileContainer)
        } catch (e: Throwable) {
            animProxyListener.onFailed(
                Constant.REPORT_ERROR_TYPE_FILE_ERROR,
                Constant.ERROR_MSG_FILE_ERROR
            )
            animProxyListener.onVideoComplete()
        }
    }


    override fun startPlay(fileContainer: IFileContainer) {
        ui {
            if (visibility != VISIBLE) {
                ALog.e(TAG, "AnimView is GONE, can't play")
                return@ui
            }
            if (!player.isRunning()) {
                lastFile = fileContainer
                player.startPlay(fileContainer)
            } else {
                ALog.e(TAG, "is running can not start")
            }
        }
    }


    override fun resumePlay() {
        ui {
            if (visibility != VISIBLE) {
                ALog.e(TAG, "AnimView is GONE, can't play")
                return@ui
            }
            if (!player.isRunning()) {
                lastFile?.also {
                    player.startPlay(it)
                } ?: ALog.e(TAG, "LastFile is null, can't resumePlay")
            } else {
                ALog.e(TAG, "is running can not start")
            }
        }
    }

    override fun stopPlay() {
        player.stopPlay()
    }

    override fun isRunning(): Boolean {
        return player.isRunning()
    }

    override fun getRealSize(): Pair<Int, Int> {
        return scaleTypeUtil.getRealSize()
    }

    private fun hide() {
        lastFile?.close()
        ui {
            removeAllViews()
        }
    }

    private fun ui(f: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) f() else uiHandler.post { f() }
    }

}