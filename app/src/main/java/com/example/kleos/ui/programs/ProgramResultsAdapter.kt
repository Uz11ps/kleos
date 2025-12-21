package com.example.kleos.ui.programs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.data.network.ProgramDto
import com.example.kleos.databinding.ItemProgramResultBinding

class ProgramResultsAdapter(
    private var items: List<ProgramDto>,
    private val onClick: (ProgramDto) -> Unit
) : RecyclerView.Adapter<ProgramResultsAdapter.VH>() {

    class VH(val binding: ItemProgramResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProgramResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.programTitle.text = item.title
        holder.binding.programDescription.text = item.description ?: ""
        
        holder.binding.root.setOnClickListener {
            onClick(item)
        }
    }

    fun submitList(newItems: List<ProgramDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}

