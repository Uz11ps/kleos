package com.kleos.education.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kleos.education.BuildConfig
import com.kleos.education.data.model.GalleryItem
import com.kleos.education.databinding.ItemGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
        holder.binding.categoryText.text = holder.itemView.context.getString(com.kleos.education.R.string.category_gallery)
        
        // Форматируем дату из description или используем текущую дату
        val dateText = item.description?.takeIf { it.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")) } 
            ?: java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        holder.binding.dateText.text = dateText
        
        // Отменяем предыдущую загрузку для этого ViewHolder
        imageLoadJobs[holder.binding.backgroundImage]?.cancel()
        imageLoadJobs.remove(holder.binding.backgroundImage)
        
        // Сбрасываем изображение перед загрузкой нового
        holder.binding.backgroundImage.setImageBitmap(null)
        holder.binding.backgroundImage.visibility = android.view.View.GONE
        holder.binding.backgroundImage.tag = null
        
        // По умолчанию показываем градиентный фон (overlay непрозрачный)
        holder.binding.overlayView.alpha = 1.0f
        
        // Загружаем изображение из mediaUrl, если это фото
        if (item.mediaType == "photo" && item.mediaUrl.isNotEmpty()) {
            loadImage(holder.binding.backgroundImage, item.mediaUrl, holder.binding.overlayView)
        }
        
        // Обработка клика на кнопку со стрелкой
        holder.binding.arrowButton.setOnClickListener {
            com.kleos.education.ui.utils.AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                com.kleos.education.ui.utils.AnimationUtils.releaseButton(it)
                onItemClick(item)
            }, 150)
        }
        
        // Анимация появления карточки с задержкой
        com.kleos.education.ui.utils.AnimationUtils.cardEnter(holder.itemView, position * 80L)
        
        // Анимация при нажатии на всю карточку
        holder.itemView.setOnClickListener { 
            com.kleos.education.ui.utils.AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                com.kleos.education.ui.utils.AnimationUtils.releaseButton(it)
                onItemClick(item)
            }, 150)
        }
    }
    
    private var imageLoadJobs = mutableMapOf<android.widget.ImageView, Job>()
    
    private fun loadImage(imageView: android.widget.ImageView, imageUrl: String, overlayView: android.view.View) {
        // Отменяем предыдущую загрузку для этого ImageView
        imageLoadJobs[imageView]?.cancel()
        
        // Используем GlobalScope для загрузки изображений, так как адаптер не имеет lifecycle
        val job = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO + kotlinx.coroutines.SupervisorJob()) {
            try {
                if (!isActive) return@launch
                
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
                
                if (!isActive) return@launch
                
                // Используем OkHttp клиент из ApiClient для правильной авторизации
                val okHttpClient = com.kleos.education.data.network.ApiClient.okHttpClient
                val request = Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!isActive) return@launch
                
                if (response.isSuccessful && response.body != null) {
                    response.body?.use { body ->
                        val inputStream = body.byteStream()
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        
                        if (!isActive) return@launch
                        
                        withContext(Dispatchers.Main) {
                            // Проверяем, что ImageView еще привязан к этому элементу
                            if (imageView.tag == imageUrl && bitmap != null && !bitmap.isRecycled) {
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
                        if (imageView.tag == imageUrl) {
                            imageView.visibility = android.view.View.GONE
                            overlayView.alpha = 1.0f
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Игнорируем отмену
                throw e
            } catch (e: Exception) {
                android.util.Log.e("GalleryAdapter", "Error loading image from $imageUrl: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (imageView.tag == imageUrl) {
                        imageView.visibility = android.view.View.GONE
                        overlayView.alpha = 1.0f
                    }
                }
            } finally {
                imageLoadJobs.remove(imageView)
            }
        }
        
        imageLoadJobs[imageView] = job
        imageView.tag = imageUrl // Сохраняем URL как тег для проверки актуальности
    }
    
    override fun onViewAttachedToWindow(holder: GalleryViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Дополнительная анимация при появлении на экране
        com.kleos.education.ui.utils.AnimationUtils.fadeInScale(holder.itemView, 300)
    }

    fun submitList(newItems: List<GalleryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}


