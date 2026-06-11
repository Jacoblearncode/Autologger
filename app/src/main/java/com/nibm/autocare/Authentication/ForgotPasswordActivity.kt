package com.nibm.autocare.Authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.nibm.autocare.R

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var btnSendCode: Button
    private lateinit var tvBackToSignIn: TextView
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        etEmail       = findViewById(R.id.etEmail)
        btnSendCode   = findViewById(R.id.btnSendCode)
        tvBackToSignIn = findViewById(R.id.tvBackToSignIn)
        tvSignUp      = findViewById(R.id.tvSignUp)

        btnSendCode.text = "Send Reset Link"

        btnSendCode.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            } else {
                sendPasswordReset(email)
            }
        }

        tvBackToSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun sendPasswordReset(email: String) {
        btnSendCode.isEnabled = false
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Password reset link sent — check your inbox.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnSendCode.isEnabled = true
                Toast.makeText(this, "Could not send reset email: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
