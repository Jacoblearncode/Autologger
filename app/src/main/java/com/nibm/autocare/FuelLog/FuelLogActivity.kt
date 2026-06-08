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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.nibm.autocare.FuelLog.FuelLogViewModel
import com.nibm.autocare.FuelLog.FuelLogViewModelFactory
import com.nibm.autocare.FuelLog.FuelPriceTrendActivity
import com.nibm.autocare.Vehicle.AddVehicleActivity
import com.nibm.autocare.model.FuelLog
import kotlinx.coroutines.launch
import java.io.File

/**
 * Displays the user's fuel fill-up history with vehicle filter and PDF/CSV export.
 *
 * Follows MVVM: FuelLogViewModel owns the Firebase listener and exposes LiveData.
 * This Activity only handles UI: filtering, adapter setup, navigation, and file export.
 */
class FuelLogActivity : AppCompatActivity() {

    private lateinit var viewModel: FuelLogViewModel
    private lateinit var lvFuelLogs: ListView
    private lateinit var spinnerFilter: Spinner
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView

    // Local filter state; always a subset of viewModel.fuelLogs.value
    private val displayedLogs = mutableListOf<FuelLog>()
    private var selectedVehicle = ALL_VEHICLES
    private lateinit var pdfGenerator: PdfGenerator

    companion object {
        const val EXTRA_VEHICLE = "vehicleRegistration"
        private const val ALL_VEHICLES = "All Vehicles"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuel_log)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lvFuelLogs = findViewById(R.id.lvFuelLogs)
        spinnerFilter = findViewById(R.id.spinnerVehicleFilter)

        val emptyState = findViewById<View>(R.id.emptyStateFuel)
        tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = emptyState.findViewById(R.id.tvEmptySubtitle)
        lvFuelLogs.setEmptyView(emptyState)

        selectedVehicle = intent.getStringExtra(EXTRA_VEHICLE) ?: ALL_VEHICLES
        pdfGenerator = PdfGenerator(this)

        viewModel = ViewModelProvider(this, FuelLogViewModelFactory(userId))[FuelLogViewModel::class.java]
        observeViewModel()
        setupNavigation()

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
            AlertDialog.Builder(this)
                .setTitle("Export Fuel Log")
                .setItems(arrayOf("Export as PDF", "Export as CSV")) { _, which ->
                    if (which == 0) generateFuelPdf() else generateFuelCsv()
                }
                .show()
        }
    }

    private fun observeViewModel() {
        // Full list from Firebase — re-apply filter and rebuild spinner on every change
        viewModel.fuelLogs.observe(this) { allLogs ->
            updateFilterSpinner(allLogs)
            applyFilter(allLogs)
        }

        viewModel.toastMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    private fun updateFilterSpinner(allLogs: List<FuelLog>) {
        val vehicles = mutableListOf(ALL_VEHICLES)
        vehicles.addAll(allLogs.map { it.registrationNumber }.distinct().sorted())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerFilter.onItemSelectedListener = null
        spinnerFilter.adapter = adapter

        val idx = vehicles.indexOf(selectedVehicle)
        if (idx >= 0) spinnerFilter.setSelection(idx, false)

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedVehicle = spinnerFilter.getItemAtPosition(pos).toString()
                applyFilter(viewModel.fuelLogs.value ?: emptyList())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilter(allLogs: List<FuelLog>) {
        displayedLogs.clear()
        displayedLogs.addAll(
            if (selectedVehicle == ALL_VEHICLES) allLogs
            else allLogs.filter { it.registrationNumber == selectedVehicle }
        )

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

    private fun confirmDelete(logId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Fuel Log")
            .setMessage("Are you sure you want to delete this fuel log?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteFuelLog(logId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchEditForm(log: FuelLog) {
        startActivity(Intent(this, AddFuelLogActivity::class.java).apply {
            putExtra("isEditMode", true)
            putExtra("logId", log.id)
            putExtra("vehicleRegistration", log.registrationNumber)
            putExtra("date", log.date)
            putExtra("odometer", log.odometer)
            putExtra("liters", log.liters)
            putExtra("pricePerLiter", log.pricePerLiter)
            putExtra("totalCost", log.totalCost)
            putExtra("fuelType", log.fuelType)
            putExtra("notes", log.notes)
        })
    }

    private fun generateFuelPdf() {
        val dialog = AlertDialog.Builder(this).setMessage("Generating PDF...").setCancelable(false).create().also { it.show() }
        lifecycleScope.launch {
            val label = if (selectedVehicle == ALL_VEHICLES) ALL_VEHICLES else selectedVehicle
            val (path, ok) = pdfGenerator.generateFuelLogPdf(label, displayedLogs)
            dialog.dismiss()
            if (ok && path != null) shareFile(path, "application/pdf")
            else Toast.makeText(this@FuelLogActivity, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateFuelCsv() {
        val dialog = AlertDialog.Builder(this).setMessage("Generating CSV...").setCancelable(false).create().also { it.show() }
        lifecycleScope.launch {
            val label = if (selectedVehicle == ALL_VEHICLES) ALL_VEHICLES else selectedVehicle
            val (path, ok) = pdfGenerator.generateFuelLogCsv(label, displayedLogs)
            dialog.dismiss()
            if (ok && path != null) shareFile(path, "text/csv")
            else Toast.makeText(this@FuelLogActivity, "Failed to generate CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(filePath: String, mimeType: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            },
            "Share ${file.name}"
        ))
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
            vh.tvPricePerLiter.text = if (log.pricePerLiter.isNotBlank()) "Rs ${log.pricePerLiter}/L" else "Price not recorded"

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

            vh.btnEdit.setOnClickListener { launchEditForm(log) }
            vh.btnDelete.setOnClickListener { confirmDelete(log.id) }

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
            val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        }
    }
}
