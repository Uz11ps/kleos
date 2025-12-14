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

    private fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        // Try ISO8601 → "MMMM d, yyyy"
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outFmt = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val d = inFmt.parse(iso)
            if (d != null) outFmt.format(d) else iso.substring(0, minOf(10, iso.length))
        } catch (_: Exception) {
            try {
                val inFmt2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val outFmt = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                val d = inFmt2.parse(iso)
                if (d != null) outFmt.format(d) else iso.substring(0, minOf(10, iso.length))
            } catch (_: Exception) {
                iso.substring(0, minOf(10, iso.length))
            }
        }
    }
}

