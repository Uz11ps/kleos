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
        
        // Определяем тип карточки (новость или интересное) по ID
        val isInteresting = item.id.hashCode() % 2 != 0
        
        if (isInteresting) {
            // Интересное - желтый фон
            holder.itemView.setBackgroundResource(com.example.kleos.R.drawable.bg_interesting_card)
            holder.binding.categoryBadge.text = "Интересное"
            holder.binding.categoryBadge.setBackgroundResource(com.example.kleos.R.drawable.bg_category_badge_interesting)
            holder.binding.titleText.setTextColor(android.graphics.Color.BLACK)
            holder.binding.dateText.setTextColor(android.graphics.Color.BLACK)
        } else {
            // Новости - синий фон
            holder.itemView.setBackgroundResource(com.example.kleos.R.drawable.bg_news_card)
            holder.binding.categoryBadge.text = "Новости"
            holder.binding.categoryBadge.setBackgroundResource(com.example.kleos.R.drawable.bg_category_badge_news)
            holder.binding.titleText.setTextColor(android.graphics.Color.WHITE)
            holder.binding.dateText.setTextColor(android.graphics.Color.WHITE)
        }
        
        // Обработка клика на кнопку со стрелкой
        holder.binding.arrowButton.setOnClickListener {
            AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                AnimationUtils.releaseButton(it)
                if (onItemClick != null) {
                    onItemClick(item)
                }
            }, 150)
        }
        
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


