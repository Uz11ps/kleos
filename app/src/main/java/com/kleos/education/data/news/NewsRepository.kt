package com.kleos.education.data.news

import com.kleos.education.data.model.NewsItem
import com.kleos.education.data.network.ApiClient
import com.kleos.education.data.network.NewsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NewsRepository {
    private val api = ApiClient.retrofit.create(NewsApi::class.java)

    suspend fun fetch(): List<NewsItem> = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = api.list()
            dtos.map { dto ->
                NewsItem(
                    id = dto.id,
                    title = dto.title,
                    dateText = formatDate(dto.publishedAt),
                    content = dto.content,
                    imageUrl = dto.imageUrl
                )
            }
        }.getOrElse { e ->
            android.util.Log.e("NewsRepository", "Error fetching news", e)
            emptyList()
        }
    }

    suspend fun get(id: String): NewsItem? = withContext(Dispatchers.IO) {
        runCatching {
            val dto = api.get(id)
            NewsItem(
                id = dto.id,
                title = dto.title,
                dateText = formatDate(dto.publishedAt),
                content = dto.content,
                imageUrl = dto.imageUrl
            )
        }.getOrNull()
    }

    private fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        // Try ISO8601 → "dd.MM.yyyy" (01.01.2000)
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val d = inFmt.parse(iso)
            if (d != null) outFmt.format(d) else {
                // Если не удалось распарсить, пробуем извлечь дату из строки
                if (iso.length >= 10) {
                    val datePart = iso.substring(0, 10)
                    datePart.replace("-", ".")
                } else {
                    iso.substring(0, minOf(10, iso.length))
                }
            }
        } catch (_: Exception) {
            try {
                val inFmt2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val outFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val d = inFmt2.parse(iso)
                if (d != null) outFmt.format(d) else {
                    if (iso.length >= 10) {
                        val datePart = iso.substring(0, 10)
                        datePart.replace("-", ".")
                    } else {
                        iso.substring(0, minOf(10, iso.length))
                    }
                }
            } catch (_: Exception) {
                // Если не удалось распарсить, пробуем извлечь дату из строки
                if (iso.length >= 10) {
                    val datePart = iso.substring(0, 10)
                    datePart.replace("-", ".")
                } else {
                    iso.substring(0, minOf(10, iso.length))
                }
            }
        }
    }
}


