package com.kernelflux.svga

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.kernelflux.svga.drawer.SVGACanvasDrawer
import kotlin.collections.forEach

class SVGADrawable(val videoItem: SVGAVideoEntity, val dynamicItem: SVGADynamicEntity): Drawable() {

    constructor(videoItem: SVGAVideoEntity): this(videoItem, SVGADynamicEntity())

    var cleared = true
        internal set (value) {
            if (field == value) {
                return
            }
            field = value
            invalidateSelf()
        }

    var currentFrame = 0
        internal set (value) {
            if (field == value) {
                return
            }
            field = value
            invalidateSelf()
        }

    var scaleType: ImageView.ScaleType = ImageView.ScaleType.MATRIX
    
    // 维护所有正在播放的音频流 ID，防止 playID 丢失导致无法停止
    private val activePlayIds = mutableSetOf<Int>()

    private val drawer = SVGACanvasDrawer(videoItem, dynamicItem, this)

    override fun draw(canvas: Canvas) {
        if (cleared) {
            return
        }
        canvas?.let {
            drawer.drawFrame(it,currentFrame, scaleType)
        }
    }

    override fun setAlpha(alpha: Int) {

    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {

    }

    fun resume() {
        videoItem.isPlaying = true
        videoItem.audioList.forEach { audio ->
            audio.playID?.let {
                if (SVGASoundManager.isInit()){
                    SVGASoundManager.resume(it)
                }else{
                    videoItem.soundPool?.resume(it)
                }
            }
        }
    }

    fun pause() {
        videoItem.isPlaying = false
        videoItem.audioList.forEach { audio ->
            audio.playID?.let {
                if (SVGASoundManager.isInit()){
                    SVGASoundManager.pause(it)
                }else{
                    videoItem.soundPool?.pause(it)
                }
            }
        }
    }

    fun stop() {
        videoItem.isPlaying = false
        // 停止所有记录的 playID（防止 playID 丢失导致无法停止）
        activePlayIds.forEach { playId ->
            if (SVGASoundManager.isInit()){
                SVGASoundManager.stop(playId)
            }else{
                videoItem.soundPool?.stop(playId)
            }
        }
        activePlayIds.clear()
        
        // 也停止 audioList 中的
        videoItem.audioList.forEach { audio ->
            audio.playID?.let { playId ->
                if (SVGASoundManager.isInit()){
                    SVGASoundManager.stop(playId)
                }else{
                    videoItem.soundPool?.stop(playId)
                }
                activePlayIds.remove(playId)
            }
            audio.playID = null
        }
    }

    fun clear() {
        videoItem.isPlaying = false
        // 停止所有音频
        stop()
        videoItem.clear()
    }
    
    /**
     * 记录音频播放 ID，用于后续停止
     */
    internal fun recordPlayId(playId: Int) {
        activePlayIds.add(playId)
    }
    
    /**
     * 移除音频播放 ID
     */
    internal fun removePlayId(playId: Int) {
        activePlayIds.remove(playId)
    }
}