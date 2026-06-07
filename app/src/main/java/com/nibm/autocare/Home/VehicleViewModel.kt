package com.nibm.autocare.Home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.model.Vehicle
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VehicleViewModel(private val userId: String) : ViewModel() {

    private val database = FirebaseDatabase.getInstance()

    private val _vehicles = MutableLiveData<List<Vehicle>>(emptyList())
    val vehicles: LiveData<List<Vehicle>> = _vehicles

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val vehiclesRef: DatabaseReference =
        database.reference.child("users_vehicles").child(userId)

    private val vehiclesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = mutableListOf<Vehicle>()
            for (vehicleSnapshot in snapshot.children) {
                val registrationNumber = vehicleSnapshot.child("registrationNumber").getValue(String::class.java)
                val brand = vehicleSnapshot.child("brand").getValue(String::class.java)
                val manufacturedYear = vehicleSnapshot.child("manufacturedYear").getValue(String::class.java)
                val model = vehicleSnapshot.child("model").getValue(String::class.java)

                if (registrationNumber != null && brand != null && manufacturedYear != null && model != null) {
                    list.add(Vehicle(
                        registrationNumber,
                        brand,
                        manufacturedYear,
                        model,
                        vehicleSnapshot.child("currentMileage").getValue(Int::class.java) ?: 0,
                        vehicleSnapshot.child("weeklyRidingDistance").getValue(Int::class.java) ?: 0,
                        vehicleSnapshot.child("photoUrl").getValue(String::class.java) ?: "",
                        vehicleSnapshot.child("defaultImageUrl").getValue(String::class.java) ?: ""
                    ))
                }
            }
            _vehicles.value = list
        }

        override fun onCancelled(error: DatabaseError) {
            _toastMessage.value = "Failed to load vehicles: ${error.message}"
        }
    }

    init {
        vehiclesRef.addValueEventListener(vehiclesListener)
        fetchUsername()
    }

    private fun fetchUsername() {
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("username").getValue(String::class.java)
                    _username.value = if (name != null) "Hello, $name!" else "Hello, User"
                }

                override fun onCancelled(error: DatabaseError) {
                    _username.value = "Hello, User"
                }
            })
    }

    /**
     * Deletes a vehicle and its associated service records.
     * Uses viewModelScope + coroutines so the two-step delete runs
     * sequentially without nested callbacks.
     */
    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            val vehicleId = findVehicleId(vehicle.registrationNumber)
            if (vehicleId == null) {
                _toastMessage.value = "Vehicle not found"
                return@launch
            }
            database.reference.child("users_services").child(userId)
                .child(vehicle.registrationNumber).removeValue()
            database.reference.child("users_vehicles").child(userId)
                .child(vehicleId).removeValue()
            _toastMessage.value = "Vehicle deleted"
        }
    }

    private suspend fun findVehicleId(registrationNumber: String): String? =
        suspendCancellableCoroutine { cont ->
            database.reference.child("users_vehicles").child(userId)
                .orderByChild("registrationNumber").equalTo(registrationNumber)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        cont.resume(snapshot.children.firstOrNull()?.key)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        cont.resume(null)
                    }
                })
        }

    fun findVehicleIdForEdit(registrationNumber: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            callback(findVehicleId(registrationNumber))
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        vehiclesRef.removeEventListener(vehiclesListener)
    }
}

class VehicleViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehicleViewModel::class.java)) {
            return VehicleViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
