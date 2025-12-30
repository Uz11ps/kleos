package com.kleos.education.data.network

import retrofit2.http.GET

data class GalleryItemDto(
    val id: String,
    val title: String,
    val description: String?,
    val mediaUrl: String,
    val mediaType: String,
    val createdAt: String
)

interface GalleryApi {
    @GET("gallery")
    suspend fun list(): List<GalleryItemDto>
}


