package com.kleos.education.data.gallery

import com.kleos.education.data.model.GalleryItem
import com.kleos.education.data.network.ApiClient
import com.kleos.education.data.network.GalleryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryRepository {
    private val api = ApiClient.retrofit.create(GalleryApi::class.java)

    suspend fun fetch(): List<GalleryItem> = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = api.list()
            dtos.map { dto ->
                GalleryItem(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description,
                    mediaUrl = dto.mediaUrl,
                    mediaType = dto.mediaType
                )
            }
        }.getOrElse { e ->
            android.util.Log.e("GalleryRepository", "Error fetching gallery", e)
            emptyList()
        }
    }
}


