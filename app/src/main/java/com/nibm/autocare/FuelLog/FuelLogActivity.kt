package com.nibm.autocare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.Vehicle.AddVehicleActivity
import kotlinx.coroutines.launch
import java.io.File

class FuelLogActivity : AppCompatActivity() {

    private lateinit var lvFuelLogs: ListView
    private lateinit var spinnerFilter: Spinner
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var fuelLogsRef: DatabaseReference

    private val allLogs = mutableListOf<FuelLog>()
    private val displayedLogs = mutableListOf<FuelLog>()
    private var logsListener: ValueEventListener? = null
    private var selectedVehicle = ALL_VEHICLES
    private lateinit var pdfGenerator: PdfGenerator

    companion object {
        const val EXTRA_VEHICLE = "vehicleRegistration"
        private const val ALL_VEHICLES = "All Vehicles"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuel_log)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        lvFuelLogs = findViewById(R.id.lvFuelLogs)
        spinnerFilter = findViewById(R.id.spinnerVehicleFilter)

        val emptyState = findViewById<View>(R.id.emptyStateFuel)
        tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = emptyState.findViewById(R.id.tvEmptySubtitle)
        lvFuelLogs.setEmptyView(emptyState)

        selectedVehicle = intent.getStringExtra(EXTRA_VEHICLE) ?: ALL_VEHICLES

        fuelLogsRef = database.reference
            .child("users_fuel_logs")
            .child(auth.currentUser?.uid ?: "")

        pdfGenerator = PdfGenerator(this)

        setupNavigation()
        setupFilterSpinner()

        findViewById<View>(R.id.btnAddFuelLog).setOnClickListener {
            startActivity(Intent(this, AddFuelLogActivity::class.java))
        }

        findViewById<View>(R.id.btnFuelTrend).setOnClickListener {
            startActivity(Intent(this, FuelPriceTrendActivity::class.java))
        }

        findViewById<View>(R.id.btnDownloadFuelPdf).setOnClickListener {
            if (displayedLogs.isEmpty()) {
                Toast.makeText(this, "No fuel logs to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Export Fuel Log")
                .setItems(arrayOf("Export as PDF", "Export as CSV")) { _, which ->
                    if (which == 0) generateFuelPdf() else generateFuelCsv()
                }
                .show()
        }
    }

    override fun onStart() {
        super.onStart()
        attachListener()
    }

    override fun onStop() {
        super.onStop()
        logsListener?.let { fuelLogsRef.removeEventListener(it) }
        logsListener = null
    }

