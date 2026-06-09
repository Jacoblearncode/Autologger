package com.nibm.autocare

import android.app.DatePickerDialog
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
import com.nibm.autocare.PdfGenerator
import com.nibm.autocare.TripLog.TripLogViewModel
import com.nibm.autocare.TripLog.TripLogViewModelFactory
import com.nibm.autocare.model.Trip
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Displays trips for a single vehicle and exposes add/edit/delete via dialogs.
 * TripLogViewModel owns the Firebase listener and all write operations.
 */
class TripLogActivity : AppCompatActivity() {

    private lateinit var viewModel: TripLogViewModel
    private lateinit var lvTrips: ListView
    private lateinit var tvTripCount: TextView
    private lateinit var tvTotalKm: TextView
    private lateinit var tvLongestTrip: TextView
    private lateinit var vehicleRegistration: String
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val pdfGenerator by lazy { PdfGenerator(this) }
    private var currentTrips: List<Trip> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_log)

        vehicleRegistration = intent.getStringExtra("vehicleRegistration") ?: ""
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        findViewById<TextView>(R.id.tvAppName).text = "Trips — $vehicleRegistration"

        lvTrips = findViewById(R.id.lvTrips)
        tvTripCount = findViewById(R.id.tvTripCount)
        tvTotalKm = findViewById(R.id.tvTotalKm)
        tvLongestTrip = findViewById(R.id.tvLongestTrip)
        lvTrips.setEmptyView(findViewById(R.id.emptyStateTrips))

        viewModel = ViewModelProvider(
            this, TripLogViewModelFactory(uid, vehicleRegistration)
        )[TripLogViewModel::class.java]

        observeViewModel()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddTrip).setOnClickListener { showTripDialog() }
        findViewById<View>(R.id.btnExportTrips).setOnClickListener { exportCsv() }
    }

    private fun observeViewModel() {
        viewModel.trips.observe(this) { trips ->
            currentTrips = trips
            lvTrips.adapter = TripsAdapter(trips)
            updateSummary(trips)
        }
        viewModel.toastMessage.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    private fun exportCsv() {
        if (currentTrips.isEmpty()) {
            Toast.makeText(this, "No trips to export", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = AlertDialog.Builder(this).setMessage("Generating CSV…").setCancelable(false).create().also { it.show() }
        lifecycleScope.launch {
            val (path, ok) = pdfGenerator.generateTripLogCsv(vehicleRegistration, currentTrips)
            dialog.dismiss()
            if (ok && path != null) {
                val file = File(path)
                val uri = FileProvider.getUriForFile(this@TripLogActivity, "${packageName}.provider", file)
                startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    },
                    "Share ${file.name}"
                ))
            } else {
                Toast.makeText(this@TripLogActivity, "Failed to generate CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSummary(trips: List<Trip>) {
        tvTripCount.text = "${trips.size}"
        tvTotalKm.text = "%.0f km".format(trips.sumOf { it.distance })
        val longest = trips.maxOfOrNull { it.distance }
        tvLongestTrip.text = if (longest != null) "%.0f km".format(longest) else "— km"
    }

    /**
     * Shows the add/edit dialog. Pass [existing] to pre-fill fields for editing;
     * leave null to show a blank add form.
     */
    private fun showTripDialog(existing: Trip? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_trip, null)
        val etPurpose = dialogView.findViewById<EditText>(R.id.etTripPurpose)
        val etDate = dialogView.findViewById<EditText>(R.id.etTripDate)
        val etStart = dialogView.findViewById<EditText>(R.id.etStartOdometer)
        val etEnd = dialogView.findViewById<EditText>(R.id.etEndOdometer)
        val etNotes = dialogView.findViewById<EditText>(R.id.etTripNotes)

        if (existing != null) {
            etPurpose.setText(existing.purpose)
            etDate.setText(existing.date)
            etStart.setText(existing.startOdometer)
            etEnd.setText(existing.endOdometer)
            etNotes.setText(existing.notes)
        }

        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val picked = Calendar.getInstance().apply { set(year, month, day) }
                    etDate.setText(dateFormat.format(picked.time))
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing != null) "Edit Trip" else "Log a Trip")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val startKm = etStart.text.toString().trim().toDoubleOrNull()
                val endKm = etEnd.text.toString().trim().toDoubleOrNull()
                if (startKm == null || endKm == null || endKm <= startKm) {
                    Toast.makeText(this, "End odometer must be greater than start", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val data = mapOf(
                    "date" to etDate.text.toString().trim(),
                    "purpose" to etPurpose.text.toString().trim().ifEmpty { "Trip" },
                    "startOdometer" to startKm.toString(),
                    "endOdometer" to endKm.toString(),
                    "distance" to (endKm - startKm),
                    "notes" to etNotes.text.toString().trim()
                )
                if (existing != null) viewModel.updateTrip(existing.id, data)
                else viewModel.addTrip(data)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class TripsAdapter(private val trips: List<Trip>) : BaseAdapter() {
        override fun getCount() = trips.size
        override fun getItem(pos: Int): Any = trips[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(parent?.context)
                .inflate(R.layout.list_item_trip, parent, false)

            val trip = trips[position]
            view.findViewById<TextView>(R.id.tvTripPurpose).text = trip.purpose.ifEmpty { "Trip" }
            view.findViewById<TextView>(R.id.tvTripDate).text = trip.date
            view.findViewById<TextView>(R.id.tvTripDistance).text = "%.0f km".format(trip.distance)
            view.findViewById<TextView>(R.id.tvStartOdometer).text = "${trip.startOdometer} km"
            view.findViewById<TextView>(R.id.tvEndOdometer).text = "${trip.endOdometer} km"

            val tvNotes = view.findViewById<TextView>(R.id.tvTripNotes)
            tvNotes.text = trip.notes
            tvNotes.visibility = if (trip.notes.isNotEmpty()) View.VISIBLE else View.GONE

            view.findViewById<View>(R.id.btnMapTrip).setOnClickListener {
                val uri = android.net.Uri.parse("geo:0,0?q=auto+workshop+near+me")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }

            view.findViewById<View>(R.id.btnEditTrip).setOnClickListener {
                showTripDialog(trip)
            }

            view.findViewById<View>(R.id.btnDeleteTrip).setOnClickListener {
                AlertDialog.Builder(this@TripLogActivity)
                    .setTitle("Delete Trip")
                    .setMessage("Remove this trip record?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteTrip(trip.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            return view
        }
    }
}
