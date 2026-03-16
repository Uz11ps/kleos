package com.kleos.education.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.kleos.education.data.model.NewsItem
import com.kleos.education.databinding.ItemNewsCardBinding
import com.kleos.education.ui.utils.AnimationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import okhttp3.Request

class NewsAdapter(
    private var items: List<NewsItem>,
    private val onItemClick: ((NewsItem) -> Unit)? = null
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(val binding: ItemNewsCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNewsCardBinding.inflate(inflater, parent, false)
        return NewsViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.title
        holder.binding.dateText.text = item.dateText
        
        // Отменяем предыдущую загрузку для этого ViewHolder
        imageLoadJobs[holder.binding.backgroundImage]?.cancel()
        imageLoadJobs.remove(holder.binding.backgroundImage)
        
        // Сбрасываем изображение перед загрузкой нового
        holder.binding.backgroundImage.setImageBitmap(null)
        holder.binding.backgroundImage.visibility = android.view.View.GONE
        holder.binding.backgroundImage.tag = null
        
        // По умолчанию показываем градиентный фон (overlay непрозрачный)
        holder.binding.overlayView.alpha = 1.0f
        
        // Определяем тип карточки (новость или интересное) по ID
        val isInteresting = item.id.hashCode() % 2 != 0
        
        val backgroundDrawable = if (isInteresting) {
            // Интересное - желтый фон
            com.kleos.education.R.drawable.bg_interesting_card
        } else {
            // Новости - синий фон
            com.kleos.education.R.drawable.bg_news_card
        }
        
        holder.binding.overlayView.setBackgroundResource(backgroundDrawable)
        holder.binding.root.setCardBackgroundColor(android.graphics.Color.TRANSPARENT) // Ensure transparent background for MaterialCardView
        
        // Устанавливаем бейдж для категории
        val badgeBackground = if (isInteresting) {
            com.kleos.education.R.drawable.bg_category_badge_interesting
        } else {
            com.kleos.education.R.drawable.bg_category_badge_news
        }
        holder.binding.categoryBadge.setBackgroundResource(badgeBackground)
        
        if (isInteresting) {
            holder.binding.categoryBadge.text = holder.itemView.context.getString(com.kleos.education.R.string.category_interesting)
            holder.binding.titleText.setTextColor(android.graphics.Color.BLACK)
            holder.binding.dateText.setTextColor(android.graphics.Color.BLACK)
        } else {
            holder.binding.categoryBadge.text = holder.itemView.context.getString(com.kleos.education.R.string.category_news)
            holder.binding.titleText.setTextColor(android.graphics.Color.BLACK)
            holder.binding.dateText.setTextColor(android.graphics.Color.WHITE)
        }
        
        // Загружаем изображение из imageUrl, если оно есть
        if (!item.imageUrl.isNullOrEmpty()) {
            android.util.Log.d("NewsAdapter", "Item: ${item.title}, Loading image from: ${item.imageUrl}")
            loadImage(holder.binding.backgroundImage, item.imageUrl, holder.binding.overlayView)
        } else {
            android.util.Log.d("NewsAdapter", "No image to load for item: ${item.title}")
        }
        
        // Обработка клика на кнопку со стрелкой
        holder.binding.arrowButton.setOnClickListener {
            AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                AnimationUtils.releaseButton(it)
                if (onItemClick != null) {
                    onItemClick(item)
                }
            }, 150)
        }
        
        // Анимация появления карточки с задержкой
        AnimationUtils.cardEnter(holder.itemView, position * 100L)
        
        // Обработка клика по карточке
        holder.itemView.setOnClickListener {
            AnimationUtils.pressButton(it)
            it.postDelayed({
                AnimationUtils.releaseButton(it)
                // Показываем диалог с деталями новости
                if (onItemClick != null) {
                    onItemClick(item)
                } else {
                    // Fallback: показываем простой диалог
                    val context = holder.itemView.context
                    if (context is FragmentActivity) {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(item.title)
                            .setMessage("${item.dateText}\n\n${item.content ?: "Нет дополнительной информации"}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }, 100)
        }
    }
    
    override fun onViewAttachedToWindow(holder: NewsViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Дополнительная анимация при появлении на экране
        AnimationUtils.fadeInScale(holder.itemView, 300)
    }

    fun submitList(newItems: List<NewsItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    private var imageLoadJobs = mutableMapOf<android.widget.ImageView, Job>()
    
    private fun loadImage(imageView: android.widget.ImageView, imageUrl: String, overlayView: android.view.View) {
        // Отменяем предыдущую загрузку для этого ImageView
        imageLoadJobs[imageView]?.cancel()
        imageLoadJobs.remove(imageView)
        
        // Используем GlobalScope для загрузки изображений, так как адаптер не имеет lifecycle
        val job = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO + kotlinx.coroutines.SupervisorJob()) {
            try {
                if (!isActive) return@launch
                
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
                throw e
            } catch (e: Exception) {
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
        imageView.tag = imageUrl
    }
}



