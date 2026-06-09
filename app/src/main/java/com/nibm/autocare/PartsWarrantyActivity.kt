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
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.nibm.autocare.Parts.PartsWarrantyViewModel
import com.nibm.autocare.Parts.PartsWarrantyViewModelFactory
import com.nibm.autocare.Reminder.ReminderScheduler
import com.nibm.autocare.model.Part
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Displays replaced parts/components for a vehicle with warranty expiry tracking.
 * PartsWarrantyViewModel owns the Firebase listener and write operations.
 */
class PartsWarrantyActivity : AppCompatActivity() {

    private lateinit var viewModel: PartsWarrantyViewModel
    private lateinit var lvParts: ListView
    private lateinit var vehicleRegistration: String
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parts_warranty)

        vehicleRegistration = intent.getStringExtra("vehicleRegistration") ?: ""
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        findViewById<TextView>(R.id.tvAppName).text = "Parts — $vehicleRegistration"

        lvParts = findViewById(R.id.lvParts)
        lvParts.setEmptyView(findViewById(R.id.emptyStateParts))

        viewModel = ViewModelProvider(
            this, PartsWarrantyViewModelFactory(uid, vehicleRegistration)
        )[PartsWarrantyViewModel::class.java]

        observeViewModel()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddPart).setOnClickListener { showPartDialog() }
    }

    private fun observeViewModel() {
        viewModel.parts.observe(this) { parts ->
            lvParts.adapter = PartsAdapter(parts)
        }
        viewModel.toastMessage.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    /**
     * Shows the add/edit dialog. Pass [existing] to pre-fill fields for editing;
     * leave null to show a blank add form.
     */
    private fun showPartDialog(existing: Part? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_part, null)
        val etPartName = dialogView.findViewById<EditText>(R.id.etPartName)
        val etInstallDate = dialogView.findViewById<EditText>(R.id.etInstallDate)
        val etWarrantyExpiry = dialogView.findViewById<EditText>(R.id.etWarrantyExpiry)
        val etNotes = dialogView.findViewById<EditText>(R.id.etPartNotes)

        if (existing != null) {
            etPartName.setText(existing.name)
            etInstallDate.setText(existing.installDate)
            etWarrantyExpiry.setText(existing.warrantyExpiry)
            etNotes.setText(existing.notes)
        }

        etInstallDate.setOnClickListener { showDatePickerFor(etInstallDate) }
        etWarrantyExpiry.setOnClickListener { showDatePickerFor(etWarrantyExpiry) }

        AlertDialog.Builder(this)
            .setTitle(if (existing != null) "Edit Part" else "Add Replaced Part")
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
                // Null partId → push() new node; non-null → update existing
                viewModel.savePart(existing?.id, data)
                val expiry = etWarrantyExpiry.text.toString().trim()
                if (expiry.isNotEmpty()) {
                    ReminderScheduler.scheduleWarrantyReminder(
                        this@PartsWarrantyActivity, vehicleRegistration, name, expiry
                    )
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
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
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
            val days = TimeUnit.MILLISECONDS.toDays(expiry.time - Calendar.getInstance().time.time)
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

    inner class PartsAdapter(private val parts: List<Part>) : BaseAdapter() {
        override fun getCount() = parts.size
        override fun getItem(pos: Int): Any = parts[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(parent?.context)
                .inflate(R.layout.list_item_part, parent, false)

            val part = parts[position]
            view.findViewById<TextView>(R.id.tvPartName).text = part.name
            view.findViewById<TextView>(R.id.tvInstallDate).text =
                if (part.installDate.isNotEmpty()) "Installed: ${part.installDate}" else "Install date not set"

            val tvNotes = view.findViewById<TextView>(R.id.tvPartNotes)
            tvNotes.text = part.notes
            tvNotes.visibility = if (part.notes.isNotEmpty()) View.VISIBLE else View.GONE

            updateWarrantyViews(
                part.warrantyExpiry,
                view.findViewById(R.id.tvWarrantyExpiry),
                view.findViewById(R.id.tvWarrantyCountdown)
            )

            view.findViewById<View>(R.id.btnEditPart).setOnClickListener {
                showPartDialog(part)
            }

            view.findViewById<View>(R.id.btnDeletePart).setOnClickListener {
                AlertDialog.Builder(this@PartsWarrantyActivity)
                    .setTitle("Delete Part")
                    .setMessage("Remove \"${part.name}\" from tracking?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deletePart(part.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            return view
        }
    }
}
