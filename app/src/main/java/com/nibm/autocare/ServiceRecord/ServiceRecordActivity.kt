package com.nibm.autocare

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.nibm.autocare.Vehicle.AddVehicleActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ServiceRecordActivity : AppCompatActivity() {

    private lateinit var lvServiceRecords: ListView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var vehicleRegistration: String
    private lateinit var pdfGenerator: PdfGenerator
    private lateinit var servicesRef: DatabaseReference
    private var currentServiceRecords = mutableListOf<ServiceRecord>()
    private var servicesListener: ValueEventListener? = null

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        private const val DELETE_CONFIRMATION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_record)

        initializeComponents()
        setupClickListeners()
        fetchServiceRecords()
    }

    private fun initializeComponents() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        pdfGenerator = PdfGenerator(this)
        vehicleRegistration = intent.getStringExtra("vehicleRegistration") ?: ""
        findViewById<TextView>(R.id.tvAppName).text = "Services for $vehicleRegistration"
        lvServiceRecords = findViewById(R.id.lvServiceRecords)

        val currentUser = auth.currentUser
        servicesRef = database.reference.child("users_services").child(currentUser?.uid ?: "").child(vehicleRegistration)
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.llHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        findViewById<View>(R.id.llAddVehicle).setOnClickListener {
            startActivity(Intent(this, AddVehicleActivity::class.java))
        }

        findViewById<View>(R.id.llAddService).setOnClickListener {
            startActivity(Intent(this, AddServiceActivity::class.java))
        }

        findViewById<View>(R.id.btnDownloadPdf).setOnClickListener {
            if (currentServiceRecords.isEmpty()) {
                showToast("No service records to export")
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || hasStoragePermissions()) {
                generateAndDownloadPdf()
            } else {
                requestStoragePermissions()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        servicesListener?.let { servicesRef.removeEventListener(it) }
        servicesListener = null
    }

    override fun onStart() {
        super.onStart()
        fetchServiceRecords()
    }

    private fun fetchServiceRecords() {
        servicesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentServiceRecords.clear()
                for (serviceSnapshot in snapshot.children) {
                    parseServiceRecord(serviceSnapshot)?.let {
                        currentServiceRecords.add(it)
                    }
                }
                currentServiceRecords.sortByDescending { it.date }
                lvServiceRecords.adapter = ServiceRecordAdapter(currentServiceRecords, this@ServiceRecordActivity)
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to fetch services: ${error.message}")
            }
        }
        servicesRef.addValueEventListener(servicesListener!!)
    }

    private fun parseServiceRecord(serviceSnapshot: DataSnapshot): ServiceRecord? {
        return try {
            val date = serviceSnapshot.child("date").getValue(String::class.java) ?: return null
            val odometerReading = serviceSnapshot.child("odometerReading").getValue(String::class.java) ?: return null
            val serviceCost = serviceSnapshot.child("serviceCost").getValue(String::class.java) ?: return null
            val serviceType = serviceSnapshot.child("serviceType").getValue(String::class.java)
            val checkedItems = serviceSnapshot.child("checkedItems").children.mapNotNull { it.getValue(String::class.java) }
            val notes = serviceSnapshot.child("notes").getValue(String::class.java)
            val photoUrls = serviceSnapshot.child("photoUrls").children.mapNotNull { it.getValue(String::class.java) }
            val recordId = serviceSnapshot.key ?: ""

            ServiceRecord(date, odometerReading, serviceCost, serviceType, checkedItems, notes, photoUrls, recordId)
        } catch (e: Exception) {
            null
        }
    }

    private fun deleteServiceRecord(recordId: String) {
        servicesRef.child(recordId).removeValue()
            .addOnSuccessListener {
                showToast("Service record deleted successfully")
            }
            .addOnFailureListener {
                showToast("Failed to delete service record")
            }
    }

    private fun showDeleteConfirmation(recordId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Service Record")
            .setMessage("Are you sure you want to delete this service record?")
            .setPositiveButton("Delete") { _, _ ->
                deleteServiceRecord(recordId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateAndDownloadPdf() {
        val progressDialog = AlertDialog.Builder(this)
            .setMessage("Generating PDF...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            val (filePath, success) = pdfGenerator.generateServiceRecordPdf(vehicleRegistration, currentServiceRecords)
            progressDialog.dismiss()
            if (success && filePath != null) {
                sharePdfFile(filePath)
            } else {
                Toast.makeText(this@ServiceRecordActivity, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sharePdfFile(filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        startActivity(Intent.createChooser(shareIntent, "Share Service Records"))

        Toast.makeText(
            this,
            "PDF saved to ${file.parentFile?.name ?: "app storage"} folder",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun hasStoragePermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                generateAndDownloadPdf()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showPermissionExplanation()
                } else {
                    Toast.makeText(
                        this,
                        "Permission denied. You can enable it in app settings.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage("This permission is required to save PDF files to your device")
            .setPositiveButton("Grant") { _, _ ->
                requestStoragePermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    inner class ServiceRecordAdapter(
        private val serviceList: List<ServiceRecord>,
        private val context: Context
    ) : BaseAdapter() {
        private val expandedPositions = mutableSetOf<Int>()
        private val imageSize = 250.dpToPx()

        override fun getCount(): Int = serviceList.size
        override fun getItem(position: Int): Any = serviceList[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val viewHolder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(parent?.context)
                    .inflate(R.layout.list_item_service_record, parent, false)
                viewHolder = ViewHolder(view)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
                viewHolder.imageContainer.removeAllViews()
            }

            val service = serviceList[position]
            setupBasicInfo(viewHolder, service)
            setupExpandedDetails(viewHolder, service)
            setupImages(viewHolder, service)
            setupDeleteButton(viewHolder, service)
            setupExpandCollapse(view, viewHolder, position)

            return view
        }

        private fun setupBasicInfo(viewHolder: ViewHolder, service: ServiceRecord) {
            viewHolder.tvOdometerReading.text = "${service.odometerReading} km"
            viewHolder.tvServiceDate.text = service.date
            viewHolder.tvServiceCost.text = "Rs ${service.serviceCost}"
        }

        private fun setupExpandedDetails(viewHolder: ViewHolder, service: ServiceRecord) {
            service.serviceType?.let {
                viewHolder.tvServiceType.text = "Service: $it"
                viewHolder.tvServiceType.visibility = View.VISIBLE
            } ?: run { viewHolder.tvServiceType.visibility = View.GONE }

            service.checkedItems?.let {
                viewHolder.tvCheckedItems.text = "Services:\n${it.joinToString("\n• ", "• ")}"
                viewHolder.tvCheckedItems.visibility = View.VISIBLE
            } ?: run { viewHolder.tvCheckedItems.visibility = View.GONE }

            service.notes?.let {
                viewHolder.tvNotes.text = "Notes: $it"
                viewHolder.tvNotes.visibility = View.VISIBLE
            } ?: run { viewHolder.tvNotes.visibility = View.GONE }
        }

        private fun setupImages(viewHolder: ViewHolder, service: ServiceRecord) {
            service.photoUrls?.takeIf { it.isNotEmpty() }?.forEach { url ->
                val imageView = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
                        marginEnd = 8.dpToPx()
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = true
                    clipToOutline = true
                    background = ContextCompat.getDrawable(context, R.drawable.image_border)
                }

                Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(imageView)

                imageView.setOnClickListener { showFullImageDialog(url) }
                viewHolder.imageContainer.addView(imageView)
            }
        }

        private fun setupDeleteButton(viewHolder: ViewHolder, service: ServiceRecord) {
            viewHolder.btnDelete.setOnClickListener {
                showDeleteConfirmation(service.recordId)
            }
        }

        private fun setupExpandCollapse(view: View, viewHolder: ViewHolder, position: Int) {
            viewHolder.llExpandedDetails.visibility =
                if (expandedPositions.contains(position)) View.VISIBLE else View.GONE

            view.setOnClickListener {
                if (expandedPositions.contains(position)) {
                    expandedPositions.remove(position)
                } else {
                    expandedPositions.add(position)
                }
                notifyDataSetChanged()
            }
        }

        private fun showFullImageDialog(imageUrl: String) {
            Dialog(context).apply {
                setContentView(R.layout.dialog_full_image)
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                findViewById<ImageView>(R.id.ivFullImage).let { imageView ->
                    Glide.with(context)
                        .load(imageUrl)
                        .into(imageView)
                }
                findViewById<View>(R.id.btnClose).setOnClickListener { dismiss() }
                show()
            }
        }

        private inner class ViewHolder(view: View) {
            val tvOdometerReading: TextView = view.findViewById(R.id.tvOdometerReading)
            val tvServiceDate: TextView = view.findViewById(R.id.tvServiceDate)
            val tvServiceCost: TextView = view.findViewById(R.id.tvServiceCost)
            val tvServiceType: TextView = view.findViewById(R.id.tvServiceType)
            val tvCheckedItems: TextView = view.findViewById(R.id.tvCheckedItems)
            val tvNotes: TextView = view.findViewById(R.id.tvNotes)
            val llExpandedDetails: LinearLayout = view.findViewById(R.id.llExpandedDetails)
            val imageContainer: LinearLayout = view.findViewById(R.id.imageContainer)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        }

        private fun Int.dpToPx(): Int = (this * context.resources.displayMetrics.density).toInt()
    }

    data class ServiceRecord(
        val date: String,
        val odometerReading: String,
        val serviceCost: String,
        val serviceType: String? = null,
        val checkedItems: List<String>? = null,
        val notes: String? = null,
        val photoUrls: List<String>? = null,
        val recordId: String = ""
    )

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}