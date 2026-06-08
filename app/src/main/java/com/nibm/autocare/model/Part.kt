package com.nibm.autocare.model

/**
 * Represents a replaced part / component with optional warranty tracking.
 * Extracted from PartsWarrantyActivity so the ViewModel can reference it independently.
 */
data class Part(
    val id: String = "",
    val name: String = "",
    val installDate: String = "",
    val warrantyExpiry: String = "",
    val notes: String = ""
)
