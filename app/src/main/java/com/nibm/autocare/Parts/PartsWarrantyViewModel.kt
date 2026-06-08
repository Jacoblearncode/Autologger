package com.nibm.autocare.Parts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.model.Part

/**
 * ViewModel for the Parts & Warranty screen.
 *
 * Owns the Firebase listener for real-time updates. [savePart] handles both
 * add (null partId → push()) and update (non-null partId → setValue on existing node).
 */
class PartsWarrantyViewModel(
    private val userId: String,
    private val vehicleRegistration: String
) : ViewModel() {

    private val partsRef = FirebaseDatabase.getInstance().reference
        .child("users_parts").child(userId).child(vehicleRegistration)

    private val _parts = MutableLiveData<List<Part>>(emptyList())
    val parts: LiveData<List<Part>> = _parts

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = mutableListOf<Part>()
            for (child in snapshot.children) {
                val part = Part(
                    id = child.key ?: continue,
                    name = child.child("name").getValue(String::class.java) ?: continue,
                    installDate = child.child("installDate").getValue(String::class.java) ?: "",
                    warrantyExpiry = child.child("warrantyExpiry").getValue(String::class.java) ?: "",
                    notes = child.child("notes").getValue(String::class.java) ?: ""
                )
                list.add(part)
            }
            list.sortByDescending { it.installDate }
            _parts.value = list
        }

        override fun onCancelled(error: DatabaseError) {
            _toastMessage.value = "Failed to load parts"
        }
    }

    init {
        partsRef.addValueEventListener(listener)
    }

    /**
     * Saves a part. If [partId] is null a new node is created via push();
     * otherwise the existing node is updated in place.
     */
    fun savePart(partId: String?, data: Map<String, Any>) {
        val ref = if (partId != null) partsRef.child(partId) else partsRef.push()
        val msg = if (partId != null) "Part updated" else null
        ref.setValue(data)
            .addOnSuccessListener { if (msg != null) _toastMessage.value = msg }
            .addOnFailureListener { _toastMessage.value = "Failed to save part" }
    }

    fun deletePart(partId: String) {
        partsRef.child(partId).removeValue()
            .addOnFailureListener { _toastMessage.value = "Failed to delete part" }
    }

    fun clearToast() { _toastMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        partsRef.removeEventListener(listener)
    }
}

class PartsWarrantyViewModelFactory(
    private val userId: String,
    private val vehicleRegistration: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PartsWarrantyViewModel::class.java))
            return PartsWarrantyViewModel(userId, vehicleRegistration) as T
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
