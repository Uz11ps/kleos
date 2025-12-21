package com.example.kleos.ui.partners

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.data.network.PartnerDto
import com.example.kleos.databinding.ItemPartnerBinding

class PartnerAdapter(
    private var items: List<PartnerDto>,
    private val onItemClick: (PartnerDto) -> Unit
) : RecyclerView.Adapter<PartnerAdapter.PartnerViewHolder>() {

    class PartnerViewHolder(val binding: ItemPartnerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartnerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPartnerBinding.inflate(inflater, parent, false)
        return PartnerViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PartnerViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.name
        holder.binding.categoryText.text = "Партнеры"
        
        // Обработка клика на кнопку со стрелкой
        holder.binding.arrowButton.setOnClickListener {
            com.example.kleos.ui.utils.AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                com.example.kleos.ui.utils.AnimationUtils.releaseButton(it)
                onItemClick(item)
            }, 150)
        }
        
        // Анимация появления карточки с задержкой
        com.example.kleos.ui.utils.AnimationUtils.cardEnter(holder.itemView, position * 80L)
        
        // Анимация при нажатии на всю карточку
        holder.itemView.setOnClickListener { 
            com.example.kleos.ui.utils.AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                com.example.kleos.ui.utils.AnimationUtils.releaseButton(it)
                onItemClick(item)
            }, 150)
        }
    }
    
    override fun onViewAttachedToWindow(holder: PartnerViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Дополнительная анимация при появлении на экране
        com.example.kleos.ui.utils.AnimationUtils.fadeInScale(holder.itemView, 300)
    }

    fun submitList(newItems: List<PartnerDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}


