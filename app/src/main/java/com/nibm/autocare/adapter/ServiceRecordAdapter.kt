package com.nibm.autocare.adapter

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nibm.autocare.R
import com.nibm.autocare.model.ServiceRecord

/**
 * RecyclerView adapter for the service record timeline (Tab 1).
 *
 * Each row can be expanded to reveal full details (service type, checked items,
 * notes, and photos). Expand state is tracked per-position in expandedPositions
 * so multiple rows can be open simultaneously and state survives scrolling.
 *
 * notifyItemChanged(position) is used for expand/collapse to redraw only the
 * tapped row, avoiding the full-list flash that notifyDataSetChanged() causes.
 */
class ServiceRecordAdapter(
    private val onEditClick: (ServiceRecord) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<ServiceRecordAdapter.ServiceViewHolder>() {

    private val items = mutableListOf<ServiceRecord>()

    // Tracks which positions are currently expanded. Stored as a Set for O(1) lookup.
    private val expandedPositions = mutableSetOf<Int>()

    /**
     * Replaces the dataset. Clears expanded state so stale rows don't stay
     * open when the list is refreshed after a delete.
     */
    fun submitList(newList: List<ServiceRecord>) {
        items.clear()
        items.addAll(newList)
        expandedPositions.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_service_timeline, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOdometerReading: TextView = itemView.findViewById(R.id.tvOdometerReading)
        private val tvServiceDate: TextView = itemView.findViewById(R.id.tvServiceDate)
        private val tvServiceCost: TextView = itemView.findViewById(R.id.tvServiceCost)
        private val tvServiceType: TextView = itemView.findViewById(R.id.tvServiceType)
        private val tvCheckedItems: TextView = itemView.findViewById(R.id.tvCheckedItems)
        private val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
        private val llExpandedDetails: LinearLayout = itemView.findViewById(R.id.llExpandedDetails)
        private val imageContainer: LinearLayout = itemView.findViewById(R.id.imageContainer)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val viewLineTop: View = itemView.findViewById(R.id.viewLineTop)
        private val viewLineBottom: View = itemView.findViewById(R.id.viewLineBottom)

        fun bind(service: ServiceRecord, position: Int) {
            tvOdometerReading.text = "${service.odometerReading} km"
            tvServiceDate.text = service.date
            tvServiceCost.text = "Rs ${service.serviceCost}"

            // Optional fields: hide the view entirely when the record has no value.
            service.serviceType?.let {
                tvServiceType.text = it
                tvServiceType.visibility = View.VISIBLE
            } ?: run { tvServiceType.visibility = View.GONE }

            service.checkedItems?.let {
                tvCheckedItems.text = "Services:\n${it.joinToString("\n• ", "• ")}"
                tvCheckedItems.visibility = View.VISIBLE
            } ?: run { tvCheckedItems.visibility = View.GONE }

            service.notes?.let {
                tvNotes.text = "Notes: $it"
                tvNotes.visibility = View.VISIBLE
            } ?: run { tvNotes.visibility = View.GONE }

            // Rebuild photo thumbnails every bind to keep them in sync after list changes.
            imageContainer.removeAllViews()
            service.photoUrls?.takeIf { it.isNotEmpty() }?.forEach { url ->
                val imageSize = dpToPx(itemView.context, 250)
                val imageView = ImageView(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply {
                        marginEnd = dpToPx(itemView.context, 8)
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = true
                    clipToOutline = true
                    background = ContextCompat.getDrawable(itemView.context, R.drawable.image_border)
                }
                Glide.with(itemView.context)
                    .load(url)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(imageView)
                imageView.setOnClickListener { showFullImageDialog(url) }
                imageContainer.addView(imageView)
            }

            // Show or hide the expanded detail section based on the tracked set.
            llExpandedDetails.visibility =
                if (expandedPositions.contains(position)) View.VISIBLE else View.GONE

            // Hide the timeline connector lines at the top of the first item and
            // the bottom of the last item so the timeline looks visually complete.
            viewLineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            viewLineBottom.visibility =
                if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE

            itemView.setOnClickListener {
                if (expandedPositions.contains(position)) {
                    expandedPositions.remove(position)
                } else {
                    expandedPositions.add(position)
                }
                // notifyItemChanged redraws only this row — avoids the full-list flash
                // that notifyDataSetChanged() would produce on every tap.
                notifyItemChanged(position)
            }

            btnEdit.setOnClickListener { onEditClick(service) }
            btnDelete.setOnClickListener { onDeleteClick(service.recordId) }
        }

        private fun showFullImageDialog(imageUrl: String) {
            Dialog(itemView.context).apply {
                setContentView(R.layout.dialog_full_image)
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                findViewById<ImageView>(R.id.ivFullImage).let { imageView ->
                    Glide.with(itemView.context).load(imageUrl).into(imageView)
                }
                findViewById<View>(R.id.btnClose).setOnClickListener { dismiss() }
                show()
            }
        }

        private fun dpToPx(context: Context, dp: Int): Int =
            (dp * context.resources.displayMetrics.density).toInt()
    }
}
