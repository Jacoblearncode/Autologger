package com.nibm.autocare

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.File

class SettingsActivity : AppCompatActivity() {

    // Suppress Switch deprecation — we target API 24+ and SwitchMaterial has styling issues
    // with the current theme; plain Switch is intentional here.
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        setupAccount()
        setupTheme()
        setupSecurity()
        setupNotifications()
        setupVehicle()
        setupDisplay()
    }

    // ── Account / profile photo (C1) ──────────────────────────────────────────

    private var cameraImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> uploadProfilePhoto(uri) }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uploadProfilePhoto(it) }
        }
    }

    private fun setupAccount() {
        val iv = findViewById<ImageView>(R.id.ivProfilePhoto)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvChange = findViewById<TextView>(R.id.tvChangePhoto)

        val user = FirebaseAuth.getInstance().currentUser
        tvEmail.text = user?.email ?: "Signed in"

        val uid = user?.uid
        if (uid != null) {
            FirebaseDatabase.getInstance().reference
                .child("users").child(uid).child("profilePhotoUrl")
                .get().addOnSuccessListener { snap ->
                    val url = snap.getValue(String::class.java)
                    if (!url.isNullOrEmpty()) {
                        Glide.with(this).load(url).circleCrop()
                            .placeholder(R.drawable.circle_gray_bg).into(iv)
                    }
                }
        }

        val pick = View.OnClickListener { showPhotoSourceDialog() }
        iv.setOnClickListener(pick)
        tvChange.setOnClickListener(pick)
    }

    private fun showPhotoSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Profile photo")
            .setItems(arrayOf("Take photo", "Choose from gallery")) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> launchGallery()
                }
            }
            .show()
    }

    private fun launchCamera() {
        val imageFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "profile_${System.currentTimeMillis()}.jpg"
        )
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        galleryLauncher.launch(Intent.createChooser(intent, "Select photo"))
    }

    private fun uploadProfilePhoto(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val bytes = compressImage(uri) ?: run {
            Toast.makeText(this, "Could not read image", Toast.LENGTH_SHORT).show(); return
        }
        Toast.makeText(this, "Uploading photo…", Toast.LENGTH_SHORT).show()

        MediaManager.get().upload(bytes)
            .option("folder", "Home/AutoCare/profiles")
            .option("public_id", "profile_$uid")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, total: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["url"]?.toString() ?: return
                    FirebaseDatabase.getInstance().reference
                        .child("users").child(uid).child("profilePhotoUrl").setValue(url)
                    runOnUiThread {
                        Glide.with(this@SettingsActivity).load(url).circleCrop()
                            .into(findViewById<ImageView>(R.id.ivProfilePhoto))
                        Toast.makeText(this@SettingsActivity, "Profile photo updated", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Profile upload failed: ${error.description}")
                    runOnUiThread { Toast.makeText(this@SettingsActivity, "Upload failed", Toast.LENGTH_SHORT).show() }
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    private fun compressImage(uri: Uri): ByteArray? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            opts.inSampleSize = run {
                var sample = 1
                val h = opts.outHeight
                val w = opts.outWidth
                while (h / sample > 512 || w / sample > 512) sample *= 2
                sample
            }
            opts.inJustDecodeBounds = false
            val bitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    // ── Security (A1) ─────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun setupSecurity() {
        val sw = findViewById<Switch>(R.id.switchBiometric)
        val status = findViewById<android.widget.TextView>(R.id.tvBiometricStatus)
        val available = androidx.biometric.BiometricManager.from(this).canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

        if (!available) {
            sw.isEnabled = false
            sw.isChecked = false
            status.text = "No fingerprint enrolled on this device"
            return
        }

        sw.isChecked = SettingsManager.isBiometricEnabled(this)
        sw.setOnCheckedChangeListener { _, on -> SettingsManager.setBiometric(this, on) }
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

        val etBudget = findViewById<EditText>(R.id.etMonthlyBudget)
        val savedBudget = SettingsManager.getMonthlyBudget(this)
        if (savedBudget > 0) etBudget.setText("%.0f".format(savedBudget))
        etBudget.setOnEditorActionListener { _, _, _ ->
            val v = etBudget.text.toString().toDoubleOrNull() ?: 0.0
            SettingsManager.setMonthlyBudget(this, v)
            false
        }
        etBudget.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = etBudget.text.toString().toDoubleOrNull() ?: 0.0
                SettingsManager.setMonthlyBudget(this, v)
            }
        }
    }
}
