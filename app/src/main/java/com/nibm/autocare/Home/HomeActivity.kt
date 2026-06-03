package com.nibm.autocare

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.Authentication.LoginActivity
import com.nibm.autocare.Vehicle.AddVehicleActivity

class HomeActivity : AppCompatActivity() {

    private val vehicleList = mutableListOf<Vehicle>()
    private lateinit var tvGreeting: TextView
    private lateinit var lvVehicles: ListView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var etSearch: EditText
    private lateinit var originalVehicleList: MutableList<Vehicle>
    private var isSearchActive = false

    private var vehiclesListener: ValueEventListener? = null
    private var vehiclesDbRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase and views
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        tvGreeting = findViewById(R.id.tvGreeting)
        lvVehicles = findViewById(R.id.lvEmails)
        etSearch = findViewById(R.id.etSearch)
        originalVehicleList = mutableListOf()

        setupSearch()
        fetchUsername()

        // Set up click listeners
        findViewById<View>(R.id.ivMenu).setOnClickListener { showMenu(it) }
        findViewById<View>(R.id.llAddVehicle).setOnClickListener {
            startActivity(Intent(this, AddVehicleActivity::class.java))
        }
        findViewById<View>(R.id.llAddService).setOnClickListener {
            startActivity(Intent(this, AddServiceActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            attachVehiclesListener()
        }
    }

    override fun onStop() {
        super.onStop()
        vehiclesListener?.let { vehiclesDbRef?.removeEventListener(it) }
        vehiclesListener = null
    }

    private fun setupSearch() {
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    if (isSearchActive) {
                        vehicleList.clear()
                        vehicleList.addAll(originalVehicleList)
                        (lvVehicles.adapter as? BaseAdapter)?.notifyDataSetChanged()
                        isSearchActive = false
                    }
                } else {
                    performSearch()
                }
            }
        })
    }

    private fun performSearch() {
        val query = etSearch.text.toString().trim().lowercase()
        if (query.isEmpty()) return

        isSearchActive = true
        val filteredList = originalVehicleList.filter {
            it.registrationNumber.lowercase().contains(query)
        }

        vehicleList.clear()
        vehicleList.addAll(filteredList)
        (lvVehicles.adapter as? BaseAdapter)?.notifyDataSetChanged()

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No vehicles found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(vehicle: Vehicle) {
        AlertDialog.Builder(this)
            .setTitle("Delete Vehicle")
            .setMessage("Delete ${vehicle.registrationNumber} and all its service records?")
            .setPositiveButton("Delete") { _, _ ->
                deleteVehicle(vehicle)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteVehicle(vehicle: Vehicle) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val userVehiclesRef = database.reference.child("users_vehicles").child(userId)
        val query: Query = userVehiclesRef.orderByChild("registrationNumber").equalTo(vehicle.registrationNumber)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@HomeActivity, "Vehicle not found", Toast.LENGTH_SHORT).show()
                    return
                }

                for (vehicleSnapshot in snapshot.children) {
                    val vehicleId = vehicleSnapshot.key ?: continue
                    deleteVehicleAndServices(userId, vehicleId, vehicle.registrationNumber)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Failed to find vehicle: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteVehicleAndServices(userId: String, vehicleId: String, registrationNumber: String) {
        val vehicleRef = database.reference.child("users_vehicles").child(userId).child(vehicleId)
        val servicesRef = database.reference.child("users_services").child(userId).child(registrationNumber)

        servicesRef.removeValue()
            .addOnSuccessListener {
                vehicleRef.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@HomeActivity,
                            "Vehicle and all service records deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@HomeActivity,
                            "Vehicle deleted but services may remain: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@HomeActivity,
                    "Failed to delete service records: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchUsername() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userRef = database.reference.child("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java)
                    if (username != null) {
                        tvGreeting.text = "Hello, $username!"
                    } else {
                        tvGreeting.text = "Hello, User"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Failed to fetch username", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun attachVehiclesListener() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        vehiclesDbRef = database.reference.child("users_vehicles").child(userId)
        vehiclesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                originalVehicleList.clear()
                vehicleList.clear()

                for (vehicleSnapshot in snapshot.children) {
                    val registrationNumber = vehicleSnapshot.child("registrationNumber").getValue(String::class.java)
                    val brand = vehicleSnapshot.child("brand").getValue(String::class.java)
                    val manufacturedYear = vehicleSnapshot.child("manufacturedYear").getValue(String::class.java)
                    val model = vehicleSnapshot.child("model").getValue(String::class.java)

                    if (registrationNumber != null && brand != null && manufacturedYear != null && model != null) {
                        val vehicle = Vehicle(
                            registrationNumber,
                            brand,
                            manufacturedYear,
                            model,
                            vehicleSnapshot.child("currentMileage").getValue(Int::class.java) ?: 0,
                            vehicleSnapshot.child("weeklyRidingDistance").getValue(Int::class.java) ?: 0
                        )
                        originalVehicleList.add(vehicle)
                    }
                }

                if (!isSearchActive) {
                    vehicleList.clear()
                    vehicleList.addAll(originalVehicleList)
                }

                val adapter = VehicleAdapter(vehicleList)
                lvVehicles.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Failed to fetch vehicles: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        vehiclesDbRef?.addValueEventListener(vehiclesListener!!)
    }

    private fun getVehicleId(registrationNumber: String, callback: (String?) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            callback(null)
            return
        }

        database.reference.child("users_vehicles").child(currentUser.uid)
            .orderByChild("registrationNumber").equalTo(registrationNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (vehicleSnapshot in snapshot.children) {
                            callback(vehicleSnapshot.key)
                            return
                        }
                    }
                    callback(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    private fun showMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.menu_home)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_logout -> {
                    logout()
                    true
                }
                R.id.menu_delete_account -> {
                    deleteAccount()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "No user is currently logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val pathsToDelete = mapOf(
            "users" to database.reference.child("users").child(userId),
            "users_services" to database.reference.child("users_services").child(userId),
            "users_vehicles" to database.reference.child("users_vehicles").child(userId)
        )

        val deleteTasks = pathsToDelete.map { (_, ref) -> ref.removeValue() }

        com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
            .addOnSuccessListener {
                currentUser.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account and all data deleted successfully", Toast.LENGTH_SHORT).show()
                        logout()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                            "Account data deleted but failed to remove authentication: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Failed to delete user data: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    data class Vehicle(
        val registrationNumber: String,
        val brand: String,
        val manufacturedYear: String,
        val model: String,
        val currentMileage: Int = 0,
        val weeklyRidingDistance: Int = 0
    )

    inner class VehicleAdapter(private val vehicleList: List<Vehicle>) : BaseAdapter() {
        override fun getCount(): Int = vehicleList.size
        override fun getItem(position: Int): Any = vehicleList[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val viewHolder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_vehicle, parent, false)
                viewHolder = ViewHolder(view)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            val vehicle = vehicleList[position]
            viewHolder.tvRegistrationNumber.text = vehicle.registrationNumber
            viewHolder.tvBrand.text = vehicle.brand
            viewHolder.tvManufacturedYear.text = vehicle.manufacturedYear
            viewHolder.tvModel.text = vehicle.model

            viewHolder.btnEdit.setOnClickListener {
                // Get the vehicle ID from Firebase using the registration number
                getVehicleId(vehicle.registrationNumber) { vehicleId ->
                    if (vehicleId != null) {
                        val intent = Intent(this@HomeActivity, AddVehicleActivity::class.java).apply {
                            putExtra("vehicleId", vehicleId)
                            putExtra("registrationNumber", vehicle.registrationNumber)
                            putExtra("brand", vehicle.brand)
                            putExtra("model", vehicle.model)
                            putExtra("manufacturedYear", vehicle.manufacturedYear)
                            putExtra("currentMileage", vehicle.currentMileage.toString())
                            putExtra("weeklyRidingDistance", vehicle.weeklyRidingDistance.toString())
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@HomeActivity, "Could not find vehicle ID", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            view.setOnClickListener {
                val intent = Intent(this@HomeActivity, ServiceRecordActivity::class.java).apply {
                    putExtra("vehicleRegistration", vehicle.registrationNumber)
                }
                startActivity(intent)
            }

            view.setOnLongClickListener {
                showDeleteConfirmationDialog(vehicle)
                true
            }

            return view
        }

        private inner class ViewHolder(view: View) {
            val tvRegistrationNumber: TextView = view.findViewById(R.id.tvRegistrationNumber)
            val tvBrand: TextView = view.findViewById(R.id.tvBrand)
            val tvManufacturedYear: TextView = view.findViewById(R.id.tvManufacturedYear)
            val tvModel: TextView = view.findViewById(R.id.tvModel)
            val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        }
    }
}