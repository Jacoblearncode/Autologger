package com.nibm.autocare.SplashScreen

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nibm.autocare.Authentication.LoginActivity
import com.nibm.autocare.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvTitle = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)

        val titleFadeIn = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f).apply { duration = 700 }
        val titleScaleX = ObjectAnimator.ofFloat(tvTitle, "scaleX", 0.82f, 1f).apply { duration = 700 }
        val titleScaleY = ObjectAnimator.ofFloat(tvTitle, "scaleY", 0.82f, 1f).apply { duration = 700 }
        val taglineFade = ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 500
        }

        AnimatorSet().apply {
            playTogether(titleFadeIn, titleScaleX, titleScaleY, taglineFade)
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2500)
    }
}