    private fun attachListener() {
        logsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allLogs.clear()
                for (snap in snapshot.children) {
                    parseFuelLog(snap)?.let { allLogs.add(it) }
                }
                allLogs.sortByDescending { it.date }
                calculateEfficiency(allLogs)
                updateFilterSpinner()
                applyFilter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FuelLogActivity, "Failed to load fuel logs", Toast.LENGTH_SHORT).show()
            }
        }
        fuelLogsRef.addValueEventListener(logsListener!!)
    }

    private fun parseFuelLog(snap: DataSnapshot): FuelLog? {
        return try {
            FuelLog(
                id = snap.key ?: return null,
                registrationNumber = snap.child("registrationNumber").getValue(String::class.java) ?: "",
                date = snap.child("date").getValue(String::class.java) ?: "",
                odometer = snap.child("odometer").getValue(String::class.java) ?: "",
                liters = snap.child("liters").getValue(String::class.java) ?: "",
                pricePerLiter = snap.child("pricePerLiter").getValue(String::class.java) ?: "",
                totalCost = snap.child("totalCost").getValue(String::class.java) ?: "",
                fuelType = snap.child("fuelType").getValue(String::class.java) ?: "",
                notes = snap.child("notes").getValue(String::class.java) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateEfficiency(logs: List<FuelLog>) {
        val byVehicle = logs.groupBy { it.registrationNumber }
        for ((_, vehicleLogs) in byVehicle) {
            val sorted = vehicleLogs.sortedBy { it.odometer.toDoubleOrNull() ?: 0.0 }
            for (i in 1 until sorted.size) {
                val curr = sorted[i]
                val prev = sorted[i - 1]
                val currOdo = curr.odometer.toDoubleOrNull() ?: continue
                val prevOdo = prev.odometer.toDoubleOrNull() ?: continue
                val liters = curr.liters.toDoubleOrNull() ?: continue
                if (liters > 0 && currOdo > prevOdo) {
                    curr.efficiency = String.format("%.1f km/L", (currOdo - prevOdo) / liters)
                }
            }
        }
    }

    private fun updateFilterSpinner() {
        val vehicles = mutableListOf(ALL_VEHICLES)
        vehicles.addAll(allLogs.map { it.registrationNumber }.distinct().sorted())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerFilter.onItemSelectedListener = null
        spinnerFilter.adapter = adapter

        val idx = vehicles.indexOf(selectedVehicle)
        if (idx >= 0) spinnerFilter.setSelection(idx, false)

        setupFilterSpinner()
    }

    private fun setupFilterSpinner() {
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedVehicle = spinnerFilter.getItemAtPosition(pos).toString()
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilter() {
        displayedLogs.clear()
        if (selectedVehicle == ALL_VEHICLES) {
            displayedLogs.addAll(allLogs)
        } else {
            displayedLogs.addAll(allLogs.filter { it.registrationNumber == selectedVehicle })
        }

        if (displayedLogs.isEmpty()) {
            if (allLogs.isEmpty()) {
                tvEmptyTitle.text = "No fuel logs yet"
                tvEmptySubtitle.text = "Tap + to log your first fill-up"
            } else {
                tvEmptyTitle.text = "No logs for this vehicle"
                tvEmptySubtitle.text = "Select a different vehicle or tap + to add a log"
            }
        }

        lvFuelLogs.adapter = FuelLogAdapter(displayedLogs)
    }

    private fun showDeleteConfirmation(logId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Fuel Log")
            .setMessage("Are you sure you want to delete this fuel log?")
            .setPositiveButton("Delete") { _, _ ->
                fuelLogsRef.child(logId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Fuel log deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateFuelPdf() {
        val progress = AlertDialog.Builder(this)
            .setMessage("Generating PDF...")
            .setCancelable(false)
            .create()
            .also { it.show() }

        lifecycleScope.launch {
            val label = if (selectedVehicle == ALL_VEHICLES) ALL_VEHICLES else selectedVehicle
            val (filePath, success) = pdfGenerator.generateFuelLogPdf(label, displayedLogs)
            progress.dismiss()
            if (success && filePath != null) {
                shareFile(filePath, "application/pdf")
            } else {
                Toast.makeText(this@FuelLogActivity, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateFuelCsv() {
        val progress = AlertDialog.Builder(this)
            .setMessage("Generating CSV...")
            .setCancelable(false)
            .create()
            .also { it.show() }

        lifecycleScope.launch {
            val label = if (selectedVehicle == ALL_VEHICLES) ALL_VEHICLES else selectedVehicle
            val (filePath, success) = pdfGenerator.generateFuelLogCsv(label, displayedLogs)
            progress.dismiss()
            if (success && filePath != null) {
                shareFile(filePath, "text/csv")
            } else {
                Toast.makeText(this@FuelLogActivity, "Failed to generate CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareFile(filePath: String, mimeType: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
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
    }

    inner class FuelLogAdapter(private val logs: List<FuelLog>) : BaseAdapter() {
        private val expandedPositions = mutableSetOf<Int>()

        override fun getCount() = logs.size
        override fun getItem(pos: Int): Any = logs[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val vh: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(parent?.context)
                    .inflate(R.layout.list_item_fuel_record, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            val log = logs[position]

            vh.tvDate.text = log.date
            vh.tvOdometer.text = "${log.odometer} km"
            vh.tvRegistration.text = log.registrationNumber
            vh.tvTotalCost.text = if (log.totalCost.isNotBlank()) "Rs ${log.totalCost}" else "—"
            vh.tvLitersSummary.text = "${log.liters} L"
            vh.tvFuelType.text = log.fuelType

            vh.tvPricePerLiter.text = if (log.pricePerLiter.isNotBlank())
                "Rs ${log.pricePerLiter}/L" else "Price not recorded"

            if (log.efficiency.isNotBlank()) {
                vh.tvEfficiency.text = "Efficiency: ${log.efficiency}"
                vh.tvEfficiency.visibility = View.VISIBLE
            } else {
                vh.tvEfficiency.visibility = View.GONE
            }

            if (log.notes.isNotBlank()) {
                vh.tvNotes.text = log.notes
                vh.tvNotes.visibility = View.VISIBLE
            } else {
                vh.tvNotes.visibility = View.GONE
            }

            vh.llExpandedDetails.visibility =
                if (expandedPositions.contains(position)) View.VISIBLE else View.GONE

            view.setOnClickListener {
                if (expandedPositions.contains(position)) expandedPositions.remove(position)
                else expandedPositions.add(position)
                notifyDataSetChanged()
            }

            vh.btnDelete.setOnClickListener {
                showDeleteConfirmation(log.id)
            }

            return view
        }

        private inner class ViewHolder(view: View) {
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvOdometer: TextView = view.findViewById(R.id.tvOdometer)
            val tvRegistration: TextView = view.findViewById(R.id.tvRegistration)
            val tvTotalCost: TextView = view.findViewById(R.id.tvTotalCost)
            val tvLitersSummary: TextView = view.findViewById(R.id.tvLitersSummary)
            val tvFuelType: TextView = view.findViewById(R.id.tvFuelType)
            val tvPricePerLiter: TextView = view.findViewById(R.id.tvPricePerLiter)
            val tvEfficiency: TextView = view.findViewById(R.id.tvEfficiency)
            val tvNotes: TextView = view.findViewById(R.id.tvNotes)
            val llExpandedDetails: LinearLayout = view.findViewById(R.id.llExpandedDetails)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        }
    }

    data class FuelLog(
        val id: String,
        val registrationNumber: String,
        val date: String,
        val odometer: String,
        val liters: String,
        val pricePerLiter: String,
        val totalCost: String,
        val fuelType: String,
        val notes: String,
        var efficiency: String = ""
    )
}
