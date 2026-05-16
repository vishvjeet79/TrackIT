package com.example.trackit.ui.inventory

import android.net.nsd.NsdServiceInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackit.databinding.ItemPeerBinding

class PeerAdapter(
    private val onPeerClick: (NsdServiceInfo) -> Unit
) : ListAdapter<NsdServiceInfo, PeerAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemPeerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPeerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peer = getItem(position)
        holder.binding.peerName.text = peer.serviceName
        holder.binding.root.setOnClickListener { onPeerClick(peer) }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NsdServiceInfo>() {
        override fun areItemsTheSame(oldItem: NsdServiceInfo, newItem: NsdServiceInfo): Boolean {
            return oldItem.serviceName == newItem.serviceName
        }

        override fun areContentsTheSame(oldItem: NsdServiceInfo, newItem: NsdServiceInfo): Boolean {
            return oldItem == newItem
        }
    }
}
