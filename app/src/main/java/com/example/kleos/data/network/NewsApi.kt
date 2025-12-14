package com.example.kleos.data.network

import retrofit2.http.GET

data class NewsDto(
    val id: String,
    val title: String,
    val content: String?,
    val imageUrl: String?,
    val publishedAt: String
)

interface NewsApi {
    @GET("news")
    suspend fun list(): List<NewsDto>
    
    @GET("news/{id}")
    suspend fun get(@retrofit2.http.Path("id") id: String): NewsDto
}

