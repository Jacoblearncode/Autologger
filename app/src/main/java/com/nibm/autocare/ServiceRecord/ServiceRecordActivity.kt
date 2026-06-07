package com.nibm.autocare.ServiceRecord

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
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.nibm.autocare.AddServiceActivity
import com.nibm.autocare.FuelLogActivity
import com.nibm.autocare.HomeActivity
import com.nibm.autocare.PartsWarrantyActivity
import com.nibm.autocare.PdfGenerator
import com.nibm.autocare.R
import com.nibm.autocare.TripLogActivity
import com.nibm.autocare.Vehicle.AddVehicleActivity
import com.nibm.autocare.VehicleDocumentsActivity
import kotlinx.coroutines.launch
import java.io.File

/**
 * Container activity for the service record screen.
 * Owns ServiceViewModel and exposes it to child Fragments via requireActivity().
 * The two tabs (Timeline / Summary) are managed by ServicesFragment and StatsFragment.
 */
class ServiceRecordActivity : AppCompatActivity() {

    lateinit var viewModel: ServiceViewModel
    private lateinit var pdfGenerator: PdfGenerator

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

        // Create ViewModel before Fragments are attached so they can retrieve it
        viewModel = ViewModelProvider(
            this, ServiceViewModelFactory(userId, vehicleRegistration)
        )[ServiceViewModel::class.java]

        findViewById<TextView>(R.id.tvAppName).text = "Services for $vehicleRegistration"
        setupTabs()
        setupNavButtons(vehicleRegistration)
        setupPdfButton(vehicleRegistration)
    }

    private fun setupTabs() {
        val pager = findViewById<ViewPager2>(R.id.viewPager)
        pager.adapter = ServicePagerAdapter(this)

        TabLayoutMediator(
            findViewById(R.id.tabLayout), pager
        ) { tab, position ->
            tab.text = if (position == 0) "Timeline" else "Summary"
        }.attach()
    }

    private fun setupNavButtons(vehicleRegistration: String) {
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
    }

    private fun setupPdfButton(vehicleRegistration: String) {
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
        val dialog = AlertDialog.Builder(this).setMessage("Generating PDF...").setCancelable(false).create()
        dialog.show()
        lifecycleScope.launch {
            val records = viewModel.serviceRecords.value ?: emptyList()
            val (path, ok) = pdfGenerator.generateServiceRecordPdf(vehicleRegistration, records)
            dialog.dismiss()
            if (ok && path != null) shareFile(path, "application/pdf")
            else Toast.makeText(this@ServiceRecordActivity, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateCsv(vehicleRegistration: String) {
        val dialog = AlertDialog.Builder(this).setMessage("Generating CSV...").setCancelable(false).create()
        dialog.show()
        lifecycleScope.launch {
            val records = viewModel.serviceRecords.value ?: emptyList()
            val (path, ok) = pdfGenerator.generateServiceRecordCsv(vehicleRegistration, records)
            dialog.dismiss()
            if (ok && path != null) shareFile(path, "text/csv")
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

    private fun hasStoragePermissions(): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE &&
            grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            generatePdf(viewModel.vehicleRegistration)
        }
    }
}
