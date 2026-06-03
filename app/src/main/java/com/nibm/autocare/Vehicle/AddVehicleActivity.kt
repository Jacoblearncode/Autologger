package com.nibm.autocare.Vehicle

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import com.nibm.autocare.Reminder.ReminderScheduler
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.AddServiceActivity
import com.nibm.autocare.HomeActivity
import com.nibm.autocare.R

class AddVehicleActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vehicle)

        // Initialize views
        initViews()

        // Check if we're in edit mode
        checkEditMode()

        // Load brands into the brand spinner
        loadBrands()

        // Set up brand spinner item selection listener
        spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBrand = brandList[position]
                loadModels(selectedBrand)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set up model spinner item selection listener
        spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedModel = modelList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set up save/update vehicle button click listener
        btnSaveVehicle.setOnClickListener {
            if (isEditMode) {
                updateVehicle()
            } else {
                saveVehicle()
            }
        }

        // Set up footer navigation
        setupFooterNavigation()
    }

    private fun initViews() {
        etRegistrationNumber = findViewById(R.id.etRegistrationNumber)
        spinnerBrand = findViewById(R.id.spinnerBrand)
        spinnerModel = findViewById(R.id.spinnerModel)
        etManufacturedYear = findViewById(R.id.etManufacturedYear)
        etCurrentMileage = findViewById(R.id.etCurrentMileage)
        etWeeklyRidingDistance = findViewById(R.id.etWeeklyRidingDistance)
        btnSaveVehicle = findViewById(R.id.btnSaveVehicle)
    }

    private fun checkEditMode() {
        val intent = intent
        isEditMode = intent.hasExtra("vehicleId")

        if (isEditMode) {
            // Change UI for edit mode
            findViewById<TextView>(R.id.tvAppName).text = "Update Vehicle"
            btnSaveVehicle.text = "Update Vehicle"

            // Get vehicle details from intent
            vehicleId = intent.getStringExtra("vehicleId")
            originalRegistrationNumber = intent.getStringExtra("registrationNumber")

            // Load vehicle details
            loadVehicleDetails()
        }
    }

    private fun loadVehicleDetails() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid
        val vehicleRef = database.reference.child("users_vehicles").child(userId).child(vehicleId!!)

        vehicleRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val registrationNumber = snapshot.child("registrationNumber").getValue(String::class.java)
                    val brand = snapshot.child("brand").getValue(String::class.java)
                    val model = snapshot.child("model").getValue(String::class.java)
                    val manufacturedYear = snapshot.child("manufacturedYear").getValue(String::class.java)
                    val currentMileage = snapshot.child("currentMileage").getValue(Int::class.java)
                    val weeklyRidingDistance = snapshot.child("weeklyRidingDistance").getValue(Int::class.java)

                    // Set values to views
                    etRegistrationNumber.setText(registrationNumber)
                    etManufacturedYear.setText(manufacturedYear)
                    currentMileage?.let { etCurrentMileage.setText(it.toString()) }
                    weeklyRidingDistance?.let { etWeeklyRidingDistance.setText(it.toString()) }

                    // Set brand and model after they are loaded
                    selectedBrand = brand ?: ""
                    selectedModel = model ?: ""
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

                // After brands are loaded, select the vehicle's brand if in edit mode
                if (isEditMode && selectedBrand.isNotEmpty()) {
                    val brandPosition = brandList.indexOf(selectedBrand)
                    if (brandPosition != -1) {
                        spinnerBrand.setSelection(brandPosition)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddVehicleActivity, "Failed to load brands", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadModels(selectedBrand: String) {
        val modelsRef = brandsRef.child(selectedBrand).child("models")
        modelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                modelList.clear()
                for (modelSnapshot in snapshot.children) {
                    val model = modelSnapshot.getValue(String::class.java) ?: continue
                    modelList.add(model)
                }

                val modelAdapter = ArrayAdapter(this@AddVehicleActivity, android.R.layout.simple_spinner_item, modelList)
                modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerModel.adapter = modelAdapter

                // After models are loaded, select the vehicle's model if in edit mode
                if (isEditMode && selectedModel.isNotEmpty()) {
                    val modelPosition = modelList.indexOf(selectedModel)
                    if (modelPosition != -1) {
                        spinnerModel.setSelection(modelPosition)
                    }
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

        if (!validateInputs(registrationNumber, manufacturedYear, currentMileage, weeklyRidingDistance)) {
            return
        }

        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val vehicle = HashMap<String, Any>()
        vehicle["registrationNumber"] = registrationNumber
        vehicle["brand"] = selectedBrand
        vehicle["model"] = selectedModel
        vehicle["manufacturedYear"] = manufacturedYear
        vehicle["currentMileage"] = currentMileage.toInt()
        vehicle["weeklyRidingDistance"] = weeklyRidingDistance.toInt()

        val userId = currentUser.uid
        val usersVehiclesRef = database.reference.child("users_vehicles").child(userId)
        val newVehicleId = usersVehiclesRef.push().key

        if (newVehicleId != null) {
            usersVehiclesRef.child(newVehicleId).setValue(vehicle)
                .addOnSuccessListener {
                    ReminderScheduler.scheduleForVehicle(
                        this, registrationNumber,
                        currentMileage.toInt(), weeklyRidingDistance.toInt()
                    )
                    Toast.makeText(this, "Vehicle saved successfully", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save vehicle", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateVehicle() {
        val registrationNumber = etRegistrationNumber.text.toString().trim()
        val manufacturedYear = etManufacturedYear.text.toString().trim()
        val currentMileage = etCurrentMileage.text.toString().trim()
        val weeklyRidingDistance = etWeeklyRidingDistance.text.toString().trim()

        if (!validateInputs(registrationNumber, manufacturedYear, currentMileage, weeklyRidingDistance)) {
            return
        }

        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val vehicleUpdates = HashMap<String, Any>()
        vehicleUpdates["registrationNumber"] = registrationNumber
        vehicleUpdates["brand"] = selectedBrand
        vehicleUpdates["model"] = selectedModel
        vehicleUpdates["manufacturedYear"] = manufacturedYear
        vehicleUpdates["currentMileage"] = currentMileage.toInt()
        vehicleUpdates["weeklyRidingDistance"] = weeklyRidingDistance.toInt()

        val userId = currentUser.uid
        database.reference.child("users_vehicles").child(userId).child(vehicleId!!)
            .updateChildren(vehicleUpdates)
            .addOnSuccessListener {
                // Also update the registration number in services if it was changed
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

    private fun updateServicesRegistrationNumber(userId: String, oldRegNumber: String, newRegNumber: String) {
        val servicesRef = database.reference.child("users_services").child(userId)

        // First get all services under old registration number
        servicesRef.child(oldRegNumber).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Move services to new registration number
                    servicesRef.child(newRegNumber).setValue(snapshot.value)
                        .addOnSuccessListener {
                            // Remove old services
                            servicesRef.child(oldRegNumber).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this@AddVehicleActivity,
                                        "Vehicle and services updated successfully",
                                        Toast.LENGTH_SHORT).show()
                                    navigateToHome()
                                }
                        }
                } else {
                    Toast.makeText(this@AddVehicleActivity,
                        "Vehicle updated successfully",
                        Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddVehicleActivity,
                    "Vehicle updated but services may not be updated",
                    Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
        })
    }

    private fun validateInputs(
        registrationNumber: String,
        manufacturedYear: String,
        currentMileage: String,
        weeklyRidingDistance: String
    ): Boolean {
        if (registrationNumber.isEmpty() || selectedBrand.isEmpty() || selectedModel.isEmpty() ||
            manufacturedYear.isEmpty() || currentMileage.isEmpty() || weeklyRidingDistance.isEmpty()) {
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