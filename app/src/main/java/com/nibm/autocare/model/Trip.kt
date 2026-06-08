package com.nibm.autocare.model

/**
 * Represents a single trip entry. Extracted from TripLogActivity
 * so TripLogViewModel can reference it independently.
 */
data class Trip(
    val id: String = "",
    val date: String = "",
    val purpose: String = "",
    val startOdometer: String = "",
    val endOdometer: String = "",
    val distance: Double = 0.0,
    val notes: String = ""
)
