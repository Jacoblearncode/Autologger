package com.nibm.autocare.model

data class Vehicle(
    val registrationNumber: String,
    val brand: String,
    val manufacturedYear: String,
    val model: String,
    val currentMileage: Int = 0,
    val weeklyRidingDistance: Int = 0,
    val photoUrl: String = "",
    val defaultImageUrl: String = "",
    val vehicleId: String = ""
)
