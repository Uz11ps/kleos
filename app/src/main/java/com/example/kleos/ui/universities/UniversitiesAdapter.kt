package com.example.kleos.ui.universities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.databinding.ItemUniversityBinding

class UniversitiesAdapter(
    private var items: List<String>
) : RecyclerView.Adapter<UniversitiesAdapter.UniversityViewHolder>() {

    class UniversityViewHolder(val binding: ItemUniversityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UniversityViewHolder {
        val binding = ItemUniversityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UniversityViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: UniversityViewHolder, position: Int) {
        holder.binding.nameText.text = items[position]
    }

    fun submitList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}


