package com.kernelflux.anifluxsample

import android.app.Application
import android.content.Context
import com.kernelflux.aniflux.AniFlux

class AntiFluxSampleApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        // 初始化 AniFlux，并设置 Glide 占位图加载器
        AniFlux.init(this) {
            setPlaceholderImageLoader(GlidePlaceholderImageLoader())
        }
    }
}