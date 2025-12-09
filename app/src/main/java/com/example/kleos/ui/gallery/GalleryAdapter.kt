package com.example.kleos.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.data.model.GalleryItem
import com.example.kleos.databinding.ItemGalleryBinding

class GalleryAdapter(
    private var items: List<GalleryItem>,
    private val onItemClick: (GalleryItem) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    class GalleryViewHolder(val binding: ItemGalleryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGalleryBinding.inflate(inflater, parent, false)
        return GalleryViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.title
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    fun submitList(newItems: List<GalleryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

