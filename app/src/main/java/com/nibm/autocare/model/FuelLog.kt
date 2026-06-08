package com.nibm.autocare.model

/**
 * Represents a single fuel fill-up entry.
 * Extracted from FuelLogActivity so ViewModel and PdfGenerator
 * can both reference it without circular dependency.
 *
 * [efficiency] is computed by FuelLogViewModel after loading all logs;
 * it is not stored in Firebase.
 */
data class FuelLog(
    val id: String = "",
    val registrationNumber: String = "",
    val date: String = "",
    val odometer: String = "",
    val liters: String = "",
    val pricePerLiter: String = "",
    val totalCost: String = "",
    val fuelType: String = "",
    val notes: String = "",
    var efficiency: String = ""
)
