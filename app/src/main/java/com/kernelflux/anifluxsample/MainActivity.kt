package com.kernelflux.anifluxsample

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieDrawable
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.util.CacheStrategy
import com.opensource.svgaplayer.SVGADrawable
import com.opensource.svgaplayer.SVGAImageView
import org.libpag.PAGFile
import org.libpag.PAGImageView
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView


class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val pagImageView = findViewById<PAGImageView>(R.id.pag_view)
        val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        AniFlux.with(this).load(pagUrl).cacheStrategy(CacheStrategy.ALL).into(
            object : CustomAnimationTarget<PAGFile>() {

                override fun onLoadStarted(placeholder: Drawable?) {
                    AniFluxLogger.i("PAGImageView onLoadStarted")
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    AniFluxLogger.i("PAGImageView onLoadFailed: $errorDrawable")
                }

                override fun onResourceReady(resource: PAGFile) {
                    AniFluxLogger.i("PAGImageView onResourceReady: $resource")
                    pagImageView.apply {
                        setRepeatCount(-1)
                        composition = resource
                        play()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    AniFluxLogger.i("PAGImageView onLoadCleared")
                }
            }
        )


        val svgaImageView = findViewById<SVGAImageView>(R.id.svga_view)
        val svgaUrl =
            "https://peanut-oss.wemogu.net/client/animation/common/live_room_seat_boy.svga"
        AniFlux.with(this).load(svgaUrl).into(
            object : CustomAnimationTarget<SVGADrawable>() {

                override fun onLoadStarted(placeholder: Drawable?) {
                    AniFluxLogger.i("SVGAImageView onLoadStarted")
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    AniFluxLogger.i("SVGAImageView onLoadFailed: $errorDrawable")
                }

                override fun onResourceReady(resource: SVGADrawable) {
                    AniFluxLogger.i("SVGAImageView onResourceReady: $resource")
                    svgaImageView.apply {
                        setVideoItem(videoItem = resource.videoItem)
                        startAnimation()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    AniFluxLogger.i("SVGAImageView onLoadCleared")
                }
            }
        )

        val gifImageView = findViewById<GifImageView>(R.id.gif_view)
        val gifUrl = "http://imgcom.static.suishenyun.net/c6a3e39be73229d8a2ca2be5662b5a49.gif"
        AniFlux.with(this).load(gifUrl).into(
            object : CustomAnimationTarget<GifDrawable>() {

                override fun onLoadStarted(placeholder: Drawable?) {
                    AniFluxLogger.i("GifImageView onLoadStarted")
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    AniFluxLogger.i("GifImageView onLoadFailed: $errorDrawable")
                }

                override fun onResourceReady(resource: GifDrawable) {
                    AniFluxLogger.i("GifImageView onResourceReady: $resource")
                    gifImageView.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    AniFluxLogger.i("GifImageView onLoadCleared")
                }
            }
        )


        val lottieView = findViewById<LottieAnimationView>(R.id.lottie_view)
        val jsonUrl = "http://peanut-oss.wemogu.net/f21b87ca2153473bb4332323fd5d1880.json"
        AniFlux.with(this).load(jsonUrl).into(
            object : CustomAnimationTarget<LottieDrawable>() {

                override fun onLoadStarted(placeholder: Drawable?) {
                    AniFluxLogger.i("LottieAnimationView onLoadStarted")
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    AniFluxLogger.i("LottieAnimationView onLoadFailed: $errorDrawable")
                }

                override fun onResourceReady(resource: LottieDrawable) {
                    AniFluxLogger.i("LottieAnimationView onResourceReady: $resource")
                    lottieView.apply {
                        repeatCount = LottieDrawable.INFINITE
                        setComposition(resource.composition)
                        playAnimation()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    AniFluxLogger.i("LottieAnimationView onLoadCleared")
                }
            }
        )


    }


}