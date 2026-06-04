package com.nibm.autocare.Vehicle

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.AddServiceActivity
import com.nibm.autocare.HomeActivity
import com.nibm.autocare.R
import com.nibm.autocare.Reminder.ReminderScheduler
import java.io.ByteArrayOutputStream

class AddVehicleActivity : AppCompatActivity() {

    private lateinit var ivVehiclePhoto: ImageView
    private lateinit var etRegistrationNumber: EditText
    private lateinit var spinnerBrand: Spinner
    private lateinit var spinnerModel: Spinner
    private lateinit var etManufacturedYear: EditText
    private lateinit var etCurrentMileage: EditText
    private lateinit var etWeeklyRidingDistance: EditText
    private lateinit var btnSaveVehicle: Button

    private val database = FirebaseDatabase.getInstance()
    private val brandsRef = database.reference.child("vehicles").child("brands")
    private val auth = FirebaseAuth.getInstance()

    private val brandList = mutableListOf<String>()
    private val modelList = mutableListOf<String>()

    private var selectedBrand: String = ""
    private var selectedModel: String = ""
    private var isEditMode = false
    private var vehicleId: String? = null
    private var originalRegistrationNumber: String? = null
    private var selectedPhotoUri: Uri? = null
    private var existingPhotoUrl: String? = null
    private var cameraImageUri: Uri? = null

