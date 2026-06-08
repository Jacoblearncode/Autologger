package com.nibm.autocare.ServiceRecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nibm.autocare.R
import com.nibm.autocare.adapter.ServiceRecordAdapter

/**
 * Tab 1 — displays the scrollable service record timeline.
 * Shares ServiceViewModel with its parent Activity; no separate data fetch needed.
 */
class ServicesFragment : Fragment() {

    private lateinit var viewModel: ServiceViewModel
    private lateinit var serviceAdapter: ServiceRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_services, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the ViewModel already created by the host Activity.
        // No factory is needed here because the instance already exists in the Activity's store.
        viewModel = ViewModelProvider(requireActivity())[ServiceViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvServiceRecords)
        rv.layoutManager = LinearLayoutManager(requireContext())

        serviceAdapter = ServiceRecordAdapter(
            onDeleteClick = { recordId -> confirmDelete(recordId) }
        )
        rv.adapter = serviceAdapter

        // viewLifecycleOwner is used instead of 'this' (the Fragment) so the observer
        // is automatically removed when the Fragment's view is destroyed during tab switches,
        // preventing memory leaks and stale callbacks.
        viewModel.serviceRecords.observe(viewLifecycleOwner) { records ->
            serviceAdapter.submitList(records)
            // RecyclerView has no built-in empty state, so visibility is toggled manually.
            val empty = records.isEmpty()
            rv.visibility = if (empty) View.GONE else View.VISIBLE
            view.findViewById<View>(R.id.emptyStateServices).visibility =
                if (empty) View.VISIBLE else View.GONE
        }
    }

    private fun confirmDelete(recordId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Service Record")
            .setMessage("Are you sure you want to delete this service record?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteServiceRecord(recordId) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
