package com.nibm.autocare

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val rgTheme = findViewById<RadioGroup>(R.id.rgTheme)
        val rbLight = findViewById<RadioButton>(R.id.rbLight)
        val rbDark = findViewById<RadioButton>(R.id.rbDark)
        val rbSystem = findViewById<RadioButton>(R.id.rbSystem)

        // Reflect current saved mode
        when (ThemeManager.getMode(this)) {
            ThemeManager.MODE_DARK -> rbDark.isChecked = true
            ThemeManager.MODE_SYSTEM -> rbSystem.isChecked = true
            else -> rbLight.isChecked = true
        }

        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbDark -> ThemeManager.MODE_DARK
                R.id.rbSystem -> ThemeManager.MODE_SYSTEM
                else -> ThemeManager.MODE_LIGHT
            }
            ThemeManager.setMode(this, mode)
            // Recreate so the current screen immediately reflects the change
            recreate()
        }
    }
}
