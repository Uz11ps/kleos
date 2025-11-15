package com.example.kleos.ui.partners

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.data.network.PartnerDto
import com.example.kleos.databinding.ItemUniversityBinding

class PartnersSimpleAdapter(
    private var items: List<PartnerDto>,
    private val onClick: (PartnerDto) -> Unit
) : RecyclerView.Adapter<PartnersSimpleAdapter.VH>() {

    class VH(val binding: ItemUniversityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUniversityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.nameText.text = item.name
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    fun submitList(newItems: List<PartnerDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}



