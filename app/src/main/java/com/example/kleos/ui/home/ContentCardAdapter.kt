package com.example.kleos.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.R
import com.example.kleos.data.model.NewsItem
import com.example.kleos.databinding.ItemContentCardBinding

data class ContentCard(
    val id: String,
    val category: String,
    val title: String,
    val date: String,
    val backgroundColor: Int
)

class ContentCardAdapter(
    private var items: List<ContentCard>,
    private val onItemClick: ((ContentCard) -> Unit)? = null
) : RecyclerView.Adapter<ContentCardAdapter.ContentCardViewHolder>() {

    class ContentCardViewHolder(val binding: ItemContentCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentCardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemContentCardBinding.inflate(inflater, parent, false)
        return ContentCardViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ContentCardViewHolder, position: Int) {
        val item = items[position]
        holder.binding.categoryText.text = item.category
        holder.binding.titleText.text = item.title
        holder.binding.dateText.text = item.date
        
        // Устанавливаем цвет фона карточки
        holder.binding.root.setCardBackgroundColor(item.backgroundColor)
        
        // Обработка клика
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    fun submitList(newItems: List<ContentCard>) {
        items = newItems
        notifyDataSetChanged()
    }
}


