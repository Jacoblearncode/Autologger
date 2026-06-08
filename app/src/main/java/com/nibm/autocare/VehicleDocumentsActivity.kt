package com.nibm.autocare

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class VehicleDocumentsActivity : AppCompatActivity() {

    private lateinit var vehicleRegistration: String
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private lateinit var tvInsuranceExpiry: TextView
    private lateinit var tvInsuranceCountdown: TextView
    private lateinit var tvRoadTaxExpiry: TextView
    private lateinit var tvRoadTaxCountdown: TextView
    private lateinit var tvFitnessExpiry: TextView
    private lateinit var tvFitnessCountdown: TextView

    private lateinit var docsRef: com.google.firebase.database.DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_documents)

        vehicleRegistration = intent.getStringExtra("vehicleRegistration") ?: ""
        findViewById<TextView>(R.id.tvAppName).text = "Documents — $vehicleRegistration"

        tvInsuranceExpiry = findViewById(R.id.tvInsuranceExpiry)
        tvInsuranceCountdown = findViewById(R.id.tvInsuranceCountdown)
        tvRoadTaxExpiry = findViewById(R.id.tvRoadTaxExpiry)
        tvRoadTaxCountdown = findViewById(R.id.tvRoadTaxCountdown)
        tvFitnessExpiry = findViewById(R.id.tvFitnessExpiry)
        tvFitnessCountdown = findViewById(R.id.tvFitnessCountdown)

        val uid = auth.currentUser?.uid ?: return
        docsRef = database.reference
            .child("users_vehicle_documents")
            .child(uid)
            .child(vehicleRegistration)

        loadDocuments()

        findViewById<ImageButton>(R.id.btnEditInsurance).setOnClickListener {
            showDatePicker("insurance", tvInsuranceExpiry, tvInsuranceCountdown)
        }
        findViewById<ImageButton>(R.id.btnDeleteInsurance).setOnClickListener {
            confirmClear("insurance", tvInsuranceExpiry, tvInsuranceCountdown)
        }
        findViewById<ImageButton>(R.id.btnEditRoadTax).setOnClickListener {
            showDatePicker("road_tax", tvRoadTaxExpiry, tvRoadTaxCountdown)
        }
        findViewById<ImageButton>(R.id.btnDeleteRoadTax).setOnClickListener {
            confirmClear("road_tax", tvRoadTaxExpiry, tvRoadTaxCountdown)
        }
        findViewById<ImageButton>(R.id.btnEditFitness).setOnClickListener {
            showDatePicker("fitness", tvFitnessExpiry, tvFitnessCountdown)
        }
        findViewById<ImageButton>(R.id.btnDeleteFitness).setOnClickListener {
            confirmClear("fitness", tvFitnessExpiry, tvFitnessCountdown)
        }
    }

    private fun confirmClear(field: String, tvDate: TextView, tvCountdown: TextView) {
        AlertDialog.Builder(this)
            .setTitle("Clear Date")
            .setMessage("Remove the saved expiry date for this document?")
            .setPositiveButton("Clear") { _, _ ->
                docsRef.child(field).removeValue()
                    .addOnSuccessListener { updateCard(null, tvDate, tvCountdown) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadDocuments() {
        docsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateCard(snapshot.child("insurance").getValue(String::class.java), tvInsuranceExpiry, tvInsuranceCountdown)
                updateCard(snapshot.child("road_tax").getValue(String::class.java), tvRoadTaxExpiry, tvRoadTaxCountdown)
                updateCard(snapshot.child("fitness").getValue(String::class.java), tvFitnessExpiry, tvFitnessCountdown)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateCard(dateStr: String?, tvDate: TextView, tvCountdown: TextView) {
        if (dateStr.isNullOrEmpty()) {
            tvDate.text = "Not set"
            tvDate.setTextColor(ContextCompat.getColor(this, R.color.gray))
            tvCountdown.text = ""
            return
        }

        tvDate.text = dateStr

        try {
            val expiry = dateFormat.parse(dateStr) ?: return
            val today = Calendar.getInstance().time
            val diffMs = expiry.time - today.time
            val days = TimeUnit.MILLISECONDS.toDays(diffMs)

            when {
                days < 0 -> {
                    tvDate.setTextColor(ContextCompat.getColor(this, R.color.red))
                    tvCountdown.text = "Expired ${-days} day${if (-days == 1L) "" else "s"} ago"
                    tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.red))
                }
                days <= 30 -> {
                    tvDate.setTextColor(ContextCompat.getColor(this, R.color.dark_yellow))
                    tvCountdown.text = "Expires in $days day${if (days == 1L) "" else "s"}"
                    tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.dark_yellow))
                }
                else -> {
                    tvDate.setTextColor(ContextCompat.getColor(this, R.color.green))
                    tvCountdown.text = "$days days remaining"
                    tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.green))
                }
            }
        } catch (e: Exception) {
            tvDate.setTextColor(ContextCompat.getColor(this, R.color.gray))
            tvCountdown.text = ""
        }
    }

    private fun showDatePicker(field: String, tvDate: TextView, tvCountdown: TextView) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                val dateStr = dateFormat.format(picked.time)
                docsRef.child(field).setValue(dateStr)
                    .addOnSuccessListener { updateCard(dateStr, tvDate, tvCountdown) }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
