package com.nibm.autocare.ServiceRecord

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.model.ServiceRecord
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ViewModel for the Service Records screen, shared between ServicesFragment (Tab 1)
 * and StatsFragment (Tab 2) via the parent Activity's ViewModelStore.
 *
 * Maintains two independent data sources — service records (real-time) and fuel logs
 * (one-shot) — and combines them into a single CombinedStats value using MediatorLiveData.
 */
class ServiceViewModel(
    private val userId: String,
    val vehicleRegistration: String
) : ViewModel() {

    private val database = FirebaseDatabase.getInstance()

    // Private mutable / public immutable LiveData pattern keeps data flow unidirectional.
    private val _serviceRecords = MutableLiveData<List<ServiceRecord>>(emptyList())
    val serviceRecords: LiveData<List<ServiceRecord>> = _serviceRecords

    private val _fuelData = MutableLiveData<FuelData>(FuelData())
    val fuelData: LiveData<FuelData> = _fuelData

    /**
     * MediatorLiveData that recomputes combined spend and efficiency metrics whenever
     * either service records or fuel data changes. Both sources are observed internally
     * so observers of combinedStats don't need to watch two separate streams.
     */
    val combinedStats: LiveData<CombinedStats> = MediatorLiveData<CombinedStats>().apply {
        fun recompute() {
            val records = _serviceRecords.value ?: emptyList()
            val fuel = _fuelData.value ?: FuelData()
            value = computeStats(records, fuel)
        }
        addSource(_serviceRecords) { recompute() }
        addSource(_fuelData) { recompute() }
    }

    val monthlySpend: LiveData<List<MonthlySpend>> = MediatorLiveData<List<MonthlySpend>>().apply {
        fun recompute() {
            val records = _serviceRecords.value ?: emptyList()
            val fuel = _fuelData.value ?: FuelData()
            value = computeMonthlySpend(records, fuel)
        }
        addSource(_serviceRecords) { recompute() }
        addSource(_fuelData) { recompute() }
    }

    // Raw fuel data extracted from Firebase, used only by computeStats.
    data class FuelData(
        val totalCost: Double = 0.0,
        val maxOdometer: Double = 0.0,
        val minOdometer: Double = Double.MAX_VALUE,
        val efficiencyLogs: List<Pair<Double, Double>> = emptyList(),
        val monthlyFuelCost: Map<String, Double> = emptyMap()  // "MMM yy" -> total cost
    )

    // Monthly spend breakdown for the bar chart in StatsFragment.
    data class MonthlySpend(val label: String, val svcAmount: Float, val fuelAmount: Float)

    // Derived stats surfaced in StatsFragment. -1 for nextServiceKm means no data yet.
    data class CombinedStats(
        val svcTotal: Double = 0.0,
        val fuelTotal: Double = 0.0,
        val combinedTotal: Double = 0.0,
        val recordCount: Int = 0,
        val avgEfficiency: String = "—",
        val costPerKm: String = "—",
        val nextServiceKm: Double = -1.0,
        val lastServiceOdometer: Double = 0.0
    )

    private val servicesRef: DatabaseReference =
        database.reference.child("users_services").child(userId).child(vehicleRegistration)

    // Real-time listener — the list updates immediately when any record is added/deleted.
    private val servicesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = mutableListOf<ServiceRecord>()
            for (serviceSnapshot in snapshot.children) {
                parseServiceRecord(serviceSnapshot)?.let { list.add(it) }
            }
            // Newest records first so the timeline shows most-recent at the top.
            list.sortByDescending { it.date }
            _serviceRecords.value = list
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    init {
        servicesRef.addValueEventListener(servicesListener)
        fetchFuelData()
    }

    // One-shot read; fuel data is only needed for the summary card, not for real-time display.
    private fun fetchFuelData() {
        val inputFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val keyFmt = SimpleDateFormat("MMM yy", Locale.getDefault())

        database.reference.child("users_fuel_logs").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalCost = 0.0
                    var maxOdo = 0.0
                    var minOdo = Double.MAX_VALUE
                    val logs = mutableListOf<Pair<Double, Double>>()
                    val monthlyFuel = mutableMapOf<String, Double>()

                    for (child in snapshot.children) {
                        // Fuel logs are stored globally per user, so filter by this vehicle.
                        val reg = child.child("registrationNumber").getValue(String::class.java) ?: continue
                        if (reg != vehicleRegistration) continue

                        val cost = child.child("totalCost").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                        totalCost += cost

                        val odo = child.child("odometer").getValue(String::class.java)?.toDoubleOrNull()
                        val liters = child.child("liters").getValue(String::class.java)?.toDoubleOrNull()
                        if (odo != null) {
                            maxOdo = maxOf(maxOdo, odo)
                            minOdo = minOf(minOdo, odo)
                            if (liters != null && liters > 0) logs.add(odo to liters)
                        }

                        // Accumulate monthly fuel cost for the bar chart
                        val dateStr = child.child("date").getValue(String::class.java)
                        if (dateStr != null && cost > 0) {
                            try {
                                val key = keyFmt.format(inputFmt.parse(dateStr)!!)
                                monthlyFuel[key] = (monthlyFuel[key] ?: 0.0) + cost
                            } catch (_: Exception) {}
                        }
                    }
                    _fuelData.value = FuelData(totalCost, maxOdo, minOdo, logs, monthlyFuel)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun deleteServiceRecord(recordId: String) {
        servicesRef.child(recordId).removeValue()
    }

    // Returns null for malformed records rather than crashing the whole list load.
    private fun parseServiceRecord(snapshot: DataSnapshot): ServiceRecord? {
        return try {
            val date = snapshot.child("date").getValue(String::class.java) ?: return null
            val odometer = snapshot.child("odometerReading").getValue(String::class.java) ?: return null
            val cost = snapshot.child("serviceCost").getValue(String::class.java) ?: return null
            val serviceType = snapshot.child("serviceType").getValue(String::class.java)
            val checkedItems = snapshot.child("checkedItems").children.mapNotNull { it.getValue(String::class.java) }
            val notes = snapshot.child("notes").getValue(String::class.java)
            val photoUrls = snapshot.child("photoUrls").children.mapNotNull { it.getValue(String::class.java) }
            val recordId = snapshot.key ?: ""
            ServiceRecord(date, odometer, cost, serviceType, checkedItems, notes, photoUrls, recordId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes all summary statistics from the combined service and fuel datasets.
     *
     * Efficiency is calculated as km/L by comparing consecutive odometer readings —
     * a minimum of two fuel log entries is required for a meaningful result.
     * Next-service threshold is fixed at 5 000 km above the last recorded service odometer.
     */
    private fun computeStats(records: List<ServiceRecord>, fuel: FuelData): CombinedStats {
        val svcTotal = records.sumOf { it.serviceCost.toDoubleOrNull() ?: 0.0 }
        val recordCount = records.size

        val odoValues = records.mapNotNull { it.odometerReading.toDoubleOrNull() }
        val lastServiceOdo = if (odoValues.isNotEmpty()) odoValues.max() else 0.0
        val minSvcOdo = if (odoValues.isNotEmpty()) odoValues.min() else Double.MAX_VALUE

        // Use the earliest odometer value across both data sources as the start of the range.
        val minAllOdo = minOf(
            if (fuel.minOdometer == Double.MAX_VALUE) Double.MAX_VALUE else fuel.minOdometer,
            if (minSvcOdo == Double.MAX_VALUE) Double.MAX_VALUE else minSvcOdo
        )
        val currentEst = maxOf(fuel.maxOdometer, lastServiceOdo)
        val nextServiceKm = if (lastServiceOdo > 0) (lastServiceOdo + 5000) - currentEst else -1.0

        val kmRange = if (minAllOdo != Double.MAX_VALUE) currentEst - minAllOdo else 0.0
        val costPerKm = if (kmRange > 0) "MYR %.2f/km".format((svcTotal + fuel.totalCost) / kmRange) else "—"

        val avgEfficiency = if (fuel.efficiencyLogs.size >= 2) {
            val sorted = fuel.efficiencyLogs.sortedBy { it.first }
            var totalKm = 0.0; var totalLiters = 0.0
            for (i in 1 until sorted.size) {
                val kmDiff = sorted[i].first - sorted[i - 1].first
                if (kmDiff > 0) { totalKm += kmDiff; totalLiters += sorted[i].second }
            }
            if (totalLiters > 0) "%.1f km/L".format(totalKm / totalLiters) else "—"
        } else "—"

        return CombinedStats(
            svcTotal = svcTotal,
            fuelTotal = fuel.totalCost,
            combinedTotal = svcTotal + fuel.totalCost,
            recordCount = recordCount,
            avgEfficiency = avgEfficiency,
            costPerKm = costPerKm,
            nextServiceKm = nextServiceKm,
            lastServiceOdometer = lastServiceOdo
        )
    }

    private fun computeMonthlySpend(records: List<ServiceRecord>, fuel: FuelData): List<MonthlySpend> {
        val inputFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val keyFmt = SimpleDateFormat("MMM yy", Locale.getDefault())
        val sortFmt = SimpleDateFormat("MMM yy", Locale.getDefault())

        val svcByMonth = mutableMapOf<String, Double>()
        for (record in records) {
            try {
                val key = keyFmt.format(inputFmt.parse(record.date)!!)
                svcByMonth[key] = (svcByMonth[key] ?: 0.0) + (record.serviceCost.toDoubleOrNull() ?: 0.0)
            } catch (_: Exception) {}
        }

        val allKeys = (svcByMonth.keys + fuel.monthlyFuelCost.keys)
            .toSortedSet(Comparator { a, b ->
                (sortFmt.parse(a)?.time ?: 0L).compareTo(sortFmt.parse(b)?.time ?: 0L)
            })

        return allKeys.map { key ->
            MonthlySpend(
                label = key,
                svcAmount = (svcByMonth[key] ?: 0.0).toFloat(),
                fuelAmount = (fuel.monthlyFuelCost[key] ?: 0.0).toFloat()
            )
        }.takeLast(12)
    }

    override fun onCleared() {
        super.onCleared()
        // Remove Firebase listener to stop callbacks after the ViewModel is destroyed.
        servicesRef.removeEventListener(servicesListener)
    }
}

/**
 * Factory that passes userId and vehicleRegistration into ServiceViewModel.
 * Only the Activity calls this; child Fragments retrieve the existing instance
 * via ViewModelProvider(requireActivity()) without needing the factory again.
 */
class ServiceViewModelFactory(
    private val userId: String,
    private val vehicleRegistration: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServiceViewModel::class.java)) {
            return ServiceViewModel(userId, vehicleRegistration) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
