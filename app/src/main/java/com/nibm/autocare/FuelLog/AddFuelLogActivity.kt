package com.nibm.autocare

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.Vehicle.AddVehicleActivity
import java.text.SimpleDateFormat
import java.util.*

class AddFuelLogActivity : AppCompatActivity() {

    private lateinit var spinnerVehicle: Spinner
    private lateinit var spinnerFuelType: Spinner
    private lateinit var etDate: EditText
    private lateinit var etOdometer: EditText
    private lateinit var etLiters: EditText
    private lateinit var etPricePerLiter: EditText
    private lateinit var etTotalCost: EditText
    private lateinit var etNotes: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var autoCalcEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_fuel_log)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        spinnerVehicle = findViewById(R.id.spinnerVehicle)
        spinnerFuelType = findViewById(R.id.spinnerFuelType)
        etDate = findViewById(R.id.etDate)
        etOdometer = findViewById(R.id.etOdometer)
        etLiters = findViewById(R.id.etLiters)
        etPricePerLiter = findViewById(R.id.etPricePerLiter)
        etTotalCost = findViewById(R.id.etTotalCost)
        etNotes = findViewById(R.id.etNotes)

        setupFuelTypeSpinner()
        setupDatePicker()
        setupAutoCalculate()
        setupNavigation()
        fetchVehicles()

        findViewById<View>(R.id.btnSave).setOnClickListener {
            if (validateInputs()) saveFuelLog()
        }
    }

    private fun setupFuelTypeSpinner() {
        val types = listOf("Petrol", "Diesel", "CNG")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFuelType.adapter = adapter
    }

    private fun setupDatePicker() {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etDate.setText(fmt.format(cal.time))

        val listener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            cal.set(year, month, day)
            etDate.setText(fmt.format(cal.time))
        }

        etDate.setOnClickListener {
            DatePickerDialog(
                this, listener,
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupAutoCalculate() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!autoCalcEnabled) return
                val liters = etLiters.text.toString().toDoubleOrNull() ?: return
                val price = etPricePerLiter.text.toString().toDoubleOrNull() ?: return
                autoCalcEnabled = false
                etTotalCost.setText(String.format("%.2f", liters * price))
                autoCalcEnabled = true
            }
        }
        etLiters.addTextChangedListener(watcher)
        etPricePerLiter.addTextChangedListener(watcher)
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.llHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<View>(R.id.llAddVehicle).setOnClickListener {
            startActivity(Intent(this, AddVehicleActivity::class.java))
        }
        findViewById<View>(R.id.llAddService).setOnClickListener {
            startActivity(Intent(this, AddServiceActivity::class.java))
        }
        findViewById<View>(R.id.llFuelLog).setOnClickListener {
            startActivity(Intent(this, FuelLogActivity::class.java))
        }
    }

    private fun fetchVehicles() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("users_vehicles").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val regs = mutableListOf<String>()
                    snapshot.children.forEach { v ->
                        v.child("registrationNumber").getValue(String::class.java)?.let { regs.add(it) }
                    }
                    if (regs.isEmpty()) {
                        Toast.makeText(
                            this@AddFuelLogActivity,
                            "No vehicles found. Add a vehicle first.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    val adapter = ArrayAdapter(
                        this@AddFuelLogActivity,
                        android.R.layout.simple_spinner_item,
                        regs
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerVehicle.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddFuelLogActivity, "Failed to load vehicles", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun validateInputs(): Boolean {
        return when {
            spinnerVehicle.adapter == null || spinnerVehicle.adapter.count == 0 -> {
                Toast.makeText(this, "Please add a vehicle first", Toast.LENGTH_SHORT).show()
                false
            }
            etDate.text.isNullOrBlank() -> {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                false
            }
            etOdometer.text.isNullOrBlank() -> {
                Toast.makeText(this, "Please enter odometer reading", Toast.LENGTH_SHORT).show()
                false
            }
            etLiters.text.isNullOrBlank() -> {
                Toast.makeText(this, "Please enter liters filled", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun saveFuelLog() {
        val uid = auth.currentUser?.uid ?: return
        val registration = spinnerVehicle.selectedItem?.toString() ?: return

        val data = hashMapOf(
            "registrationNumber" to registration,
            "date" to etDate.text.toString().trim(),
            "odometer" to etOdometer.text.toString().trim(),
            "liters" to etLiters.text.toString().trim(),
            "pricePerLiter" to etPricePerLiter.text.toString().trim(),
            "totalCost" to etTotalCost.text.toString().trim(),
            "fuelType" to (spinnerFuelType.selectedItem?.toString() ?: "Petrol"),
            "notes" to etNotes.text.toString().trim()
        )

        database.reference.child("users_fuel_logs").child(uid).push()
            .setValue(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Fuel log saved", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, FuelLogActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
