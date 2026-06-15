package com.nibm.autocare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageButton

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }
}
