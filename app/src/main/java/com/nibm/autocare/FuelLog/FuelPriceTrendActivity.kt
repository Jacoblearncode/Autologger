package com.nibm.autocare.FuelLog

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nibm.autocare.R
import java.text.SimpleDateFormat
import java.util.*

class FuelPriceTrendActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var spinnerVehicle: Spinner
    private lateinit var tvNoData: TextView
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    data class LogEntry(val date: String, val registrationNumber: String, val pricePerLiter: Double)

    private val allLogs = mutableListOf<LogEntry>()
    private var logsListener: ValueEventListener? = null
    private val fuelLogsRef by lazy {
        database.reference.child("users_fuel_logs").child(auth.currentUser?.uid ?: "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuel_price_trend)

        lineChart = findViewById(R.id.lineChart)
        spinnerVehicle = findViewById(R.id.spinnerChartVehicle)
        tvNoData = findViewById(R.id.tvNoData)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        setupChart()
    }

    override fun onStart() {
        super.onStart()
        attachListener()
    }

    override fun onStop() {
        super.onStop()
        logsListener?.let { fuelLogsRef.removeEventListener(it) }
        logsListener = null
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled = true
            setNoDataText("No fuel price data available")

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -30f
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 11f
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
        }
    }

    private fun attachListener() {
        logsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allLogs.clear()
                for (child in snapshot.children) {
                    val reg = child.child("registrationNumber").getValue(String::class.java) ?: continue
                    val date = child.child("date").getValue(String::class.java) ?: continue
                    val price = child.child("pricePerLiter").getValue(String::class.java)
                        ?.toDoubleOrNull() ?: continue
                    if (price > 0) allLogs.add(LogEntry(date, reg, price))
                }
                allLogs.sortBy { it.date }
                updateSpinner()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FuelPriceTrendActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
        fuelLogsRef.addValueEventListener(logsListener!!)
    }

    private fun updateSpinner() {
        val vehicles = mutableListOf("All Vehicles")
        vehicles.addAll(allLogs.map { it.registrationNumber }.distinct().sorted())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehicle.adapter = adapter

        spinnerVehicle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val selected = vehicles[pos]
                val filtered = if (selected == "All Vehicles") allLogs
                else allLogs.filter { it.registrationNumber == selected }
                updateChart(filtered)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateChart(allLogs)
    }

    private fun updateChart(logs: List<LogEntry>) {
        if (logs.isEmpty()) {
            lineChart.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
            return
        }

        lineChart.visibility = View.VISIBLE
        tvNoData.visibility = View.GONE

        val byVehicle = logs.groupBy { it.registrationNumber }
        val dataSets = mutableListOf<LineDataSet>()
        val allDates = logs.map { it.date }.distinct().sorted()
        val colors = listOf(
            android.graphics.Color.parseColor("#FFD700"),
            android.graphics.Color.parseColor("#4CAF50"),
            android.graphics.Color.parseColor("#2196F3"),
            android.graphics.Color.parseColor("#F44336"),
            android.graphics.Color.parseColor("#FF9800")
        )

        byVehicle.entries.forEachIndexed { index, (reg, vehicleLogs) ->
            val entries = vehicleLogs.mapIndexed { _, log ->
                val xIndex = allDates.indexOf(log.date).toFloat()
                Entry(xIndex, log.pricePerLiter.toFloat())
            }.sortedBy { it.x }

            val dataSet = LineDataSet(entries, reg).apply {
                val color = colors[index % colors.size]
                this.color = color
                setCircleColor(color)
                lineWidth = 2.5f
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }
            dataSets.add(dataSet)
        }

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(allDates.map { shortenDate(it) })
        lineChart.xAxis.labelCount = minOf(allDates.size, 6)
        lineChart.data = LineData(dataSets.toList())
        lineChart.invalidate()
    }

    private fun shortenDate(date: String): String {
        return try {
            val parsed = dateFormat.parse(date) ?: return date
            SimpleDateFormat("MMM yy", Locale.getDefault()).format(parsed)
        } catch (e: Exception) {
            date
        }
    }
}
