package com.example.kleos.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.R

data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val answer: String
)

class FaqAdapter(
    private var items: List<FaqItem>,
    private val onClick: (FaqItem, View) -> Unit,
    private val onCtaClick: () -> Unit
) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CTA = 0
        private const val VIEW_TYPE_FAQ = 1
    }

    open class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class QuestionViewHolder(itemView: View) : FaqViewHolder(itemView) {
        val questionText: TextView = itemView.findViewById(R.id.faqQuestionText)
    }

    class CtaViewHolder(itemView: View) : FaqViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_CTA else VIEW_TYPE_FAQ
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_CTA) {
            val v = inflater.inflate(R.layout.item_faq_cta, parent, false)
            // apply dynamic i18n override
            v.findViewById<TextView>(R.id.ctaText)?.text =
                parent.context.applicationContext.let { (it as android.content.Context) }.let {
                    com.example.kleos.ui.language.TranslationManager.getOverride(it, R.string.no_suitable_question)
                }
            CtaViewHolder(v)
        } else {
            QuestionViewHolder(inflater.inflate(R.layout.item_faq_card, parent, false))
        }
    }

    override fun getItemCount(): Int = items.size + 1

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_CTA) {
            // Крутая анимация для CTA карточки с множественными эффектами
            holder.itemView.alpha = 0f
            holder.itemView.scaleX = 0.3f
            holder.itemView.scaleY = 0.3f
            holder.itemView.rotation = -180f
            holder.itemView.translationY = -50f
            
            // Последовательная анимация появления
            holder.itemView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .translationY(0f)
                .setDuration(800)
                .setInterpolator(android.view.animation.BounceInterpolator())
                .withEndAction {
                    // После появления - эффект "вспышки"
                    holder.itemView.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(200)
                        .withEndAction {
                            holder.itemView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start()
                        }
                        .start()
                }
                .start()
            
            // Пульсация для привлечения внимания (начинается через секунду)
            // Используем view.post для безопасности
            holder.itemView.postDelayed({
                if (holder.itemView.parent != null) {
                    com.example.kleos.ui.utils.AnimationUtils.pulse(holder.itemView, 2000)
                }
            }, 1500)
            
            // Анимация иконки и текста внутри CTA
            val textView = holder.itemView.findViewById<TextView>(R.id.ctaText)
            val parentLayout = textView?.parent as? ViewGroup
            val iconView = parentLayout?.let { layout ->
                // Ищем ImageView в родительском LinearLayout (обычно первый элемент)
                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    if (child is android.widget.ImageView) {
                        return@let child
                    }
                }
                null
            }
            
            // Анимация иконки
            iconView?.let { icon ->
                icon.alpha = 0f
                icon.rotation = -360f
                icon.scaleX = 0f
                icon.scaleY = 0f
                holder.itemView.postDelayed({
                    if (icon.parent != null) {
                        icon.animate()
                            .alpha(1f)
                            .rotation(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(600)
                            .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                            .start()
                    }
                }, 400)
            }
            
            // Анимация текста CTA
            textView?.let { text ->
                text.alpha = 0f
                text.translationX = 30f
                holder.itemView.postDelayed({
                    if (text.parent != null) {
                        text.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(500)
                            .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                            .start()
                    }
                }, 600)
            }
            
            // Анимация при нажатии
            holder.itemView.setOnTouchListener { view, motionEvent ->
                when (motionEvent.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        com.example.kleos.ui.utils.AnimationUtils.pressButton(view)
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        com.example.kleos.ui.utils.AnimationUtils.releaseButton(view)
                        // Дополнительный эффект перед переходом
                        com.example.kleos.ui.utils.AnimationUtils.bounceIn(view, 400)
                        view.postDelayed({
                            onCtaClick()
                        }, 300)
                    }
                }
                true
            }
        } else {
            val item = items[position - 1]
            (holder as QuestionViewHolder).questionText.text = item.question
            
            // Анимация появления FAQ карточки с задержкой
            // Проверяем, что элемент еще не был анимирован
            val tag = holder.itemView.tag as? String
            if (tag != "animated") {
                val delay = (position - 1) * 80L
                holder.itemView.alpha = 0f
                holder.itemView.scaleX = 0.8f
                holder.itemView.scaleY = 0.8f
                holder.itemView.translationY = 30f
                
                holder.itemView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(delay)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
                    .withEndAction {
                        holder.itemView.tag = "animated"
                    }
                    .start()
            }
            
            // Плавная анимация при нажатии на карточку
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
    }
    
    override fun onViewAttachedToWindow(holder: FaqViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Дополнительная анимация при появлении на экране при скролле
        // Только если элемент еще не был анимирован
        if (holder.itemView.alpha == 0f && holder.itemView.tag != "animated") {
            com.example.kleos.ui.utils.AnimationUtils.fadeInScale(holder.itemView, 400)
            holder.itemView.tag = "animated"
        }
    }

    fun submitList(newItems: List<FaqItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}


