package com.nibm.autocare

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    // Suppress Switch deprecation — we target API 24+ and SwitchMaterial has styling issues
    // with the current theme; plain Switch is intentional here.
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        setupTheme()
        setupNotifications()
        setupVehicle()
        setupDisplay()
    }

    // ── Appearance ────────────────────────────────────────────────────────────

    private fun setupTheme() {
        val rgTheme = findViewById<RadioGroup>(R.id.rgTheme)
        val rbLight = findViewById<RadioButton>(R.id.rbLight)
        val rbDark = findViewById<RadioButton>(R.id.rbDark)
        val rbSystem = findViewById<RadioButton>(R.id.rbSystem)

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
            recreate()
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun setupNotifications() {
        val swService = findViewById<Switch>(R.id.switchServiceNotif)
        val swDoc = findViewById<Switch>(R.id.switchDocNotif)
        val swWarranty = findViewById<Switch>(R.id.switchWarrantyNotif)

        swService.isChecked = SettingsManager.isServiceNotifEnabled(this)
        swDoc.isChecked = SettingsManager.isDocNotifEnabled(this)
        swWarranty.isChecked = SettingsManager.isWarrantyNotifEnabled(this)

        swService.setOnCheckedChangeListener { _, on -> SettingsManager.setServiceNotif(this, on) }
        swDoc.setOnCheckedChangeListener { _, on -> SettingsManager.setDocNotif(this, on) }
        swWarranty.setOnCheckedChangeListener { _, on -> SettingsManager.setWarrantyNotif(this, on) }

        // W4 — Alert lead time
        val dayOptions = listOf(7, 14, 30, 60)
        val dayLabels = dayOptions.map { "$it days" }
        val spinnerDays = findViewById<Spinner>(R.id.spinnerNotifDays)
        spinnerDays.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dayLabels)
        val savedDays = SettingsManager.getNotifDaysBefore(this)
        spinnerDays.setSelection(dayOptions.indexOf(savedDays).coerceAtLeast(0))
        spinnerDays.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                SettingsManager.setNotifDaysBefore(this@SettingsActivity, dayOptions[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ── Vehicle ───────────────────────────────────────────────────────────────

    private fun setupVehicle() {
        // W3 — Service interval
        val intervalOptions = listOf(3000, 5000, 8000, 10000)
        val intervalLabels = intervalOptions.map { "${it / 1000}k km" }
        val spinnerInterval = findViewById<Spinner>(R.id.spinnerServiceInterval)
        spinnerInterval.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intervalLabels)
        val savedInterval = SettingsManager.getServiceInterval(this)
        spinnerInterval.setSelection(intervalOptions.indexOf(savedInterval).coerceAtLeast(0))
        spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                SettingsManager.setServiceInterval(this@SettingsActivity, intervalOptions[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // S3 — Default vehicle (populated from Firebase)
        val spinnerVehicle = findViewById<Spinner>(R.id.spinnerDefaultVehicle)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("users_vehicles").child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val regs = snapshot.children
                    .mapNotNull { it.child("registrationNumber").getValue(String::class.java) }
                    .toMutableList()
                if (regs.isEmpty()) return@addOnSuccessListener

                regs.add(0, "None")
                spinnerVehicle.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regs)
                val saved = SettingsManager.getDefaultVehicle(this)
                val idx = if (saved.isEmpty()) 0 else regs.indexOf(saved).coerceAtLeast(0)
                spinnerVehicle.setSelection(idx)
                spinnerVehicle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        SettingsManager.setDefaultVehicle(this@SettingsActivity, if (pos == 0) "" else regs[pos])
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
            }
    }

    // ── Display ───────────────────────────────────────────────────────────────

    private fun setupDisplay() {
        val currencies = listOf("MYR", "USD", "SGD", "GBP", "EUR", "AUD", "JPY")
        val spinnerCurrency = findViewById<Spinner>(R.id.spinnerCurrency)
        spinnerCurrency.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies)
        val saved = SettingsManager.getCurrency(this)
        spinnerCurrency.setSelection(currencies.indexOf(saved).coerceAtLeast(0))
        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                SettingsManager.setCurrency(this@SettingsActivity, currencies[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }
}
