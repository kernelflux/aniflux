package com.kernelflux.anifluxsample

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.request.target.CustomAnimationTarget
import com.kernelflux.aniflux.util.CacheStrategy
import org.libpag.PAGFile
import org.libpag.PAGImageView

class PAGAnimationTarget(private val pagImageView: PAGImageView) :
    CustomAnimationTarget<PAGFile>() {

    override fun onLoadStarted(placeholder: Drawable?) {
        AniFluxLogger.i("onLoadStarted")
        // 可以设置占位符
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        AniFluxLogger.i("onLoadFailed: $errorDrawable")
        // 处理加载失败
    }

    override fun onResourceReady(resource: PAGFile) {
        AniFluxLogger.i("onResourceReady: $resource")
        // 设置PAG文件到ImageView
        pagImageView.apply {
            setRepeatCount(-1)
            composition = resource
            play()
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        AniFluxLogger.i("onLoadCleared")
        // 清理资源
    }
}


class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val pagImageView = findViewById<PAGImageView>(R.id.pag_view)
        val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        val target = PAGAnimationTarget(pagImageView)
        AniFlux.with(this).load(pagUrl).cacheStrategy(CacheStrategy.ALL).into(target)

    }


}