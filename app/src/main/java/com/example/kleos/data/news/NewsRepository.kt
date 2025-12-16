package com.example.kleos.data.news

import com.example.kleos.data.model.NewsItem
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.NewsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NewsRepository {
    private val api = ApiClient.retrofit.create(NewsApi::class.java)

    suspend fun fetch(): List<NewsItem> = withContext(Dispatchers.IO) {
        val dtos = api.list()
        return@withContext dtos.map { dto ->
            NewsItem(
                id = dto.id,
                title = dto.title,
                dateText = formatDate(dto.publishedAt),
                content = dto.content,
                imageUrl = dto.imageUrl
            )
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
        // Try ISO8601 → "d MMMM" (14 декабря) for Russian, "d MMMM" for English
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val locale = Locale.getDefault()
            // Format: "14 декабря" for Russian, "14 December" for English
            val outFmt = if (locale.language == "ru") {
                SimpleDateFormat("d MMMM", locale)
            } else {
                SimpleDateFormat("d MMMM", locale)
            }
            val d = inFmt.parse(iso)
            if (d != null) outFmt.format(d) else iso.substring(0, minOf(10, iso.length))
        } catch (_: Exception) {
            try {
                val inFmt2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val locale = Locale.getDefault()
                val outFmt = if (locale.language == "ru") {
                    SimpleDateFormat("d MMMM", locale)
                } else {
                    SimpleDateFormat("d MMMM", locale)
                }
                val d = inFmt2.parse(iso)
                if (d != null) outFmt.format(d) else iso.substring(0, minOf(10, iso.length))
            } catch (_: Exception) {
                iso.substring(0, minOf(10, iso.length))
            }
        }
    }
}

