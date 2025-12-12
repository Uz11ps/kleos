package com.example.kleos.ui.programs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.data.network.ProgramDto
import com.example.kleos.databinding.ItemUniversityBinding

class ProgramsAdapter(
    private var items: List<ProgramDto>,
    private val onClick: (ProgramDto) -> Unit
) : RecyclerView.Adapter<ProgramsAdapter.VH>() {

    class VH(val binding: ItemUniversityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUniversityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.nameText.text = item.title
        
        // Анимация появления карточки с задержкой
        com.example.kleos.ui.utils.AnimationUtils.cardEnter(holder.itemView, position * 100L)
        
        // Анимация при нажатии
        holder.binding.root.setOnClickListener { 
            com.example.kleos.ui.utils.AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                com.example.kleos.ui.utils.AnimationUtils.releaseButton(it)
                onClick(item)
            }, 150)
        }
    }
    
    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        // Дополнительная анимация при появлении на экране
        com.example.kleos.ui.utils.AnimationUtils.fadeInScale(holder.itemView, 300)
    }

    fun submitList(newItems: List<ProgramDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}



