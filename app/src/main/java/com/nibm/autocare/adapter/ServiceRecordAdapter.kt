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

class ServiceRecordAdapter(
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<ServiceRecordAdapter.ServiceViewHolder>() {

    private val items = mutableListOf<ServiceRecord>()
    private val expandedPositions = mutableSetOf<Int>()

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
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val viewLineTop: View = itemView.findViewById(R.id.viewLineTop)
        private val viewLineBottom: View = itemView.findViewById(R.id.viewLineBottom)

        fun bind(service: ServiceRecord, position: Int) {
            tvOdometerReading.text = "${service.odometerReading} km"
            tvServiceDate.text = service.date
            tvServiceCost.text = "Rs ${service.serviceCost}"

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

            llExpandedDetails.visibility =
                if (expandedPositions.contains(position)) View.VISIBLE else View.GONE

            viewLineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            viewLineBottom.visibility =
                if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE

            itemView.setOnClickListener {
                if (expandedPositions.contains(position)) {
                    expandedPositions.remove(position)
                } else {
                    expandedPositions.add(position)
                }
                notifyItemChanged(position)
            }

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
