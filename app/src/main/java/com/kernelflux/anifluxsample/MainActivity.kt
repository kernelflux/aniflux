package com.kernelflux.anifluxsample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kernelflux.aniflux.AntiFlux
import org.libpag.PAGImageView


class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val pagImageView = findViewById<PAGImageView>(R.id.pag_view)
        val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"
        AntiFlux.with(this)

    }


}