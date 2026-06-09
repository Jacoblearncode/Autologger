package com.nibm.autocare.ServiceRecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
            view.findViewById<TextView>(R.id.tvSvcTotal).text = "MYR ${fmt(stats.svcTotal)}"
            view.findViewById<TextView>(R.id.tvFuelTotal).text = "MYR ${fmt(stats.fuelTotal)}"
            view.findViewById<TextView>(R.id.tvCombinedTotal).text = "MYR ${fmt(stats.combinedTotal)}"
            view.findViewById<TextView>(R.id.tvRecordCount).text = "${stats.recordCount}"
            view.findViewById<TextView>(R.id.tvAvgEfficiency).text = stats.avgEfficiency
            view.findViewById<TextView>(R.id.tvCostPerKm).text = stats.costPerKm

            val tvNext = view.findViewById<TextView>(R.id.tvNextService)
            when {
                stats.nextServiceKm < 0 -> {
                    tvNext.text = "—"
                    tvNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
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

            updatePieChart(pieChart, stats.svcTotal, stats.fuelTotal)
        }

        viewModel.monthlySpend.observe(viewLifecycleOwner) { data ->
            updateBarChart(barChart, data)
        }

        viewModel.efficiencyTrend.observe(viewLifecycleOwner) { data ->
            updateLineChart(lineChart, data)
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
