package com.nibm.autocare

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * U8 — Side-by-side comparison of two vehicles.
 * Pulls service + fuel data for the whole user once, then recomputes the
 * comparison table locally whenever either spinner selection changes.
 */
class CompareVehiclesActivity : AppCompatActivity() {

    private data class VehicleStats(
        val serviceCost: Double = 0.0,
        val serviceCount: Int = 0,
        val fuelCost: Double = 0.0,
        val lastServiceOdo: Double = 0.0
    )

    private val uid by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private val statsByReg = mutableMapOf<String, VehicleStats>()
    private var regs = listOf<String>()

    private lateinit var spinnerA: Spinner
    private lateinit var spinnerB: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare_vehicles)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        spinnerA = findViewById(R.id.spinnerVehicleA)
        spinnerB = findViewById(R.id.spinnerVehicleB)

        loadData()
    }

    private fun loadData() {
        val db = FirebaseDatabase.getInstance().reference
        db.child("users_vehicles").child(uid).get().addOnSuccessListener { vehSnap ->
            regs = vehSnap.children.mapNotNull {
                it.child("registrationNumber").getValue(String::class.java)
            }
            if (regs.size < 2) {
                showNotEnough()
                return@addOnSuccessListener
            }

            db.child("users_services").child(uid).get().addOnSuccessListener { svcSnap ->
                for (reg in regs) {
                    var cost = 0.0; var count = 0; var maxOdo = 0.0
                    for (record in svcSnap.child(reg).children) {
                        cost += record.child("serviceCost").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                        val odo = record.child("odometerReading").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                        if (odo > maxOdo) maxOdo = odo
                        count++
                    }
                    statsByReg[reg] = VehicleStats(cost, count, 0.0, maxOdo)
                }

                db.child("users_fuel_logs").child(uid).get().addOnSuccessListener { fuelSnap ->
                    val fuelByReg = mutableMapOf<String, Double>()
                    for (entry in fuelSnap.children) {
                        val reg = entry.child("registrationNumber").getValue(String::class.java) ?: continue
                        val cost = entry.child("totalCost").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                        fuelByReg[reg] = (fuelByReg[reg] ?: 0.0) + cost
                    }
                    for (reg in regs) {
                        statsByReg[reg] = statsByReg[reg]!!.copy(fuelCost = fuelByReg[reg] ?: 0.0)
                    }
                    setupSpinners()
                }
            }
        }
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regs)
        spinnerA.adapter = adapter
        spinnerB.adapter = adapter
        spinnerA.setSelection(0)
        spinnerB.setSelection(1)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = render()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerA.onItemSelectedListener = listener
        spinnerB.onItemSelectedListener = listener
        render()
    }

    private fun render() {
        val regA = spinnerA.selectedItem as? String ?: return
        val regB = spinnerB.selectedItem as? String ?: return
        val a = statsByReg[regA] ?: VehicleStats()
        val b = statsByReg[regB] ?: VehicleStats()
        val cur = SettingsManager.getCurrency(this)

        val container = findViewById<LinearLayout>(R.id.comparisonContainer)
        container.removeAllViews()

        // Lower total cost / fewer services is "better" → highlight the winner in lime.
        addRow(container, "Service cost", "$cur ${fmt(a.serviceCost)}", "$cur ${fmt(b.serviceCost)}",
            a.serviceCost <= b.serviceCost)
        addRow(container, "Service count", "${a.serviceCount}", "${b.serviceCount}",
            a.serviceCount <= b.serviceCount)
        addRow(container, "Fuel cost", "$cur ${fmt(a.fuelCost)}", "$cur ${fmt(b.fuelCost)}",
            a.fuelCost <= b.fuelCost)
        addRow(container, "Total spend",
            "$cur ${fmt(a.serviceCost + a.fuelCost)}", "$cur ${fmt(b.serviceCost + b.fuelCost)}",
            (a.serviceCost + a.fuelCost) <= (b.serviceCost + b.fuelCost))
        addRow(container, "Last service odo", "${fmt(a.lastServiceOdo)} km", "${fmt(b.lastServiceOdo)} km", null)
    }

    private fun addRow(parent: LinearLayout, label: String, valA: String, valB: String, aWins: Boolean?) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, dp(14))
        }

        val limeColor = ContextCompat.getColor(this, R.color.accent_lime)
        val primaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        val tvA = TextView(this).apply {
            text = valA
            textSize = 15f
            gravity = android.view.Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (aWins == true) limeColor else primaryColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvLabel = TextView(this).apply {
            text = label
            textSize = 11f
            gravity = android.view.Gravity.CENTER
            setTextColor(secondaryColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvB = TextView(this).apply {
            text = valB
            textSize = 15f
            gravity = android.view.Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (aWins == false) limeColor else primaryColor)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(tvA)
        row.addView(tvLabel)
        row.addView(tvB)
        parent.addView(row)

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(ContextCompat.getColor(this@CompareVehiclesActivity, R.color.gray_300))
        }
        parent.addView(divider)
    }

    private fun showNotEnough() {
        val container = findViewById<LinearLayout>(R.id.comparisonContainer)
        container.addView(TextView(this).apply {
            text = "Add at least two vehicles to compare."
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@CompareVehiclesActivity, R.color.text_secondary))
            setPadding(0, dp(24), 0, 0)
            gravity = android.view.Gravity.CENTER
        })
    }

    private fun fmt(v: Double): String = "%,.0f".format(v)
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
