package com.kleos.education.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kleos.education.R

data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val answer: String
)

class FaqAdapter(
    private var items: List<FaqItem>,
    private val onClick: (FaqItem, View) -> Unit
) : RecyclerView.Adapter<FaqAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val questionText: TextView = itemView.findViewById(R.id.faqQuestionText)
        val hintText: TextView = itemView.findViewById(R.id.faqHintText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return QuestionViewHolder(inflater.inflate(R.layout.item_faq_card, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val item = items[position]
        holder.questionText.text = item.question
        // Подсказка показывается всегда
        holder.hintText.visibility = View.VISIBLE
        
        // Скрываем разделитель у первого элемента и корректируем отступы
        val divider = holder.itemView.findViewById<View>(R.id.divider)
        val params = holder.questionText.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        if (position == 0) {
            divider?.visibility = View.GONE
            // Для первого элемента убираем отступ сверху у вопроса
            params?.topMargin = 0
        } else {
            divider?.visibility = View.VISIBLE
            // Для остальных элементов стандартный отступ 16dp
            params?.topMargin = (16 * holder.itemView.context.resources.displayMetrics.density).toInt()
        }
        holder.questionText.layoutParams = params
        
        // Анимация появления FAQ элемента с задержкой
        val tag = holder.itemView.tag as? String
        if (tag != "animated") {
            val delay = position * 50L
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 20f
            
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(delay)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    holder.itemView.tag = "animated"
                }
                .start()
        }
        
        // Обработка нажатия на элемент FAQ
        holder.itemView.setOnClickListener {
            // Легкая анимация нажатия
            it.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    // Запускаем анимацию расширения
                    onClick(item, holder.itemView)
                }
                .start()
        }
    }
    
    override fun onViewAttachedToWindow(holder: QuestionViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Дополнительная анимация при появлении на экране при скролле
        if (holder.itemView.alpha == 0f && holder.itemView.tag != "animated") {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 20f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    holder.itemView.tag = "animated"
                }
                .start()
        }
    }

    fun submitList(newItems: List<FaqItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}



