package com.nibm.autocare.FuelLog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.model.FuelLog

/**
 * ViewModel for the Fuel Log screen.
 *
 * Owns the Firebase ValueEventListener so the list stays in sync in real time
 * without the Activity managing connection lifecycle directly.
 * Efficiency (km/L) is computed here after each data load so the Activity
 * receives a ready-to-display list.
 */
class FuelLogViewModel(private val userId: String) : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val logsRef = database.reference.child("users_fuel_logs").child(userId)

    // Private mutable / public read-only LiveData pattern
    private val _fuelLogs = MutableLiveData<List<FuelLog>>(emptyList())
    val fuelLogs: LiveData<List<FuelLog>> = _fuelLogs

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Continuous listener — fires whenever any log is added, edited, or deleted
    private val logsListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = mutableListOf<FuelLog>()
            for (snap in snapshot.children) {
                parseFuelLog(snap)?.let { list.add(it) }
            }
            list.sortByDescending { it.date }
            calculateEfficiency(list)
            _fuelLogs.value = list
        }

        override fun onCancelled(error: DatabaseError) {
            _toastMessage.value = "Failed to load fuel logs"
        }
    }

    init {
        logsRef.addValueEventListener(logsListener)
    }

    fun refresh() {
        _isLoading.value = true
        logsRef.get().addOnCompleteListener { _isLoading.postValue(false) }
    }

    fun deleteFuelLog(logId: String) {
        logsRef.child(logId).removeValue()
            .addOnSuccessListener { _toastMessage.value = "Fuel log deleted" }
            .addOnFailureListener { _toastMessage.value = "Failed to delete" }
    }

    fun clearToast() { _toastMessage.value = null }

    private fun parseFuelLog(snap: DataSnapshot): FuelLog? {
        return try {
            FuelLog(
                id = snap.key ?: return null,
                registrationNumber = snap.child("registrationNumber").getValue(String::class.java) ?: "",
                date = snap.child("date").getValue(String::class.java) ?: "",
                odometer = snap.child("odometer").getValue(String::class.java) ?: "",
                liters = snap.child("liters").getValue(String::class.java) ?: "",
                pricePerLiter = snap.child("pricePerLiter").getValue(String::class.java) ?: "",
                totalCost = snap.child("totalCost").getValue(String::class.java) ?: "",
                fuelType = snap.child("fuelType").getValue(String::class.java) ?: "",
                notes = snap.child("notes").getValue(String::class.java) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes fuel efficiency (km/L) by comparing consecutive odometer readings
     * per vehicle. The first log for each vehicle has no previous reading so its
     * efficiency stays blank.
     */
    private fun calculateEfficiency(logs: List<FuelLog>) {
        val byVehicle = logs.groupBy { it.registrationNumber }
        for ((_, vehicleLogs) in byVehicle) {
            val sorted = vehicleLogs.sortedBy { it.odometer.toDoubleOrNull() ?: 0.0 }
            for (i in 1 until sorted.size) {
                val curr = sorted[i]
                val prev = sorted[i - 1]
                val currOdo = curr.odometer.toDoubleOrNull() ?: continue
                val prevOdo = prev.odometer.toDoubleOrNull() ?: continue
                val liters = curr.liters.toDoubleOrNull() ?: continue
                if (liters > 0 && currOdo > prevOdo) {
                    curr.efficiency = String.format("%.1f km/L", (currOdo - prevOdo) / liters)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        logsRef.removeEventListener(logsListener)
    }
}

/**
 * Factory required because FuelLogViewModel takes a userId constructor parameter.
 */
class FuelLogViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FuelLogViewModel::class.java)) {
            return FuelLogViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
