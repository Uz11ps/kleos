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
        
        // Анимация появления карточки с задержкой
        com.example.kleos.ui.utils.AnimationUtils.cardEnter(holder.itemView, position * 80L)
        
        // Анимация при нажатии
        holder.itemView.setOnClickListener { 
            com.example.kleos.ui.utils.AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                com.example.kleos.ui.utils.AnimationUtils.releaseButton(it)
                onItemClick(item)
            }, 150)
        }
    }
    
    override fun onViewAttachedToWindow(holder: GalleryViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Дополнительная анимация при появлении на экране
        com.example.kleos.ui.utils.AnimationUtils.fadeInScale(holder.itemView, 300)
    }

    fun submitList(newItems: List<GalleryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

