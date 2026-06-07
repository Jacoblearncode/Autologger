package com.nibm.autocare.model

data class ServiceRecord(
    val date: String,
    val odometerReading: String,
    val serviceCost: String,
    val serviceType: String? = null,
    val checkedItems: List<String>? = null,
    val notes: String? = null,
    val photoUrls: List<String>? = null,
    val recordId: String = ""
)
