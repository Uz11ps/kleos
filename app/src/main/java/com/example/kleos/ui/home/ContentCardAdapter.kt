package com.example.kleos.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kleos.R
import com.example.kleos.data.model.NewsItem
import com.example.kleos.databinding.ItemContentCardBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

data class ContentCard(
    val id: String,
    val category: String,
    val title: String,
    val date: String,
    val backgroundColor: Int,
    val imageUrl: String? = null
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

    override fun getItemCount(): Int {
        val count = items.size
        android.util.Log.d("ContentCardAdapter", "getItemCount: $count")
        return count
    }

    override fun onBindViewHolder(holder: ContentCardViewHolder, position: Int) {
        if (position >= items.size) {
            android.util.Log.e("ContentCardAdapter", "Position $position >= items.size ${items.size}")
            return
        }
        val item = items[position]
        android.util.Log.d("ContentCardAdapter", "Binding item at position $position: ${item.title}")
        holder.binding.categoryText.text = item.category
        holder.binding.titleText.text = item.title
        holder.binding.dateText.text = item.date
        
        // Сбрасываем изображение перед загрузкой нового
        holder.binding.backgroundImage.setImageBitmap(null)
        holder.binding.backgroundImage.visibility = android.view.View.GONE
        
        // По умолчанию показываем градиентный фон (overlay непрозрачный)
        holder.binding.overlayView.alpha = 1.0f
        
        val context = holder.itemView.context
        val newsCategory = context.getString(R.string.category_news)
        val interestingCategory = context.getString(R.string.category_interesting)
        
        // Устанавливаем фон для overlay в зависимости от категории
        val backgroundDrawable = when (item.category) {
            newsCategory -> R.drawable.bg_news_card
            interestingCategory -> R.drawable.bg_interesting_card
            else -> R.drawable.bg_news_card
        }
        holder.binding.overlayView.setBackgroundResource(backgroundDrawable)
        holder.binding.root.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // Устанавливаем бейдж для категории
        val badgeBackground = when (item.category) {
            newsCategory -> R.drawable.bg_category_badge_news
            interestingCategory -> R.drawable.bg_category_badge_interesting
            else -> R.drawable.bg_category_badge_news
        }
        holder.binding.categoryText.setBackgroundResource(badgeBackground)
        
        // Загружаем изображение из imageUrl, если оно есть
        if (!item.imageUrl.isNullOrEmpty()) {
            android.util.Log.d("ContentCardAdapter", "Item: ${item.title}, Loading image from: ${item.imageUrl}")
            loadImage(holder.binding.backgroundImage, item.imageUrl, holder.binding.overlayView)
        } else {
            android.util.Log.d("ContentCardAdapter", "No image to load for item: ${item.title}")
        }
        
        // Обработка клика
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
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
                
                android.util.Log.d("ContentCardAdapter", "Loading image from: $fullUrl")
                
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
                                android.util.Log.d("ContentCardAdapter", "Image loaded successfully, size: ${bitmap.width}x${bitmap.height}")
                                imageView.setImageBitmap(bitmap)
                                imageView.visibility = android.view.View.VISIBLE
                                overlayView.alpha = 0.3f
                                android.util.Log.d("ContentCardAdapter", "Image displayed, overlay alpha set to 0.3")
                            } else {
                                android.util.Log.e("ContentCardAdapter", "Failed to decode bitmap or bitmap is recycled")
                                imageView.visibility = android.view.View.GONE
                                overlayView.alpha = 1.0f
                            }
                        }
                    }
                } else {
                    android.util.Log.e("ContentCardAdapter", "Failed to load image: HTTP ${response.code}")
                    withContext(Dispatchers.Main) {
                        imageView.visibility = android.view.View.GONE
                        overlayView.alpha = 1.0f
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ContentCardAdapter", "Error loading image from $imageUrl: ${e.message}", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    imageView.visibility = android.view.View.GONE
                    overlayView.alpha = 1.0f
                }
            }
        }
    }

    fun submitList(newItems: List<ContentCard>) {
        android.util.Log.d("ContentCardAdapter", "submitList called with ${newItems.size} items")
        items = newItems
        notifyDataSetChanged()
        android.util.Log.d("ContentCardAdapter", "notifyDataSetChanged called, current items.size: ${items.size}")
    }
}


