package com.nibm.autocare

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.nibm.autocare.Authentication.LoginActivity
import com.nibm.autocare.Home.VehicleViewModel
import com.nibm.autocare.Home.VehicleViewModelFactory
import com.nibm.autocare.Vehicle.AddVehicleActivity
import com.nibm.autocare.adapter.VehicleAdapter
import com.nibm.autocare.model.Vehicle
import com.nibm.autocare.ServiceRecord.ServiceRecordActivity

/**
 * Main screen of the app, displaying the user's vehicle list.
 *
 * Follows MVVM: VehicleViewModel owns all Firebase data and business logic;
 * HomeActivity only binds UI views to LiveData and forwards user actions to the ViewModel.
 * Search filters the already-loaded list locally — no Firebase call on every keystroke.
 */
class HomeActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 2001
    }

    private lateinit var viewModel: VehicleViewModel
    private lateinit var tvGreeting: TextView
    private lateinit var rvVehicles: RecyclerView
    private lateinit var vehicleAdapter: VehicleAdapter
    private lateinit var etSearch: EditText
    private lateinit var emptyState: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        tvGreeting = findViewById(R.id.tvGreeting)
        etSearch = findViewById(R.id.etSearch)
        emptyState = findViewById(R.id.emptyStateVehicles)
        tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = emptyState.findViewById(R.id.tvEmptySubtitle)

        setupRecyclerView()
        setupSearch()

        viewModel = ViewModelProvider(this, VehicleViewModelFactory(userId))[VehicleViewModel::class.java]
        observeViewModel()

        requestNotificationPermissionIfNeeded()

        findViewById<View>(R.id.ivMenu).setOnClickListener { showMenu(it) }
        findViewById<View>(R.id.llAddVehicle).setOnClickListener {
            startActivity(Intent(this, AddVehicleActivity::class.java))
        }
        findViewById<View>(R.id.llAddService).setOnClickListener {
            startActivity(Intent(this, AddServiceActivity::class.java))
        }
        findViewById<View>(R.id.llFuelLog).setOnClickListener {
            startActivity(Intent(this, FuelLogActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        rvVehicles = findViewById(R.id.rvVehicles)
        rvVehicles.layoutManager = LinearLayoutManager(this)
        vehicleAdapter = VehicleAdapter(
            onItemClick = { vehicle ->
                startActivity(Intent(this, ServiceRecordActivity::class.java).apply {
                    putExtra("vehicleRegistration", vehicle.registrationNumber)
                })
            },
            onItemLongClick = { vehicle -> confirmDelete(vehicle) },
            onEditClick = { vehicle ->
                viewModel.findVehicleIdForEdit(vehicle.registrationNumber) { vehicleId ->
                    if (vehicleId != null) {
                        startActivity(Intent(this, AddVehicleActivity::class.java).apply {
                            putExtra("vehicleId", vehicleId)
                            putExtra("registrationNumber", vehicle.registrationNumber)
                            putExtra("brand", vehicle.brand)
                            putExtra("model", vehicle.model)
                            putExtra("manufacturedYear", vehicle.manufacturedYear)
                            putExtra("currentMileage", vehicle.currentMileage.toString())
                            putExtra("weeklyRidingDistance", vehicle.weeklyRidingDistance.toString())
                        })
                    } else {
                        Toast.makeText(this, "Could not find vehicle ID", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        rvVehicles.adapter = vehicleAdapter
    }

    private fun observeViewModel() {
        viewModel.username.observe(this) { greeting ->
            tvGreeting.text = greeting
        }

        viewModel.vehicles.observe(this) { allVehicles ->
            val filtered = if (searchQuery.isEmpty()) allVehicles
            else allVehicles.filter { it.registrationNumber.lowercase().contains(searchQuery) }
            vehicleAdapter.submitList(filtered)
            updateEmptyState(filtered, searchQuery.isNotEmpty())
        }

        viewModel.toastMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    private fun updateEmptyState(list: List<Vehicle>, isSearching: Boolean) {
        if (list.isEmpty()) {
            rvVehicles.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            if (isSearching) {
                tvEmptyTitle.text = "No results found"
                tvEmptySubtitle.text = "Try a different search term"
            } else {
                tvEmptyTitle.text = "No vehicles yet"
                tvEmptySubtitle.text = "Tap the Vehicles button below to add your first vehicle"
            }
        } else {
            rvVehicles.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun setupSearch() {
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            actionId == EditorInfo.IME_ACTION_SEARCH
        }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim()?.lowercase() ?: ""
                // Re-filter current list through the observer
                val current = viewModel.vehicles.value ?: emptyList()
                val filtered = if (searchQuery.isEmpty()) current
                else current.filter { it.registrationNumber.lowercase().contains(searchQuery) }
                vehicleAdapter.submitList(filtered)
                updateEmptyState(filtered, searchQuery.isNotEmpty())
            }
        })
    }

    private fun confirmDelete(vehicle: Vehicle) {
        AlertDialog.Builder(this)
            .setTitle("Delete Vehicle")
            .setMessage("Delete ${vehicle.registrationNumber} and all its service records?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteVehicle(vehicle) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun showMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_home)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                R.id.menu_logout -> { logout(); true }
                R.id.menu_delete_account -> { deleteAccount(); true }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun deleteAccount() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val db = com.google.firebase.database.FirebaseDatabase.getInstance()
        val uid = currentUser.uid
        val deleteTasks = listOf(
            db.reference.child("users").child(uid).removeValue(),
            db.reference.child("users_services").child(uid).removeValue(),
            db.reference.child("users_vehicles").child(uid).removeValue()
        )
        com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
            .addOnSuccessListener {
                currentUser.delete().addOnSuccessListener {
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show()
                    logout()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete account: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
