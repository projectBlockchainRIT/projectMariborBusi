package com.example.projektna.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projektna.data.api.model.DepartureGroup
import com.example.projektna.databinding.ItemDepartureBinding

class DeparturesAdapter : ListAdapter<DepartureGroup, DeparturesAdapter.DepartureViewHolder>(DepartureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartureViewHolder {
        val binding = ItemDepartureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DepartureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DepartureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DepartureViewHolder(
        private val binding: ItemDepartureBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(departure: DepartureGroup) {
            binding.textViewLine.text = departure.line
            binding.textViewDirection.text = departure.direction

            // Show first 3 times
            val timesText = departure.times.take(3).joinToString(", ")
            binding.textViewTimes.text = timesText
        }
    }

    private class DepartureDiffCallback : DiffUtil.ItemCallback<DepartureGroup>() {
        override fun areItemsTheSame(oldItem: DepartureGroup, newItem: DepartureGroup): Boolean {
            return oldItem.line == newItem.line && oldItem.direction == newItem.direction
        }

        override fun areContentsTheSame(oldItem: DepartureGroup, newItem: DepartureGroup): Boolean {
            return oldItem == newItem
        }
    }
}
