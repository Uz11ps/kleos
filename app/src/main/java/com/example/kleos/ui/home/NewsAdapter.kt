package com.example.kleos.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.data.model.NewsItem
import com.example.kleos.databinding.ItemNewsCardBinding
import com.example.kleos.ui.utils.AnimationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NewsAdapter(
    private var items: List<NewsItem>,
    private val onItemClick: ((NewsItem) -> Unit)? = null
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(val binding: ItemNewsCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNewsCardBinding.inflate(inflater, parent, false)
        return NewsViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.title
        holder.binding.dateText.text = item.dateText
        
        // Анимация появления карточки с задержкой
        AnimationUtils.cardEnter(holder.itemView, position * 100L)
        
        // Обработка клика по карточке
        holder.itemView.setOnClickListener {
            AnimationUtils.pressButton(it)
            it.postDelayed({
                AnimationUtils.releaseButton(it)
                // Показываем диалог с деталями новости
                if (onItemClick != null) {
                    onItemClick(item)
                } else {
                    // Fallback: показываем простой диалог
                    val context = holder.itemView.context
                    if (context is FragmentActivity) {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(item.title)
                            .setMessage("${item.dateText}\n\n${item.content ?: "Нет дополнительной информации"}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }, 100)
        }
    }
    
    override fun onViewAttachedToWindow(holder: NewsViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Дополнительная анимация при появлении на экране
        AnimationUtils.fadeInScale(holder.itemView, 300)
    }

    fun submitList(newItems: List<NewsItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}


