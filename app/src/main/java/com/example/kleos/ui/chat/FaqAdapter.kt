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
    private val onClick: (FaqItem) -> Unit,
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
            holder.itemView.setOnClickListener { onCtaClick() }
        } else {
            val item = items[position - 1]
            (holder as QuestionViewHolder).questionText.text = item.question
            holder.itemView.setOnClickListener { onClick(item) }
        }
    }

    fun submitList(newItems: List<FaqItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}


