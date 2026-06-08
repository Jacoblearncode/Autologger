package com.nibm.autocare.TripLog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.model.Trip

/**
 * ViewModel for the Trip Log screen.
 *
 * Owns the Firebase listener so trips update in real time regardless of
 * Activity lifecycle. Both add and update write to the same node structure;
 * update reuses the existing key, add creates a new one via push().
 */
class TripLogViewModel(
    private val userId: String,
    private val vehicleRegistration: String
) : ViewModel() {

    private val tripsRef = FirebaseDatabase.getInstance().reference
        .child("users_trips").child(userId).child(vehicleRegistration)

    private val _trips = MutableLiveData<List<Trip>>(emptyList())
    val trips: LiveData<List<Trip>> = _trips

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = mutableListOf<Trip>()
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
                list.add(trip)
            }
            list.sortByDescending { it.date }
            _trips.value = list
        }

        override fun onCancelled(error: DatabaseError) {
            _toastMessage.value = "Failed to load trips"
        }
    }

    init {
        tripsRef.addValueEventListener(listener)
    }

    fun addTrip(data: Map<String, Any>) {
        tripsRef.push().setValue(data)
            .addOnFailureListener { _toastMessage.value = "Failed to save trip" }
    }

    /** Overwrites all fields on the existing node without changing its key. */
    fun updateTrip(tripId: String, data: Map<String, Any>) {
        tripsRef.child(tripId).setValue(data)
            .addOnSuccessListener { _toastMessage.value = "Trip updated" }
            .addOnFailureListener { _toastMessage.value = "Failed to update trip" }
    }

    fun deleteTrip(tripId: String) {
        tripsRef.child(tripId).removeValue()
            .addOnFailureListener { _toastMessage.value = "Failed to delete trip" }
    }

    fun clearToast() { _toastMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        tripsRef.removeEventListener(listener)
    }
}

class TripLogViewModelFactory(
    private val userId: String,
    private val vehicleRegistration: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripLogViewModel::class.java))
            return TripLogViewModel(userId, vehicleRegistration) as T
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