    companion object {
        private const val CAMERA_PERMISSION_CODE = 2001
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedPhotoUri = uri
                Glide.with(this).load(uri).circleCrop().into(ivVehiclePhoto)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            cameraImageUri?.let { uri ->
                selectedPhotoUri = uri
                Glide.with(this).load(uri).circleCrop().into(ivVehiclePhoto)
                cameraImageUri = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vehicle)

        initViews()
        checkEditMode()
        loadBrands()

        spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBrand = brandList[position]
                loadModels(selectedBrand)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedModel = modelList[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        ivVehiclePhoto.setOnClickListener { showPhotoSourceDialog() }

        btnSaveVehicle.setOnClickListener {
            if (isEditMode) updateVehicle() else saveVehicle()
        }

        setupFooterNavigation()
    }

    private fun initViews() {
        ivVehiclePhoto = findViewById(R.id.ivVehiclePhoto)
        etRegistrationNumber = findViewById(R.id.etRegistrationNumber)
        spinnerBrand = findViewById(R.id.spinnerBrand)
        spinnerModel = findViewById(R.id.spinnerModel)
        etManufacturedYear = findViewById(R.id.etManufacturedYear)
        etCurrentMileage = findViewById(R.id.etCurrentMileage)
        etWeeklyRidingDistance = findViewById(R.id.etWeeklyRidingDistance)
        btnSaveVehicle = findViewById(R.id.btnSaveVehicle)
    }

    private fun checkEditMode() {
        isEditMode = intent.hasExtra("vehicleId")
        if (isEditMode) {
            findViewById<TextView>(R.id.tvAppName).text = "Update Vehicle"
            btnSaveVehicle.text = "Update Vehicle"
            vehicleId = intent.getStringExtra("vehicleId")
            originalRegistrationNumber = intent.getStringExtra("registrationNumber")
            loadVehicleDetails()
        }
    }

    private fun showPhotoSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Photo")
            .setItems(arrayOf("Choose from Gallery", "Take Photo")) { _, which ->
                if (which == 0) openGallery() else checkCameraPermissionAndOpen()
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Vehicle Photo")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        cameraImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            cameraLauncher.launch(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }

    private fun loadVehicleDetails() {
        val currentUser = auth.currentUser ?: return
        val vehicleRef = database.reference.child("users_vehicles").child(currentUser.uid).child(vehicleId!!)

        vehicleRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                etRegistrationNumber.setText(snapshot.child("registrationNumber").getValue(String::class.java))
                etManufacturedYear.setText(snapshot.child("manufacturedYear").getValue(String::class.java))
                snapshot.child("currentMileage").getValue(Int::class.java)?.let { etCurrentMileage.setText(it.toString()) }
                snapshot.child("weeklyRidingDistance").getValue(Int::class.java)?.let { etWeeklyRidingDistance.setText(it.toString()) }
                selectedBrand = snapshot.child("brand").getValue(String::class.java) ?: ""
                selectedModel = snapshot.child("model").getValue(String::class.java) ?: ""

                existingPhotoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                existingPhotoUrl?.let { url ->
                    Glide.with(this@AddVehicleActivity)
                        .load(url)
                        .circleCrop()
                        .placeholder(R.drawable.circle_gray_bg)
                        .into(ivVehiclePhoto)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddVehicleActivity, "Failed to load vehicle details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupFooterNavigation() {
        findViewById<View>(R.id.llHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<View>(R.id.llAddService).setOnClickListener {
            startActivity(Intent(this, AddServiceActivity::class.java))
        }
    }

    private fun loadBrands() {
        brandsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                brandList.clear()
                for (brandSnapshot in snapshot.children) {
                    val brand = brandSnapshot.key ?: continue
                    brandList.add(brand)
                }
                val brandAdapter = ArrayAdapter(this@AddVehicleActivity, android.R.layout.simple_spinner_item, brandList)
                brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerBrand.adapter = brandAdapter

                if (isEditMode && selectedBrand.isNotEmpty()) {
                    val pos = brandList.indexOf(selectedBrand)
                    if (pos != -1) spinnerBrand.setSelection(pos)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddVehicleActivity, "Failed to load brands", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadModels(selectedBrand: String) {
        brandsRef.child(selectedBrand).child("models").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                modelList.clear()
                for (modelSnapshot in snapshot.children) {
                    val model = modelSnapshot.getValue(String::class.java) ?: continue
                    modelList.add(model)
                }
                val modelAdapter = ArrayAdapter(this@AddVehicleActivity, android.R.layout.simple_spinner_item, modelList)
                modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerModel.adapter = modelAdapter

                if (isEditMode && selectedModel.isNotEmpty()) {
                    val pos = modelList.indexOf(selectedModel)
                    if (pos != -1) spinnerModel.setSelection(pos)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddVehicleActivity, "Failed to load models", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveVehicle() {
        val registrationNumber = etRegistrationNumber.text.toString().trim()
        val manufacturedYear = etManufacturedYear.text.toString().trim()
        val currentMileage = etCurrentMileage.text.toString().trim()
        val weeklyRidingDistance = etWeeklyRidingDistance.text.toString().trim()

        if (!validateInputs(registrationNumber, manufacturedYear, currentMileage, weeklyRidingDistance)) return

        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPhotoUri != null) {
            val progress = showProgressDialog("Uploading photo...")
            uploadVehiclePhoto(selectedPhotoUri!!) { photoUrl ->
                progress.dismiss()
                persistNewVehicle(currentUser.uid, registrationNumber, manufacturedYear, currentMileage, weeklyRidingDistance, photoUrl)
            }
        } else {
            persistNewVehicle(currentUser.uid, registrationNumber, manufacturedYear, currentMileage, weeklyRidingDistance, null)
        }
    }

    private fun persistNewVehicle(
        userId: String, registrationNumber: String, manufacturedYear: String,
        currentMileage: String, weeklyRidingDistance: String, photoUrl: String?
    ) {
        val vehicle = HashMap<String, Any>()
        vehicle["registrationNumber"] = registrationNumber
        vehicle["brand"] = selectedBrand
        vehicle["model"] = selectedModel
        vehicle["manufacturedYear"] = manufacturedYear
        vehicle["currentMileage"] = currentMileage.toInt()
        vehicle["weeklyRidingDistance"] = weeklyRidingDistance.toInt()
        photoUrl?.let { vehicle["photoUrl"] = it }

        val usersVehiclesRef = database.reference.child("users_vehicles").child(userId)
        val newVehicleId = usersVehiclesRef.push().key ?: return

        usersVehiclesRef.child(newVehicleId).setValue(vehicle)
            .addOnSuccessListener {
                ReminderScheduler.scheduleForVehicle(this, registrationNumber, currentMileage.toInt(), weeklyRidingDistance.toInt())
                Toast.makeText(this, "Vehicle saved successfully", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save vehicle", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateVehicle() {
        val registrationNumber = etRegistrationNumber.text.toString().trim()
        val manufacturedYear = etManufacturedYear.text.toString().trim()
        val currentMileage = etCurrentMileage.text.toString().trim()
        val weeklyRidingDistance = etWeeklyRidingDistance.text.toString().trim()

        if (!validateInputs(registrationNumber, manufacturedYear, currentMileage, weeklyRidingDistance)) return

        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPhotoUri != null) {
            val progress = showProgressDialog("Uploading photo...")
            uploadVehiclePhoto(selectedPhotoUri!!) { photoUrl ->
                progress.dismiss()
                persistVehicleUpdate(currentUser.uid, registrationNumber, manufacturedYear, currentMileage, weeklyRidingDistance, photoUrl ?: existingPhotoUrl)
            }
        } else {
            persistVehicleUpdate(currentUser.uid, registrationNumber, manufacturedYear, currentMileage, weeklyRidingDistance, existingPhotoUrl)
        }
    }

    private fun persistVehicleUpdate(
        userId: String, registrationNumber: String, manufacturedYear: String,
        currentMileage: String, weeklyRidingDistance: String, photoUrl: String?
    ) {
        val updates = HashMap<String, Any>()
        updates["registrationNumber"] = registrationNumber
        updates["brand"] = selectedBrand
        updates["model"] = selectedModel
        updates["manufacturedYear"] = manufacturedYear
        updates["currentMileage"] = currentMileage.toInt()
        updates["weeklyRidingDistance"] = weeklyRidingDistance.toInt()
        photoUrl?.let { updates["photoUrl"] = it }

        database.reference.child("users_vehicles").child(userId).child(vehicleId!!)
            .updateChildren(updates)
            .addOnSuccessListener {
                if (originalRegistrationNumber != null && originalRegistrationNumber != registrationNumber) {
                    updateServicesRegistrationNumber(userId, originalRegistrationNumber!!, registrationNumber)
                } else {
                    Toast.makeText(this, "Vehicle updated successfully", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update vehicle", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadVehiclePhoto(uri: Uri, onComplete: (String?) -> Unit) {
        val imageBytes = compressImage(uri) ?: run {
            onComplete(null)
            return
        }
        val publicId = "vehicle_${auth.currentUser?.uid}_${System.currentTimeMillis()}"
        MediaManager.get().upload(imageBytes)
            .option("folder", "Home/AutoCare/vehicles")
            .option("public_id", publicId)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    onComplete(resultData["url"]?.toString())
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Vehicle photo upload failed: ${error.description}")
                    runOnUiThread { Toast.makeText(this@AddVehicleActivity, "Photo upload failed, saving without photo", Toast.LENGTH_SHORT).show() }
                    onComplete(null)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    private fun showProgressDialog(message: String): AlertDialog {
        return AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
            options.inJustDecodeBounds = false
            val bitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.e("Compress", "Failed to compress image: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun updateServicesRegistrationNumber(userId: String, oldRegNumber: String, newRegNumber: String) {
        val servicesRef = database.reference.child("users_services").child(userId)
        servicesRef.child(oldRegNumber).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    servicesRef.child(newRegNumber).setValue(snapshot.value)
                        .addOnSuccessListener {
                            servicesRef.child(oldRegNumber).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this@AddVehicleActivity, "Vehicle and services updated successfully", Toast.LENGTH_SHORT).show()
                                    navigateToHome()
                                }
                        }
                } else {
                    Toast.makeText(this@AddVehicleActivity, "Vehicle updated successfully", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddVehicleActivity, "Vehicle updated but services may not be updated", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
        })
    }

    private fun validateInputs(
        registrationNumber: String, manufacturedYear: String,
        currentMileage: String, weeklyRidingDistance: String
    ): Boolean {
        if (registrationNumber.isEmpty() || selectedBrand.isEmpty() || selectedModel.isEmpty() ||
            manufacturedYear.isEmpty() || currentMileage.isEmpty() || weeklyRidingDistance.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        if (currentMileage.toIntOrNull() == null) {
            Toast.makeText(this, "Please enter valid current mileage", Toast.LENGTH_SHORT).show()
            return false
        }
        if (weeklyRidingDistance.toIntOrNull() == null) {
            Toast.makeText(this, "Please enter valid weekly riding distance", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
