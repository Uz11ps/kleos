package com.example.kleos.data.gallery

import com.example.kleos.data.model.GalleryItem
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.GalleryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryRepository {
    private val api = ApiClient.retrofit.create(GalleryApi::class.java)

    suspend fun fetch(): List<GalleryItem> = withContext(Dispatchers.IO) {
        val dtos = api.list()
        return@withContext dtos.map { dto ->
            GalleryItem(
                id = dto.id,
                title = dto.title,
                description = dto.description,
                mediaUrl = dto.mediaUrl,
                mediaType = dto.mediaType
            )
        }
    }
}

