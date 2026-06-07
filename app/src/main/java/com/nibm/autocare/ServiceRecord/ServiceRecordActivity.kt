package com.nibm.autocare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.nibm.autocare.ServiceRecord.ServiceViewModel
import com.nibm.autocare.ServiceRecord.ServiceViewModelFactory
import com.nibm.autocare.Vehicle.AddVehicleActivity
import com.nibm.autocare.adapter.ServiceRecordAdapter
import kotlinx.coroutines.launch
import java.io.File

class ServiceRecordActivity : AppCompatActivity() {

    private lateinit var viewModel: ServiceViewModel
    private lateinit var rvServiceRecords: RecyclerView
    private lateinit var serviceRecordAdapter: ServiceRecordAdapter
    private lateinit var pdfGenerator: PdfGenerator

    private lateinit var tvSvcTotal: TextView
    private lateinit var tvFuelTotal: TextView
    private lateinit var tvCombinedTotal: TextView
    private lateinit var tvAvgEfficiency: TextView
    private lateinit var tvCostPerKm: TextView
    private lateinit var tvNextService: TextView
    private lateinit var tvRecordCount: TextView

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_record)

        val vehicleRegistration = intent.getStringExtra("vehicleRegistration") ?: ""
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        pdfGenerator = PdfGenerator(this)

        findViewById<TextView>(R.id.tvAppName).text = "Services for $vehicleRegistration"
        tvSvcTotal = findViewById(R.id.tvSvcTotal)
        tvFuelTotal = findViewById(R.id.tvFuelTotal)
        tvCombinedTotal = findViewById(R.id.tvCombinedTotal)
        tvAvgEfficiency = findViewById(R.id.tvAvgEfficiency)
        tvCostPerKm = findViewById(R.id.tvCostPerKm)
        tvNextService = findViewById(R.id.tvNextService)
        tvRecordCount = findViewById(R.id.tvRecordCount)

        setupRecyclerView()
        setupClickListeners(vehicleRegistration)

        viewModel = ViewModelProvider(
            this, ServiceViewModelFactory(userId, vehicleRegistration)
        )[ServiceViewModel::class.java]

        observeViewModel()
    }

    private fun setupRecyclerView() {
        rvServiceRecords = findViewById(R.id.rvServiceRecords)
        rvServiceRecords.layoutManager = LinearLayoutManager(this)
        serviceRecordAdapter = ServiceRecordAdapter(
            onDeleteClick = { recordId ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Service Record")
                    .setMessage("Are you sure you want to delete this service record?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteServiceRecord(recordId) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rvServiceRecords.adapter = serviceRecordAdapter
    }

    private fun observeViewModel() {
        viewModel.serviceRecords.observe(this) { records ->
            serviceRecordAdapter.submitList(records)
            val isEmpty = records.isEmpty()
            rvServiceRecords.visibility = if (isEmpty) View.GONE else View.VISIBLE
            findViewById<View>(R.id.emptyStateServices).visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        // MediatorLiveData fires whenever service records OR fuel data updates
        viewModel.combinedStats.observe(this) { stats ->
            tvSvcTotal.text = "Rs ${formatAmount(stats.svcTotal)}"
            tvFuelTotal.text = "Rs ${formatAmount(stats.fuelTotal)}"
            tvCombinedTotal.text = "Rs ${formatAmount(stats.combinedTotal)}"
            tvRecordCount.text = "${stats.recordCount}"
            tvAvgEfficiency.text = stats.avgEfficiency
            tvCostPerKm.text = stats.costPerKm

            when {
                stats.nextServiceKm < 0 -> tvNextService.text = "—"
                stats.nextServiceKm <= 0 -> {
                    tvNextService.text = "OVERDUE"
                    tvNextService.setTextColor(ContextCompat.getColor(this, R.color.red))
                }
                stats.nextServiceKm <= 1000 -> {
                    tvNextService.text = "%.0f km".format(stats.nextServiceKm)
                    tvNextService.setTextColor(ContextCompat.getColor(this, R.color.accent_lime))
                }
                else -> {
                    tvNextService.text = "%.0f km".format(stats.nextServiceKm)
                    tvNextService.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
            }
        }
    }

    private fun setupClickListeners(vehicleRegistration: String) {
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
        findViewById<View>(R.id.btnTrips).setOnClickListener {
            startActivity(Intent(this, TripLogActivity::class.java).apply {
                putExtra("vehicleRegistration", vehicleRegistration)
            })
        }
        findViewById<View>(R.id.btnParts).setOnClickListener {
            startActivity(Intent(this, PartsWarrantyActivity::class.java).apply {
                putExtra("vehicleRegistration", vehicleRegistration)
            })
        }
        findViewById<View>(R.id.btnDocuments).setOnClickListener {
            startActivity(Intent(this, VehicleDocumentsActivity::class.java).apply {
                putExtra("vehicleRegistration", vehicleRegistration)
            })
        }
        findViewById<View>(R.id.btnDownloadPdf).setOnClickListener {
            val records = viewModel.serviceRecords.value ?: emptyList()
            if (records.isEmpty()) {
                Toast.makeText(this, "No service records to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Export Service Records")
                .setItems(arrayOf("Export as PDF", "Export as CSV")) { _, which ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || hasStoragePermissions()) {
                        if (which == 0) generatePdf(vehicleRegistration) else generateCsv(vehicleRegistration)
                    } else {
                        requestStoragePermissions()
                    }
                }
                .show()
        }
    }

    private fun generatePdf(vehicleRegistration: String) {
        val progressDialog = AlertDialog.Builder(this).setMessage("Generating PDF...").setCancelable(false).create()
        progressDialog.show()
        lifecycleScope.launch {
            val records = viewModel.serviceRecords.value ?: emptyList()
            val (filePath, success) = pdfGenerator.generateServiceRecordPdf(vehicleRegistration, records)
            progressDialog.dismiss()
            if (success && filePath != null) shareFile(filePath, "application/pdf")
            else Toast.makeText(this@ServiceRecordActivity, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateCsv(vehicleRegistration: String) {
        val progressDialog = AlertDialog.Builder(this).setMessage("Generating CSV...").setCancelable(false).create()
        progressDialog.show()
        lifecycleScope.launch {
            val records = viewModel.serviceRecords.value ?: emptyList()
            val (filePath, success) = pdfGenerator.generateServiceRecordCsv(vehicleRegistration, records)
            progressDialog.dismiss()
            if (success && filePath != null) shareFile(filePath, "text/csv")
            else Toast.makeText(this@ServiceRecordActivity, "Failed to generate CSV", Toast.LENGTH_SHORT).show()
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

    private fun formatAmount(value: Double): String =
        if (value == 0.0) "0" else "%,.0f".format(value)

    private fun hasStoragePermissions(): Boolean =
        REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                generatePdf(viewModel.vehicleRegistration)
            } else {
                Toast.makeText(this, "Permission denied. Enable it in app settings.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
