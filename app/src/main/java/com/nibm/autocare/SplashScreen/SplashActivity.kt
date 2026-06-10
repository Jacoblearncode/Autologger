package com.nibm.autocare.SplashScreen

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.nibm.autocare.Authentication.LoginActivity
import com.nibm.autocare.HomeActivity
import com.nibm.autocare.R
import com.nibm.autocare.SettingsManager

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

        Handler(Looper.getMainLooper()).postDelayed({ route() }, 2500)
    }

    private fun route() {
        // U7 — first launch shows onboarding before anything else
        if (!SettingsManager.isOnboardingSeen(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        when {
            // A1 — logged in + biometric enabled: require fingerprint before Home
            user != null && SettingsManager.isBiometricEnabled(this) && canUseBiometric() ->
                promptBiometric()
            // Logged in, no biometric: go straight Home
            user != null -> goHome()
            // Not logged in: Login screen
            else -> goLogin()
        }
    }

    private fun canUseBiometric(): Boolean {
        return BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun promptBiometric() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                goHome()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // User cancelled or too many attempts — fall back to the login screen
                goLogin()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock AutoCare")
            .setSubtitle("Confirm your identity to continue")
            .setNegativeButtonText("Use password")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(info)
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun goLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
