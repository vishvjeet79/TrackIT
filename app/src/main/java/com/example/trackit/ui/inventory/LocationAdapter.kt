package com.example.trackit.ui.inventory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.trackit.data.Location
import com.example.trackit.databinding.ItemLocationBinding

class LocationAdapter(
    private val onLocationClick: (Location) -> Unit,
    private val onAddSubClick: (Location) -> Unit,
    private val onDeleteClick: (Location) -> Unit
) : ListAdapter<Location, LocationAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLocationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = getItem(position)
        with(holder.binding) {
            locationName.text = location.name
            
            if (location.imagePath != null) {
                locationImage.visibility = View.VISIBLE
                locationImage.load(location.imagePath)
            } else {
                locationImage.visibility = View.GONE
            }

            if (location.parentName != null) {
                sublocationIndicator.visibility = View.VISIBLE
                btnAddSub.visibility = View.GONE
                btnDelete.visibility = View.VISIBLE
            } else {
                sublocationIndicator.visibility = View.GONE
                btnAddSub.visibility = View.VISIBLE
                btnDelete.visibility = View.GONE
            }

            root.setOnClickListener { onLocationClick(location) }
            btnAddSub.setOnClickListener { onAddSubClick(location) }
            btnDelete.setOnClickListener { onDeleteClick(location) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem == newItem
        }
    }
}
