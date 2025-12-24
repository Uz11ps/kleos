package com.example.kleos.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.BuildConfig
import com.example.kleos.data.model.GalleryItem
import com.example.kleos.databinding.ItemGalleryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

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
        holder.binding.categoryText.text = holder.itemView.context.getString(com.example.kleos.R.string.category_gallery)
        
        // Форматируем дату из description или используем текущую дату
        val dateText = item.description?.takeIf { it.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")) } 
            ?: java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        holder.binding.dateText.text = dateText
        
        // Сбрасываем изображение перед загрузкой нового
        holder.binding.backgroundImage.setImageBitmap(null)
        holder.binding.backgroundImage.visibility = android.view.View.GONE
        
        // По умолчанию показываем градиентный фон (overlay непрозрачный)
        holder.binding.overlayView.alpha = 1.0f
        
        // Загружаем изображение из mediaUrl, если это фото
        if (item.mediaType == "photo" && item.mediaUrl.isNotEmpty()) {
            android.util.Log.d("GalleryAdapter", "Item: ${item.title}, Loading image from: ${item.mediaUrl}")
            loadImage(holder.binding.backgroundImage, item.mediaUrl, holder.binding.overlayView)
        } else {
            // Если это видео или нет URL, показываем только градиентный фон
            android.util.Log.d("GalleryAdapter", "No image to load - mediaType: ${item.mediaType}, mediaUrl: ${item.mediaUrl}")
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
    
    private fun loadImage(imageView: android.widget.ImageView, imageUrl: String, overlayView: android.view.View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Формируем полный URL, если он относительный
                val fullUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    imageUrl
                } else {
                    // Если URL начинается с /, добавляем базовый URL API
                    val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
                    if (imageUrl.startsWith("/")) {
                        "$baseUrl$imageUrl"
                    } else {
                        "$baseUrl/$imageUrl"
                    }
                }
                
                android.util.Log.d("GalleryAdapter", "Loading image from: $fullUrl")
                
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
                                android.util.Log.d("GalleryAdapter", "Image loaded successfully, size: ${bitmap.width}x${bitmap.height}")
                                // Сначала показываем изображение
                                imageView.setImageBitmap(bitmap)
                                imageView.visibility = android.view.View.VISIBLE
                                // Затем делаем overlay полупрозрачным, чтобы изображение было видно
                                overlayView.alpha = 0.3f
                                android.util.Log.d("GalleryAdapter", "Image displayed, overlay alpha set to 0.3")
                            } else {
                                android.util.Log.e("GalleryAdapter", "Failed to decode bitmap or bitmap is recycled")
                                imageView.visibility = android.view.View.GONE
                                overlayView.alpha = 1.0f
                            }
                        }
                    }
                } else {
                    android.util.Log.e("GalleryAdapter", "Failed to load image: HTTP ${response.code}")
                    withContext(Dispatchers.Main) {
                        imageView.visibility = android.view.View.GONE
                        overlayView.alpha = 1.0f
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryAdapter", "Error loading image from $imageUrl: ${e.message}", e)
                e.printStackTrace()
                // В случае ошибки скрываем изображение, остается градиентный фон
                withContext(Dispatchers.Main) {
                    imageView.visibility = android.view.View.GONE
                    overlayView.alpha = 1.0f
                }
            }
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

