package com.nibm.autocare.ServiceRecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nibm.autocare.R

/**
 * Tab 2 — shows cost totals, efficiency, next service distance, and record count.
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
                    tvNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }
        }
    }

    private fun fmt(value: Double) = if (value == 0.0) "0" else "%,.0f".format(value)
}
