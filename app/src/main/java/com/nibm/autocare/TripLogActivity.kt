package com.nibm.autocare

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TripLogActivity : AppCompatActivity() {

    private lateinit var lvTrips: ListView
    private lateinit var tvTripCount: TextView
    private lateinit var tvTotalKm: TextView
    private lateinit var tvLongestTrip: TextView
    private lateinit var vehicleRegistration: String
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var tripsRef: DatabaseReference
    private val tripsList = mutableListOf<Trip>()
    private var tripsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_log)

        vehicleRegistration = intent.getStringExtra("vehicleRegistration") ?: ""
        findViewById<TextView>(R.id.tvAppName).text = "Trips — $vehicleRegistration"

        lvTrips = findViewById(R.id.lvTrips)
        tvTripCount = findViewById(R.id.tvTripCount)
        tvTotalKm = findViewById(R.id.tvTotalKm)
        tvLongestTrip = findViewById(R.id.tvLongestTrip)
        lvTrips.setEmptyView(findViewById(R.id.emptyStateTrips))

        val uid = auth.currentUser?.uid ?: return
        tripsRef = database.reference
            .child("users_trips")
            .child(uid)
            .child(vehicleRegistration)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddTrip).setOnClickListener { showAddTripDialog() }
    }

    override fun onStart() {
        super.onStart()
        attachListener()
    }

    override fun onStop() {
        super.onStop()
        tripsListener?.let { tripsRef.removeEventListener(it) }
        tripsListener = null
    }

    private fun attachListener() {
        tripsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripsList.clear()
                for (child in snapshot.children) {
                    val trip = Trip(
                        id = child.key ?: continue,
                        date = child.child("date").getValue(String::class.java) ?: "",
                        purpose = child.child("purpose").getValue(String::class.java) ?: "",
                        startOdometer = child.child("startOdometer").getValue(String::class.java) ?: "",
                        endOdometer = child.child("endOdometer").getValue(String::class.java) ?: "",
                        distance = child.child("distance").getValue(Double::class.java) ?: 0.0,
                        notes = child.child("notes").getValue(String::class.java) ?: ""
                    )
                    tripsList.add(trip)
                }
                tripsList.sortByDescending { it.date }
                lvTrips.adapter = TripsAdapter()
                updateSummary()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TripLogActivity, "Failed to load trips", Toast.LENGTH_SHORT).show()
            }
        }
        tripsRef.addValueEventListener(tripsListener!!)
    }

    private fun updateSummary() {
        tvTripCount.text = "${tripsList.size}"
        val totalKm = tripsList.sumOf { it.distance }
        tvTotalKm.text = "%.0f km".format(totalKm)
        val longest = tripsList.maxOfOrNull { it.distance }
        tvLongestTrip.text = if (longest != null) "%.0f km".format(longest) else "— km"
    }

    private fun showAddTripDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_trip, null)
        val etPurpose = dialogView.findViewById<EditText>(R.id.etTripPurpose)
        val etDate = dialogView.findViewById<EditText>(R.id.etTripDate)
        val etStart = dialogView.findViewById<EditText>(R.id.etStartOdometer)
        val etEnd = dialogView.findViewById<EditText>(R.id.etEndOdometer)
        val etNotes = dialogView.findViewById<EditText>(R.id.etTripNotes)

        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val picked = Calendar.getInstance().apply { set(year, month, day) }
                    etDate.setText(dateFormat.format(picked.time))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Log a Trip")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val startKm = etStart.text.toString().trim().toDoubleOrNull()
                val endKm = etEnd.text.toString().trim().toDoubleOrNull()
                if (startKm == null || endKm == null || endKm <= startKm) {
                    Toast.makeText(this, "End odometer must be greater than start", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val distance = endKm - startKm
                val data = mapOf(
                    "date" to etDate.text.toString().trim(),
                    "purpose" to etPurpose.text.toString().trim().ifEmpty { "Trip" },
                    "startOdometer" to startKm.toString(),
                    "endOdometer" to endKm.toString(),
                    "distance" to distance,
                    "notes" to etNotes.text.toString().trim()
                )
                tripsRef.push().setValue(data)
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to save trip", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class TripsAdapter : BaseAdapter() {
        override fun getCount() = tripsList.size
        override fun getItem(pos: Int): Any = tripsList[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(parent?.context)
                .inflate(R.layout.list_item_trip, parent, false)

            val trip = tripsList[position]
            view.findViewById<TextView>(R.id.tvTripPurpose).text =
                trip.purpose.ifEmpty { "Trip" }
            view.findViewById<TextView>(R.id.tvTripDate).text = trip.date
            view.findViewById<TextView>(R.id.tvTripDistance).text =
                "%.0f km".format(trip.distance)
            view.findViewById<TextView>(R.id.tvStartOdometer).text =
                "${trip.startOdometer} km"
            view.findViewById<TextView>(R.id.tvEndOdometer).text =
                "${trip.endOdometer} km"

            val tvNotes = view.findViewById<TextView>(R.id.tvTripNotes)
            if (trip.notes.isNotEmpty()) {
                tvNotes.text = trip.notes
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            view.findViewById<View>(R.id.btnDeleteTrip).setOnClickListener {
                AlertDialog.Builder(this@TripLogActivity)
                    .setTitle("Delete Trip")
                    .setMessage("Remove this trip record?")
                    .setPositiveButton("Delete") { _, _ ->
                        tripsRef.child(trip.id).removeValue()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            return view
        }
    }

    data class Trip(
        val id: String,
        val date: String,
        val purpose: String,
        val startOdometer: String,
        val endOdometer: String,
        val distance: Double,
        val notes: String
    )
}
