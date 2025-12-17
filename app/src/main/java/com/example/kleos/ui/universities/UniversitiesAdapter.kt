package com.example.kleos.ui.universities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.data.network.UniversityDto
import com.example.kleos.databinding.ItemUniversityBinding

class UniversitiesAdapter(
    private var items: List<UniversityDto>,
    private val onItemClick: (UniversityDto) -> Unit
) : RecyclerView.Adapter<UniversitiesAdapter.UniversityViewHolder>() {

    class UniversityViewHolder(val binding: ItemUniversityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UniversityViewHolder {
        val binding = ItemUniversityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UniversityViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: UniversityViewHolder, position: Int) {
        val university = items[position]
        holder.binding.nameText.text = university.name
        
        val location = listOfNotNull(university.city, university.country).joinToString(", ")
        if (location.isNotEmpty()) {
            holder.binding.locationText?.text = location
            holder.binding.locationText?.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.locationText?.visibility = android.view.View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(university)
        }
    }

    fun submitList(newItems: List<UniversityDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}


