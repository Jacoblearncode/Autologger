package com.nibm.autocare

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PartsWarrantyActivity : AppCompatActivity() {

    private lateinit var lvParts: ListView
    private lateinit var vehicleRegistration: String
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var partsRef: DatabaseReference
    private val partsList = mutableListOf<Part>()
    private var partsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parts_warranty)

        vehicleRegistration = intent.getStringExtra("vehicleRegistration") ?: ""
        findViewById<TextView>(R.id.tvAppName).text = "Parts — $vehicleRegistration"

        lvParts = findViewById(R.id.lvParts)
        lvParts.setEmptyView(findViewById(R.id.emptyStateParts))

        val uid = auth.currentUser?.uid ?: return
        partsRef = database.reference
            .child("users_parts")
            .child(uid)
            .child(vehicleRegistration)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddPart).setOnClickListener { showAddPartDialog() }
    }

    override fun onStart() {
        super.onStart()
        attachListener()
    }

    override fun onStop() {
        super.onStop()
        partsListener?.let { partsRef.removeEventListener(it) }
        partsListener = null
    }

    private fun attachListener() {
        partsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                partsList.clear()
                for (child in snapshot.children) {
                    val part = Part(
                        id = child.key ?: continue,
                        name = child.child("name").getValue(String::class.java) ?: continue,
                        installDate = child.child("installDate").getValue(String::class.java) ?: "",
                        warrantyExpiry = child.child("warrantyExpiry").getValue(String::class.java) ?: "",
                        notes = child.child("notes").getValue(String::class.java) ?: ""
                    )
                    partsList.add(part)
                }
                partsList.sortByDescending { it.installDate }
                lvParts.adapter = PartsAdapter()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PartsWarrantyActivity, "Failed to load parts", Toast.LENGTH_SHORT).show()
            }
        }
        partsRef.addValueEventListener(partsListener!!)
    }

    private fun showAddPartDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_part, null)
        val etPartName = dialogView.findViewById<EditText>(R.id.etPartName)
        val etInstallDate = dialogView.findViewById<EditText>(R.id.etInstallDate)
        val etWarrantyExpiry = dialogView.findViewById<EditText>(R.id.etWarrantyExpiry)
        val etNotes = dialogView.findViewById<EditText>(R.id.etPartNotes)

        etInstallDate.setOnClickListener { showDatePickerFor(etInstallDate) }
        etWarrantyExpiry.setOnClickListener { showDatePickerFor(etWarrantyExpiry) }

        AlertDialog.Builder(this)
            .setTitle("Add Replaced Part")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etPartName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Part name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val data = mapOf(
                    "name" to name,
                    "installDate" to etInstallDate.text.toString().trim(),
                    "warrantyExpiry" to etWarrantyExpiry.text.toString().trim(),
                    "notes" to etNotes.text.toString().trim()
                )
                partsRef.push().setValue(data)
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to save part", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePickerFor(et: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                et.setText(dateFormat.format(picked.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateWarrantyViews(warrantyExpiry: String, tvExpiry: TextView, tvCountdown: TextView) {
        if (warrantyExpiry.isEmpty()) {
            tvExpiry.text = "No warranty date set"
            tvExpiry.setTextColor(ContextCompat.getColor(this, R.color.gray))
            tvCountdown.text = ""
            return
        }

        tvExpiry.text = warrantyExpiry
        try {
            val expiry = dateFormat.parse(warrantyExpiry) ?: return
            val today = Calendar.getInstance().time
            val days = TimeUnit.MILLISECONDS.toDays(expiry.time - today.time)
            when {
                days < 0 -> {
                    tvExpiry.setTextColor(ContextCompat.getColor(this, R.color.red))
                    tvCountdown.text = "Expired ${-days} day${if (-days == 1L) "" else "s"} ago"
                    tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.red))
                }
                days <= 30 -> {
                    tvExpiry.setTextColor(ContextCompat.getColor(this, R.color.dark_yellow))
                    tvCountdown.text = "Expires in $days day${if (days == 1L) "" else "s"}"
                    tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.dark_yellow))
                }
                else -> {
                    tvExpiry.setTextColor(ContextCompat.getColor(this, R.color.green))
                    tvCountdown.text = "$days days left"
                    tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.green))
                }
            }
        } catch (e: Exception) {
            tvExpiry.setTextColor(ContextCompat.getColor(this, R.color.gray))
            tvCountdown.text = ""
        }
    }

    inner class PartsAdapter : BaseAdapter() {
        override fun getCount() = partsList.size
        override fun getItem(pos: Int): Any = partsList[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(parent?.context)
                .inflate(R.layout.list_item_part, parent, false)

            val part = partsList[position]
            view.findViewById<TextView>(R.id.tvPartName).text = part.name
            view.findViewById<TextView>(R.id.tvInstallDate).text =
                if (part.installDate.isNotEmpty()) "Installed: ${part.installDate}" else "Install date not set"

            val tvNotes = view.findViewById<TextView>(R.id.tvPartNotes)
            if (part.notes.isNotEmpty()) {
                tvNotes.text = part.notes
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            updateWarrantyViews(
                part.warrantyExpiry,
                view.findViewById(R.id.tvWarrantyExpiry),
                view.findViewById(R.id.tvWarrantyCountdown)
            )

            view.findViewById<View>(R.id.btnDeletePart).setOnClickListener {
                AlertDialog.Builder(this@PartsWarrantyActivity)
                    .setTitle("Delete Part")
                    .setMessage("Remove \"${part.name}\" from tracking?")
                    .setPositiveButton("Delete") { _, _ ->
                        partsRef.child(part.id).removeValue()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            return view
        }
    }

    data class Part(
        val id: String,
        val name: String,
        val installDate: String,
        val warrantyExpiry: String,
        val notes: String
    )
}
