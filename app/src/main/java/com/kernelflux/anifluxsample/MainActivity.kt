package com.kernelflux.anifluxsample

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.into
import com.kernelflux.aniflux.load.AnimationDataSource
import com.kernelflux.aniflux.request.AnimationRequestListener
import com.kernelflux.aniflux.request.listener.AnimationPlayListenerAdapter
import com.kernelflux.aniflux.request.target.AnimationTarget
import com.kernelflux.aniflux.util.CacheStrategy
import com.opensource.svgaplayer.SVGAImageView
import org.libpag.PAGFile
import org.libpag.PAGImageView
import pl.droidsonroids.gif.GifImageView


class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pagImageView = findViewById<PAGImageView>(R.id.pag_view)
        findViewById<Button>(R.id.btn_load_pag).setOnClickListener {
            val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
            AniFlux.with(this)
                .asPAG()
                .load(pagUrl)
                .cacheStrategy(CacheStrategy.ALL)
                .repeatCount(2)
                .requestListener(object : AnimationRequestListener<PAGFile> {
                    override fun onLoadFailed(
                        exception: Throwable,
                        model: Any?,
                        target: AnimationTarget<PAGFile>,
                        isFirstResource: Boolean
                    ): Boolean {
                        AniFluxLogger.i("onLoadFailed...")
                        return false
                    }

                    override fun onResourceReady(
                        resource: PAGFile,
                        model: Any?,
                        target: AnimationTarget<PAGFile>,
                        dataSource: AnimationDataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        AniFluxLogger.i("onResourceReady:$resource")
                        return false
                    }
                })
                .playListener(object : AnimationPlayListenerAdapter() {
                    override fun onAnimationStart() {
                        AniFluxLogger.i("PAG动画开始播放")
                    }

                    override fun onAnimationEnd() {
                        AniFluxLogger.i("PAG动画播放结束")
                    }

                    override fun onAnimationCancel() {
                        AniFluxLogger.i("PAG动画播放取消")
                    }

                    override fun onAnimationRepeat() {
                        AniFluxLogger.i("PAG动画重复播放")
                    }

                    override fun onAnimationFailed(error: Throwable?) {
                        AniFluxLogger.i("PAG动画播放失败: ${error?.message}")
                    }
                }).into(pagImageView)
        }

        val svgaImageView = findViewById<SVGAImageView>(R.id.svga_view)
        val svgaUrl =
            "https://peanut-oss.wemogu.net/client/animation/common/live_room_seat_boy.svga"
        AniFlux.with(this)
            .load(svgaUrl)
            .playListener(object : AnimationPlayListenerAdapter() {
                override fun onAnimationStart() {
                    AniFluxLogger.i("SVGA动画开始播放")
                }

                override fun onAnimationEnd() {
                    AniFluxLogger.i("SVGA动画播放结束")
                }

                override fun onAnimationRepeat() {
                    AniFluxLogger.i("SVGA动画重复播放")
                }
            })
            .into(svgaImageView)

        val gifImageView = findViewById<GifImageView>(R.id.gif_view)
        val gifUrl = "http://imgcom.static.suishenyun.net/c6a3e39be73229d8a2ca2be5662b5a49.gif"

        AniFlux.with(this)
            .load(gifUrl)
            .playListener(object : AnimationPlayListenerAdapter() {
                override fun onAnimationStart() {
                    AniFluxLogger.i("GIF动画开始播放")
                }

                override fun onAnimationEnd() {
                    AniFluxLogger.i("GIF动画播放结束")
                }

                override fun onAnimationRepeat() {
                    AniFluxLogger.i("GIF动画重复播放")
                }
            })
            .into(gifImageView)


        val lottieView = findViewById<LottieAnimationView>(R.id.lottie_view)
        val jsonUrl = "http://peanut-oss.wemogu.net/f21b87ca2153473bb4332323fd5d1880.json"
        AniFlux.with(this)
            .load(jsonUrl)
            .playListener(object : AnimationPlayListenerAdapter() {
                override fun onAnimationStart() {
                    AniFluxLogger.i("Lottie动画开始播放")
                }

                override fun onAnimationEnd() {
                    AniFluxLogger.i("Lottie动画播放结束")
                }

                override fun onAnimationCancel() {
                    AniFluxLogger.i("Lottie动画播放取消")
                }

                override fun onAnimationRepeat() {
                    AniFluxLogger.i("Lottie动画重复播放")
                }
            }
            ).into(lottieView)


    }


}