package com.example.kleos.ui.partners

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.data.network.PartnerDto
import com.example.kleos.databinding.ItemPartnerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

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
        
        // Сбрасываем изображение перед загрузкой нового
        holder.binding.backgroundImage.setImageBitmap(null)
        holder.binding.backgroundImage.visibility = android.view.View.GONE
        
        // По умолчанию показываем градиентный фон (overlay непрозрачный)
        holder.binding.overlayView.alpha = 1.0f
        
        // Загружаем изображение из logoUrl, если оно есть
        if (!item.logoUrl.isNullOrEmpty()) {
            android.util.Log.d("PartnerAdapter", "Item: ${item.name}, Loading image from: ${item.logoUrl}")
            loadImage(holder.binding.backgroundImage, item.logoUrl, holder.binding.overlayView)
        } else {
            android.util.Log.d("PartnerAdapter", "No image to load for item: ${item.name}")
        }
        
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
    
    private fun loadImage(imageView: android.widget.ImageView, imageUrl: String, overlayView: android.view.View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Формируем полный URL, если он относительный
                val fullUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    imageUrl
                } else {
                    val baseUrl = com.example.kleos.BuildConfig.API_BASE_URL.trimEnd('/')
                    if (imageUrl.startsWith("/")) {
                        "$baseUrl$imageUrl"
                    } else {
                        "$baseUrl/$imageUrl"
                    }
                }
                
                android.util.Log.d("PartnerAdapter", "Loading image from: $fullUrl")
                
                // Используем OkHttp клиент из ApiClient для правильной авторизации
                val okHttpClient = com.example.kleos.data.network.ApiClient.okHttpClient
                val request = Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful && response.body != null) {
                    response.body?.use { body ->
                        val inputStream = body.byteStream()
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        withContext(Dispatchers.Main) {
                            if (bitmap != null && !bitmap.isRecycled) {
                                android.util.Log.d("PartnerAdapter", "Image loaded successfully, size: ${bitmap.width}x${bitmap.height}")
                                imageView.setImageBitmap(bitmap)
                                imageView.visibility = android.view.View.VISIBLE
                                overlayView.alpha = 0.3f
                                android.util.Log.d("PartnerAdapter", "Image displayed, overlay alpha set to 0.3")
                            } else {
                                android.util.Log.e("PartnerAdapter", "Failed to decode bitmap or bitmap is recycled")
                                imageView.visibility = android.view.View.GONE
                                overlayView.alpha = 1.0f
                            }
                        }
                    }
                } else {
                    android.util.Log.e("PartnerAdapter", "Failed to load image: HTTP ${response.code}")
                    withContext(Dispatchers.Main) {
                        imageView.visibility = android.view.View.GONE
                        overlayView.alpha = 1.0f
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PartnerAdapter", "Error loading image from $imageUrl: ${e.message}", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    imageView.visibility = android.view.View.GONE
                    overlayView.alpha = 1.0f
                }
            }
        }
    }
}


