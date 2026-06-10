package com.nibm.autocare

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.nibm.autocare.Authentication.LoginActivity
import com.nibm.autocare.Home.VehicleViewModel
import com.nibm.autocare.Home.VehicleViewModelFactory
import com.nibm.autocare.Vehicle.AddVehicleActivity
import com.nibm.autocare.adapter.VehicleAdapter
import com.nibm.autocare.model.Vehicle
import com.nibm.autocare.ServiceRecord.ServiceRecordActivity
import com.nibm.autocare.SettingsManager

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
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvOfflineBanner: TextView
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
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
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner)
        swipeRefresh = findViewById(R.id.swipeRefreshHome)
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent_lime))
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setupRecyclerView()
        setupSearch()

        viewModel = ViewModelProvider(this, VehicleViewModelFactory(userId))[VehicleViewModel::class.java]
        observeViewModel()

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

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

    override fun onResume() {
        super.onResume()
        vehicleAdapter.setDefaultVehicle(SettingsManager.getDefaultVehicle(this))
        tvOfflineBanner.visibility = if (isConnected()) View.GONE else View.VISIBLE
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { tvOfflineBanner.visibility = View.GONE }
            }
            override fun onLost(network: Network) {
                runOnUiThread { tvOfflineBanner.visibility = View.VISIBLE }
            }
        }
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            networkCallback
        )
    }

    override fun onPause() {
        super.onPause()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) {}
    }

    private fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setupRecyclerView() {
        rvVehicles = findViewById(R.id.rvVehicles)
        rvVehicles.layoutManager = LinearLayoutManager(this)
        vehicleAdapter = VehicleAdapter(
            onItemClick = { vehicle ->
                startActivity(Intent(this, ServiceRecordActivity::class.java).apply {
                    putExtra("vehicleRegistration", vehicle.registrationNumber)
                    putExtra("vehicleBrand", vehicle.brand)
                    putExtra("vehicleModel", vehicle.model)
                    putExtra("vehicleYear", vehicle.manufacturedYear)
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

        viewModel.isLoading.observe(this) { loading ->
            swipeRefresh.isRefreshing = loading
        }

        viewModel.lastServiceOdometers.observe(this) { scores ->
            vehicleAdapter.submitHealthScores(scores)
        }

        viewModel.vehicles.observe(this) { vehicles ->
            if (vehicles.isNotEmpty()) loadActivityFeed(userId)
        }
    }

    private fun loadActivityFeed(userId: String) {
        val card = findViewById<android.view.View>(R.id.cardRecentActivity)
        val ll = findViewById<android.widget.LinearLayout>(R.id.llActivityFeed)
        val db = com.google.firebase.database.FirebaseDatabase.getInstance()

        data class FeedItem(val label: String, val date: String)
        val items = mutableListOf<FeedItem>()

        db.reference.child("users_services").child(userId)
            .get().addOnSuccessListener { snap ->
                for (vehicleSnap in snap.children) {
                    val reg = vehicleSnap.key ?: continue
                    for (record in vehicleSnap.children) {
                        val date = record.child("date").getValue(String::class.java) ?: continue
                        val type = record.child("serviceType").getValue(String::class.java) ?: "Service"
                        items.add(FeedItem("🔧 $type — $reg", date))
                    }
                }
                db.reference.child("users_fuel_logs").child(userId)
                    .get().addOnSuccessListener { fuelSnap ->
                        for (entry in fuelSnap.children) {
                            val date = entry.child("date").getValue(String::class.java) ?: continue
                            val reg = entry.child("registrationNumber").getValue(String::class.java) ?: continue
                            items.add(FeedItem("⛽ Fuel log — $reg", date))
                        }
                        val recent = items.sortedByDescending { it.date }.take(5)
                        if (recent.isEmpty()) return@addOnSuccessListener
                        ll.removeAllViews()
                        recent.forEach { item ->
                            val row = android.widget.LinearLayout(this).apply {
                                orientation = android.widget.LinearLayout.HORIZONTAL
                                setPadding(0, 4, 0, 4)
                            }
                            val tvLabel = android.widget.TextView(this).apply {
                                text = item.label
                                textSize = 12f
                                setTextColor(androidx.core.content.ContextCompat.getColor(this@HomeActivity, R.color.text_primary))
                                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }
                            val tvDate = android.widget.TextView(this).apply {
                                text = item.date
                                textSize = 11f
                                setTextColor(androidx.core.content.ContextCompat.getColor(this@HomeActivity, R.color.text_secondary))
                            }
                            row.addView(tvLabel)
                            row.addView(tvDate)
                            ll.addView(row)
                        }
                        card.visibility = android.view.View.VISIBLE
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
