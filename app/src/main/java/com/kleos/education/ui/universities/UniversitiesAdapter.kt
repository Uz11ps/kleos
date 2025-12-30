package com.kleos.education.ui.universities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kleos.education.data.network.UniversityDto
import com.kleos.education.databinding.ItemUniversityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class UniversitiesAdapter(
    private var items: List<UniversityDto>,
    private val onItemClick: (UniversityDto) -> Unit
) : RecyclerView.Adapter<UniversitiesAdapter.UniversityViewHolder>() {

    class UniversityViewHolder(val binding: ItemUniversityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UniversityViewHolder {
        val binding = ItemUniversityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UniversityViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: UniversityViewHolder, position: Int) {
        val university = items[position]
        holder.binding.nameText.text = university.name
        
        val location = listOfNotNull(university.city, university.country).joinToString(", ")
        if (location.isNotEmpty()) {
            holder.binding.locationText.text = location
            holder.binding.locationText.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.locationText.visibility = android.view.View.GONE
        }
        
        // Устанавливаем категорию
        holder.binding.categoryText.text = holder.itemView.context.getString(com.kleos.education.R.string.category_universities)
        
        // Сбрасываем изображение перед загрузкой нового
        holder.binding.backgroundImage.setImageBitmap(null)
        holder.binding.backgroundImage.visibility = android.view.View.GONE
        
        // По умолчанию показываем градиентный фон (overlay непрозрачный)
        holder.binding.overlayView.alpha = 1.0f
        
        // Загружаем изображение из logoUrl, если оно есть
        if (!university.logoUrl.isNullOrEmpty()) {
            loadImage(holder.binding.backgroundImage, university.logoUrl, holder.binding.overlayView)
        }
        
        // Обработка клика на карточку
        holder.itemView.setOnClickListener {
            onItemClick(university)
        }
        
        // Обработка клика на кнопку стрелки
        holder.binding.arrowButton.setOnClickListener {
            onItemClick(university)
        }
    }

    fun submitList(newItems: List<UniversityDto>) {
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
                    val baseUrl = com.kleos.education.BuildConfig.API_BASE_URL.trimEnd('/')
                    if (imageUrl.startsWith("/")) {
                        "$baseUrl$imageUrl"
                    } else {
                        "$baseUrl/$imageUrl"
                    }
                }
                
                // Используем OkHttp клиент из ApiClient для правильной авторизации
                val okHttpClient = com.kleos.education.data.network.ApiClient.okHttpClient
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
                                imageView.setImageBitmap(bitmap)
                                imageView.visibility = android.view.View.VISIBLE
                                overlayView.alpha = 0.3f
                            } else {
                                imageView.visibility = android.view.View.GONE
                                overlayView.alpha = 1.0f
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        imageView.visibility = android.view.View.GONE
                        overlayView.alpha = 1.0f
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    imageView.visibility = android.view.View.GONE
                    overlayView.alpha = 1.0f
                }
            }
        }
    }
}



