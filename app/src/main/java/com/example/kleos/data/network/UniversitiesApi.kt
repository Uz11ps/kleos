package com.example.kleos.data.network

import retrofit2.http.GET
import retrofit2.http.Path

data class UniversityDto(
    val id: String,
    val name: String,
    val city: String?,
    val country: String?,
    val description: String?,
    val website: String?,
    val logoUrl: String?
)

interface UniversitiesApi {
    @GET("universities")
    suspend fun list(): List<UniversityDto>

    @GET("universities/{id}")
    suspend fun get(@Path("id") id: String): UniversityDto
}

