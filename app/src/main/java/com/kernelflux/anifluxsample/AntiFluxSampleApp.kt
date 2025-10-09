package com.kernelflux.anifluxsample

import android.app.Application
import android.content.Context
import com.kernelflux.aniflux.AntiFlux

class AntiFluxSampleApp : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        AntiFlux.init(this)


    }


}