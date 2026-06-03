package com.nibm.autocare

import android.app.Application
import com.cloudinary.android.MediaManager
import java.util.HashMap

class AutoCareCloudinary : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeCloudinary()
    }

    private fun initializeCloudinary() {
        val config = HashMap<String, String>()
        config["cloud_name"] = "dcc69stmc"
        config["api_key"] = "179157862627134"
        config["api_secret"] = "eFMpSW7Vh7U2561rUlATnQpjt5E"
        MediaManager.init(this, config)
    }
}