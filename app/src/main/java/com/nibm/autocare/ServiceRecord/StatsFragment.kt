package com.nibm.autocare.ServiceRecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.nibm.autocare.R
import com.nibm.autocare.SettingsManager

class StatsFragment : Fragment() {

    private lateinit var viewModel: ServiceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[ServiceViewModel::class.java]

        val barChart = view.findViewById<BarChart>(R.id.barChartMonthly)
        val lineChart = view.findViewById<LineChart>(R.id.lineChartEfficiency)
        val pieChart = view.findViewById<PieChart>(R.id.pieChartCosts)

        setupBarChart(barChart)
        setupLineChart(lineChart)
        setupPieChart(pieChart)

        viewModel.combinedStats.observe(viewLifecycleOwner) { stats ->
            applyStatsUI(stats, view)
            updatePieChart(pieChart, stats.svcTotal, stats.fuelTotal)
        }

        viewModel.monthlySpend.observe(viewLifecycleOwner) { data ->
            updateBarChart(barChart, data)
            updateAnnualCard(data, view)
            updateBudgetCard(data, view)
        }

        viewModel.efficiencyTrend.observe(viewLifecycleOwner) { data ->
            updateLineChart(lineChart, data)
        }
    }

    override fun onResume() {
        super.onResume()
        val v = requireView()
        viewModel.combinedStats.value?.let { applyStatsUI(it, v) }
        viewModel.monthlySpend.value?.let {
            updateAnnualCard(it, v)
            updateBudgetCard(it, v)
        }
    }

    private fun applyStatsUI(stats: ServiceViewModel.CombinedStats, view: View) {
        val cur = SettingsManager.getCurrency(requireContext())
        view.findViewById<TextView>(R.id.tvSvcTotal).text = "$cur ${fmt(stats.svcTotal)}"
        view.findViewById<TextView>(R.id.tvFuelTotal).text = "$cur ${fmt(stats.fuelTotal)}"
        view.findViewById<TextView>(R.id.tvCombinedTotal).text = "$cur ${fmt(stats.combinedTotal)}"
        view.findViewById<TextView>(R.id.tvRecordCount).text = "${stats.recordCount}"
        view.findViewById<TextView>(R.id.tvAvgEfficiency).text = stats.avgEfficiency
        view.findViewById<TextView>(R.id.tvCostPerKm).text =
            if (stats.costPerKm == "—") "—" else "$cur ${stats.costPerKm}"

        val tvNext = view.findViewById<TextView>(R.id.tvNextService)
        when {
            stats.nextServiceKm < 0 -> {
                tvNext.text = "—"
                tvNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            stats.nextServiceKm <= 0 -> {
                tvNext.text = "OVERDUE"
                tvNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            }
            stats.nextServiceKm <= 1000 -> {
                tvNext.text = "%.0f km".format(stats.nextServiceKm)
                tvNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_lime))
            }
            else -> {
                tvNext.text = "%.0f km".format(stats.nextServiceKm)
                tvNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }

        wireInfoButtons(view)
    }

    private fun wireInfoButtons(view: View) {
        view.findViewById<TextView>(R.id.infoSvcTotal).setOnClickListener {
            info(
                "Service Cost",
                "The total of all service record costs logged for this vehicle.\n\n" +
                "Tip: Add costs for every visit — oil changes, tyres, repairs — " +
                "to get an accurate picture of your maintenance spend."
            )
        }
        view.findViewById<TextView>(R.id.infoFuelTotal).setOnClickListener {
            info(
                "Fuel Cost",
                "The total amount spent on fuel across all entries in the Fuel Log for this vehicle.\n\n" +
                "Tip: Log every fill-up with the odometer reading to unlock efficiency tracking."
            )
        }
        view.findViewById<TextView>(R.id.infoCombinedTotal).setOnClickListener {
            info(
                "Total Spend",
                "Service costs + fuel costs combined — your all-time recorded spend on this vehicle.\n\n" +
                "Tip: Compare this figure month-over-month by checking the Fuel Log and service dates " +
                "to spot periods of higher spending."
            )
        }
        view.findViewById<TextView>(R.id.infoAvgEfficiency).setOnClickListener {
            info(
                "Average Fuel Efficiency",
                "Calculated from consecutive fuel log entries:\n" +
                "km travelled between fills ÷ litres added at each fill-up.\n\n" +
                "Requires at least 2 fuel log entries with odometer readings.\n\n" +
                "Tip: A declining efficiency number can signal a dirty air filter, " +
                "under-inflated tyres, or a need for a service."
            )
        }
        view.findViewById<TextView>(R.id.infoCostPerKm).setOnClickListener {
            info(
                "Cost per Kilometre",
                "Total spend (service + fuel) divided by the kilometres driven since your " +
                "earliest recorded odometer reading.\n\n" +
                "Tip: The lower this number, the cheaper each km costs to run. " +
                "A sudden rise often means a large unplanned repair — worth noting in your service records."
            )
        }
        view.findViewById<TextView>(R.id.infoNextService).setOnClickListener {
            info(
                "Next Service",
                "Estimated as 5,000 km above the odometer reading of your most recent service record.\n\n" +
                "• Lime colour — within 1,000 km, service soon\n" +
                "• Red / OVERDUE — you have passed the threshold\n\n" +
                "Tip: Update your service odometer reading each visit to keep this countdown accurate."
            )
        }
        view.findViewById<TextView>(R.id.infoRecordCount).setOnClickListener {
            info(
                "Service Records",
                "The total number of service records logged for this vehicle.\n\n" +
                "Tip: A complete history increases resale value — log every visit, " +
                "even minor ones like tyre rotations or fluid top-ups."
            )
        }
    }

    private fun info(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }

    // M4 — Annual spending breakdown
    private fun updateAnnualCard(data: List<ServiceViewModel.MonthlySpend>, view: View) {
        val ll = view.findViewById<LinearLayout>(R.id.llAnnualSpend)
        ll.removeAllViews()
        val cur = SettingsManager.getCurrency(requireContext())

        val sortFmt = SimpleDateFormat("MMM yy", Locale.getDefault())
        val yearlyTotals = data
            .groupBy { entry ->
                try { "20${sortFmt.parse(entry.label)?.let {
                    SimpleDateFormat("yy", Locale.getDefault()).format(it) } ?: "??"}" }
                catch (_: Exception) { "??" }
            }
            .map { (year, months) -> year to months.sumOf { (it.svcAmount + it.fuelAmount).toDouble() } }
            .sortedByDescending { it.first }

        if (yearlyTotals.isEmpty()) {
            val tv = TextView(requireContext()).apply { text = "No data yet"; setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary)) }
            ll.addView(tv)
            return
        }

        yearlyTotals.forEach { (year, total) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            val tvYear = TextView(requireContext()).apply {
                text = year
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvAmount = TextView(requireContext()).apply {
                text = "$cur ${fmt(total)}"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
            row.addView(tvYear)
            row.addView(tvAmount)
            ll.addView(row)
        }
    }

    // U5 — Monthly budget progress
    private fun updateBudgetCard(data: List<ServiceViewModel.MonthlySpend>, view: View) {
        val budget = SettingsManager.getMonthlyBudget(requireContext())
        val card = view.findViewById<View>(R.id.cardBudget)
        if (budget <= 0) { card.visibility = View.GONE; return }
        card.visibility = View.VISIBLE

        val currentLabel = SimpleDateFormat("MMM yy", Locale.getDefault()).format(Date())
        val spent = data.find { it.label == currentLabel }
            ?.let { (it.svcAmount + it.fuelAmount).toDouble() } ?: 0.0
        val cur = SettingsManager.getCurrency(requireContext())
        val pct = ((spent / budget) * 100).coerceIn(0.0, 100.0).toInt()

        view.findViewById<TextView>(R.id.tvBudgetStatus).text = "$cur ${fmt(spent)} / $cur ${fmt(budget)}"
        view.findViewById<ProgressBar>(R.id.pbBudget).apply {
            progress = pct
            progressTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (pct >= 90) R.color.red else R.color.accent_lime)
            )
        }
        view.findViewById<TextView>(R.id.tvBudgetNote).text = when {
            pct >= 100 -> "Budget exceeded this month"
            pct >= 80  -> "$pct% of budget used — approaching limit"
            else       -> "$pct% of budget used"
        }
    }

    // ── Bar chart ─────────────────────────────────────────────────────────────

    private fun setupBarChart(chart: BarChart) {
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        val gridColor = ContextCompat.getColor(requireContext(), R.color.divider_dark)
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            isDragEnabled = true
            setTouchEnabled(true)
            setNoDataText("No spending data yet")
            setNoDataTextColor(textColor)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                this.textColor = textColor
                textSize = 10f
                labelRotationAngle = -30f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                this.gridColor = gridColor
                this.textColor = textColor
                axisMinimum = 0f
                setLabelCount(4, true)
            }
            axisRight.isEnabled = false
        }
    }

    private fun updateBarChart(chart: BarChart, data: List<ServiceViewModel.MonthlySpend>) {
        if (data.isEmpty()) { chart.clear(); chart.invalidate(); return }

        val svcEntries = data.mapIndexed { i, m -> BarEntry(i.toFloat(), m.svcAmount) }
        val fuelEntries = data.mapIndexed { i, m -> BarEntry(i.toFloat(), m.fuelAmount) }
        val svcSet = BarDataSet(svcEntries, "Service").apply {
            color = ContextCompat.getColor(requireContext(), R.color.accent_lime)
            setDrawValues(false)
        }
        val fuelSet = BarDataSet(fuelEntries, "Fuel").apply {
            color = ContextCompat.getColor(requireContext(), R.color.yellow)
            setDrawValues(false)
        }

        val barWidth = 0.35f; val barSpace = 0.05f; val groupSpace = 0.20f
        val barData = BarData(svcSet, fuelSet).apply { this.barWidth = barWidth }
        chart.data = barData
        chart.groupBars(0f, groupSpace, barSpace)
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(data.map { it.label })
            setCenterAxisLabels(true)
            axisMinimum = 0f
            axisMaximum = barData.getGroupWidth(groupSpace, barSpace) * data.size
        }
        chart.setVisibleXRangeMaximum(barData.getGroupWidth(groupSpace, barSpace) * 6)
        chart.moveViewToX(barData.getGroupWidth(groupSpace, barSpace) * maxOf(0, data.size - 6).toFloat())
        chart.invalidate()
    }

    // ── Line chart (efficiency trend) ─────────────────────────────────────────

    private fun setupLineChart(chart: LineChart) {
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        val gridColor = ContextCompat.getColor(requireContext(), R.color.divider_dark)
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            isDragEnabled = true
            setNoDataText("Log at least 2 fuel fills to see trend")
            setNoDataTextColor(textColor)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                this.textColor = textColor
                textSize = 9f
                labelRotationAngle = -30f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                this.gridColor = gridColor
                this.textColor = textColor
                axisMinimum = 0f
                setLabelCount(4, true)
            }
            axisRight.isEnabled = false
        }
    }

    private fun updateLineChart(chart: LineChart, data: List<Pair<String, Float>>) {
        if (data.isEmpty()) { chart.clear(); chart.invalidate(); return }

        val entries = data.mapIndexed { i, p -> Entry(i.toFloat(), p.second) }
        val lime = ContextCompat.getColor(requireContext(), R.color.accent_lime)

        val dataSet = LineDataSet(entries, "km/L").apply {
            color = lime
            setCircleColor(lime)
            circleRadius = 4f
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.data = LineData(dataSet)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        chart.invalidate()
    }

    // ── Pie chart (cost breakdown) ─────────────────────────────────────────────

    private fun setupPieChart(chart: PieChart) {
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = textColor
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            setNoDataText("No cost data yet")
            setNoDataTextColor(textColor)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setHoleColor(android.graphics.Color.TRANSPARENT)
        }
    }

    private fun updatePieChart(chart: PieChart, svcTotal: Double, fuelTotal: Double) {
        if (svcTotal + fuelTotal <= 0) { chart.clear(); chart.invalidate(); return }

        val entries = mutableListOf<PieEntry>()
        if (svcTotal > 0) entries.add(PieEntry(svcTotal.toFloat(), "Service"))
        if (fuelTotal > 0) entries.add(PieEntry(fuelTotal.toFloat(), "Fuel"))

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.accent_lime),
                ContextCompat.getColor(requireContext(), R.color.yellow)
            )
            sliceSpace = 2f
            selectionShift = 5f
        }

        chart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(chart))
            setValueTextSize(11f)
            setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        chart.invalidate()
    }

    private fun fmt(value: Double) = if (value == 0.0) "0" else "%,.0f".format(value)
}
