package com.nibm.autocare.ServiceRecord

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.nibm.autocare.AddServiceActivity
import com.nibm.autocare.R
import com.nibm.autocare.adapter.ServiceRecordAdapter
import com.nibm.autocare.model.ServiceRecord

/**
 * Tab 1 — displays the scrollable service record timeline.
 * Shares ServiceViewModel with its parent Activity; no separate data fetch needed.
 */
class ServicesFragment : Fragment() {

    private lateinit var viewModel: ServiceViewModel
    private lateinit var serviceAdapter: ServiceRecordAdapter
    private var allRecords: List<ServiceRecord> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_services, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[ServiceViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvServiceRecords)
        rv.layoutManager = LinearLayoutManager(requireContext())

        serviceAdapter = ServiceRecordAdapter(
            onEditClick = { record -> launchEditActivity(record) },
            onDeleteClick = { recordId -> confirmDelete(recordId) }
        )
        rv.adapter = serviceAdapter

        // Swipe-to-delete with undo snackbar
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val displayedList = serviceAdapter.currentList
                if (pos < 0 || pos >= displayedList.size) return
                val record = displayedList[pos]
                viewModel.deleteServiceRecord(record.recordId)
                Snackbar.make(requireView(), "Service record deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") { viewModel.restoreServiceRecord(record) }
                    .show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rv)

        // Search field
        val etSearch = view.findViewById<EditText>(R.id.etSearchServices)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString() ?: "", rv, view)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.serviceRecords.observe(viewLifecycleOwner) { records ->
            allRecords = records
            applyFilter(etSearch.text.toString(), rv, view)
        }
    }

    private fun applyFilter(query: String, rv: RecyclerView, view: View) {
        val filtered = if (query.isBlank()) allRecords
        else {
            val q = query.trim().lowercase()
            allRecords.filter { r ->
                r.serviceType?.lowercase()?.contains(q) == true ||
                r.notes?.lowercase()?.contains(q) == true ||
                r.date.contains(q)
            }
        }
        serviceAdapter.submitList(filtered)
        val empty = filtered.isEmpty()
        rv.visibility = if (empty) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.emptyStateServices).visibility =
            if (empty) View.VISIBLE else View.GONE
    }

    private fun launchEditActivity(record: ServiceRecord) {
        val intent = Intent(requireContext(), AddServiceActivity::class.java).apply {
            putExtra("isEditMode", true)
            putExtra("recordId", record.recordId)
            putExtra("vehicleRegistration", viewModel.vehicleRegistration)
            putExtra("date", record.date)
            putExtra("odometerReading", record.odometerReading)
            putExtra("serviceCost", record.serviceCost)
            putExtra("serviceType", record.serviceType ?: "")
            putExtra("notes", record.notes ?: "")
            putStringArrayListExtra("checkedItems", ArrayList(record.checkedItems ?: emptyList()))
            putStringArrayListExtra("existingPhotoUrls", ArrayList(record.photoUrls ?: emptyList()))
        }
        startActivity(intent)
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
