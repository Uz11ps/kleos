package com.kleos.education.data.gallery

import com.kleos.education.data.model.GalleryItem
import com.kleos.education.data.network.ApiClient
import com.kleos.education.data.network.GalleryApi
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


