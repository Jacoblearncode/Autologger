package com.nibm.autocare.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nibm.autocare.R
import com.nibm.autocare.model.Vehicle

class VehicleAdapter(
    private val onItemClick: (Vehicle) -> Unit,
    private val onItemLongClick: (Vehicle) -> Unit,
    private val onEditClick: (Vehicle) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    private val items = mutableListOf<Vehicle>()

    fun submitList(newList: List<Vehicle>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivVehiclePhoto: ImageView = itemView.findViewById(R.id.ivVehiclePhoto)
        private val tvRegistrationNumber: TextView = itemView.findViewById(R.id.tvRegistrationNumber)
        private val tvBrand: TextView = itemView.findViewById(R.id.tvBrand)
        private val tvManufacturedYear: TextView = itemView.findViewById(R.id.tvManufacturedYear)
        private val tvModel: TextView = itemView.findViewById(R.id.tvModel)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)

        fun bind(vehicle: Vehicle) {
            tvRegistrationNumber.text = vehicle.registrationNumber
            tvBrand.text = vehicle.brand
            tvManufacturedYear.text = vehicle.manufacturedYear
            tvModel.text = vehicle.model

            when {
                vehicle.photoUrl.isNotEmpty() -> {
                    Glide.with(itemView.context)
                        .load(vehicle.photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.circle_gray_bg)
                        .into(ivVehiclePhoto)
                }
                vehicle.defaultImageUrl.isNotEmpty() -> {
                    Glide.with(itemView.context)
                        .load(vehicle.defaultImageUrl)
                        .centerCrop()
                        .circleCrop()
                        .placeholder(R.drawable.circle_gray_bg)
                        .into(ivVehiclePhoto)
                }
                else -> {
                    ivVehiclePhoto.setImageDrawable(null)
                    ivVehiclePhoto.background =
                        ContextCompat.getDrawable(itemView.context, R.drawable.circle_gray_bg)
                }
            }

            itemView.setOnClickListener { onItemClick(vehicle) }
            itemView.setOnLongClickListener {
                onItemLongClick(vehicle)
                true
            }
            btnEdit.setOnClickListener { onEditClick(vehicle) }
        }
    }
}
