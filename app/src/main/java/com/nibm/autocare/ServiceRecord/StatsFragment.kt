package com.nibm.autocare.ServiceRecord

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.nibm.autocare.R

/**
 * Tab 2 — shows cost totals, efficiency, next service distance, record count,
 * and a grouped monthly spending bar chart (service vs fuel per month).
 * Uses MediatorLiveData from ServiceViewModel so it reacts to both service and
 * fuel data changes automatically.
 */
class StatsFragment : Fragment() {

    private lateinit var viewModel: ServiceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Shared ViewModel — instance was created by the Activity with full factory parameters.
        viewModel = ViewModelProvider(requireActivity())[ServiceViewModel::class.java]

        val chart = view.findViewById<BarChart>(R.id.barChartMonthly)
        setupChart(chart)

        // combinedStats is a MediatorLiveData that fires whenever service records or fuel
        // data changes, so the summary updates automatically without any manual refresh.
        viewModel.combinedStats.observe(viewLifecycleOwner) { stats ->
            view.findViewById<TextView>(R.id.tvSvcTotal).text = "MYR ${fmt(stats.svcTotal)}"
            view.findViewById<TextView>(R.id.tvFuelTotal).text = "MYR ${fmt(stats.fuelTotal)}"
            view.findViewById<TextView>(R.id.tvCombinedTotal).text = "MYR ${fmt(stats.combinedTotal)}"
            view.findViewById<TextView>(R.id.tvRecordCount).text = "${stats.recordCount}"
            view.findViewById<TextView>(R.id.tvAvgEfficiency).text = stats.avgEfficiency
            view.findViewById<TextView>(R.id.tvCostPerKm).text = stats.costPerKm

            // Colour-code next service distance: red = overdue, lime = within 1 000 km, white = normal.
            val tvNext = view.findViewById<TextView>(R.id.tvNextService)
            when {
                stats.nextServiceKm < 0 -> {
                    tvNext.text = "—"
                    tvNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
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
        }

        viewModel.monthlySpend.observe(viewLifecycleOwner) { data ->
            updateChart(chart, data)
        }
    }

    private fun setupChart(chart: BarChart) {
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

    private fun updateChart(chart: BarChart, data: List<ServiceViewModel.MonthlySpend>) {
        if (data.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

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

        val barWidth = 0.35f
        val barSpace = 0.05f
        val groupSpace = 0.20f

        val barData = BarData(svcSet, fuelSet).apply { this.barWidth = barWidth }
        chart.data = barData
        chart.groupBars(0f, groupSpace, barSpace)

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(data.map { it.label })
            setCenterAxisLabels(true)
            axisMinimum = 0f
            axisMaximum = barData.getGroupWidth(groupSpace, barSpace) * data.size
        }

        // Scroll to show most recent months; cap visible range at 6 groups
        chart.setVisibleXRangeMaximum(barData.getGroupWidth(groupSpace, barSpace) * 6)
        val scrollTo = barData.getGroupWidth(groupSpace, barSpace) * maxOf(0, data.size - 6).toFloat()
        chart.moveViewToX(scrollTo)

        chart.invalidate()
    }

    private fun fmt(value: Double) = if (value == 0.0) "0" else "%,.0f".format(value)
}
