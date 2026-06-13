package com.nibm.autocare

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.Reminder.ReminderScheduler
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
    private lateinit var tvInsuranceBadge: TextView
    private lateinit var vStripeInsurance: View

    private lateinit var tvRoadTaxExpiry: TextView
    private lateinit var tvRoadTaxCountdown: TextView
    private lateinit var tvRoadTaxBadge: TextView
    private lateinit var vStripeRoadTax: View

    private lateinit var tvFitnessExpiry: TextView
    private lateinit var tvFitnessCountdown: TextView
    private lateinit var tvFitnessBadge: TextView
    private lateinit var vStripeFitness: View

    private lateinit var docsRef: com.google.firebase.database.DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_documents)

        vehicleRegistration = intent.getStringExtra("vehicleRegistration") ?: ""
        findViewById<TextView>(R.id.tvAppName).text = "Documents — $vehicleRegistration"

        findViewById<ImageButton>(R.id.btnBackDocs).setOnClickListener { finish() }

        tvInsuranceExpiry    = findViewById(R.id.tvInsuranceExpiry)
        tvInsuranceCountdown = findViewById(R.id.tvInsuranceCountdown)
        tvInsuranceBadge     = findViewById(R.id.tvInsuranceBadge)
        vStripeInsurance     = findViewById(R.id.stripeInsurance)

        tvRoadTaxExpiry      = findViewById(R.id.tvRoadTaxExpiry)
        tvRoadTaxCountdown   = findViewById(R.id.tvRoadTaxCountdown)
        tvRoadTaxBadge       = findViewById(R.id.tvRoadTaxBadge)
        vStripeRoadTax       = findViewById(R.id.stripeRoadTax)

        tvFitnessExpiry      = findViewById(R.id.tvFitnessExpiry)
        tvFitnessCountdown   = findViewById(R.id.tvFitnessCountdown)
        tvFitnessBadge       = findViewById(R.id.tvFitnessBadge)
        vStripeFitness       = findViewById(R.id.stripeFitness)

        val uid = auth.currentUser?.uid ?: return
        docsRef = database.reference
            .child("users_vehicle_documents")
            .child(uid)
            .child(vehicleRegistration)

        loadDocuments()

        findViewById<ImageButton>(R.id.btnEditInsurance).setOnClickListener {
            showDatePicker("insurance", tvInsuranceExpiry, tvInsuranceCountdown, tvInsuranceBadge, vStripeInsurance)
        }
        findViewById<ImageButton>(R.id.btnDeleteInsurance).setOnClickListener {
            confirmClear("insurance", tvInsuranceExpiry, tvInsuranceCountdown, tvInsuranceBadge, vStripeInsurance)
        }
        findViewById<ImageButton>(R.id.btnEditRoadTax).setOnClickListener {
            showDatePicker("road_tax", tvRoadTaxExpiry, tvRoadTaxCountdown, tvRoadTaxBadge, vStripeRoadTax)
        }
        findViewById<ImageButton>(R.id.btnDeleteRoadTax).setOnClickListener {
            confirmClear("road_tax", tvRoadTaxExpiry, tvRoadTaxCountdown, tvRoadTaxBadge, vStripeRoadTax)
        }
        findViewById<ImageButton>(R.id.btnEditFitness).setOnClickListener {
            showDatePicker("fitness", tvFitnessExpiry, tvFitnessCountdown, tvFitnessBadge, vStripeFitness)
        }
        findViewById<ImageButton>(R.id.btnDeleteFitness).setOnClickListener {
            confirmClear("fitness", tvFitnessExpiry, tvFitnessCountdown, tvFitnessBadge, vStripeFitness)
        }
    }

    private fun confirmClear(
        field: String,
        tvDate: TextView,
        tvCountdown: TextView,
        tvBadge: TextView,
        vStripe: View
    ) {
        AlertDialog.Builder(this)
            .setTitle("Clear Date")
            .setMessage("Remove the saved expiry date for this document?")
            .setPositiveButton("Clear") { _, _ ->
                docsRef.child(field).removeValue()
                    .addOnSuccessListener { updateCard(null, tvDate, tvCountdown, tvBadge, vStripe) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadDocuments() {
        docsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateCard(snapshot.child("insurance").getValue(String::class.java),
                    tvInsuranceExpiry, tvInsuranceCountdown, tvInsuranceBadge, vStripeInsurance)
                updateCard(snapshot.child("road_tax").getValue(String::class.java),
                    tvRoadTaxExpiry, tvRoadTaxCountdown, tvRoadTaxBadge, vStripeRoadTax)
                updateCard(snapshot.child("fitness").getValue(String::class.java),
                    tvFitnessExpiry, tvFitnessCountdown, tvFitnessBadge, vStripeFitness)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateCard(
        dateStr: String?,
        tvDate: TextView,
        tvCountdown: TextView,
        tvBadge: TextView,
        vStripe: View
    ) {
        if (dateStr.isNullOrEmpty()) {
            tvDate.text = "Not set"
            tvDate.setTextColor(ContextCompat.getColor(this, R.color.gray))
            tvCountdown.text = ""
            applyBadge(tvBadge, vStripe, BadgeState.NOT_SET)
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
                    applyBadge(tvBadge, vStripe, BadgeState.EXPIRED)
                }
                days <= 30 -> {
                    tvDate.setTextColor(ContextCompat.getColor(this, R.color.dark_yellow))
                    tvCountdown.text = "Expires in $days day${if (days == 1L) "" else "s"}"
                    tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.dark_yellow))
                    applyBadge(tvBadge, vStripe, BadgeState.EXPIRING)
                }
                else -> {
                    tvDate.setTextColor(ContextCompat.getColor(this, R.color.green))
                    tvCountdown.text = "$days days remaining"
                    tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.green))
                    applyBadge(tvBadge, vStripe, BadgeState.VALID)
                }
            }
        } catch (e: Exception) {
            tvDate.setTextColor(ContextCompat.getColor(this, R.color.gray))
            tvCountdown.text = ""
            applyBadge(tvBadge, vStripe, BadgeState.NOT_SET)
        }
    }

    private enum class BadgeState { VALID, EXPIRING, EXPIRED, NOT_SET }

    private fun applyBadge(tvBadge: TextView, vStripe: View, state: BadgeState) {
        val (text, badgeDrawable, textColorRes, stripeColorRes) = when (state) {
            BadgeState.VALID     -> arrayOf("VALID",    R.drawable.bg_badge_valid,    R.color.green,       R.color.green)
            BadgeState.EXPIRING  -> arrayOf("EXPIRING", R.drawable.bg_badge_expiring, R.color.dark_yellow, R.color.dark_yellow)
            BadgeState.EXPIRED   -> arrayOf("EXPIRED",  R.drawable.bg_badge_expired,  R.color.red,         R.color.red)
            BadgeState.NOT_SET   -> arrayOf("NOT SET",  R.drawable.bg_badge_notset,   R.color.gray,        R.color.gray)
        }
        tvBadge.text = text as String
        tvBadge.setBackgroundResource(badgeDrawable as Int)
        tvBadge.setTextColor(ContextCompat.getColor(this, textColorRes as Int))
        vStripe.setBackgroundColor(ContextCompat.getColor(this, stripeColorRes as Int))
    }

    private fun showDatePicker(
        field: String,
        tvDate: TextView,
        tvCountdown: TextView,
        tvBadge: TextView,
        vStripe: View
    ) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                val dateStr = dateFormat.format(picked.time)
                updateCard(dateStr, tvDate, tvCountdown, tvBadge, vStripe)
                docsRef.child(field).setValue(dateStr)
                    .addOnSuccessListener {
                        ReminderScheduler.scheduleDocumentReminder(
                            this@VehicleDocumentsActivity, vehicleRegistration, field, dateStr
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@VehicleDocumentsActivity,
                            "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
