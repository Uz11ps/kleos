package com.example.kleos.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class ProgramDto(
    val id: String,
    val title: String,
    val slug: String,
    val description: String?,
    val language: String?,
    val level: String?,
    val university: String?,
    val tuition: Double?,
    val durationMonths: Int?,
    val imageUrl: String?
)

interface ProgramsApi {
    @GET("programs")
    suspend fun list(
        @Query("q") q: String? = null,
        @Query("language") language: String? = null,
        @Query("level") level: String? = null,
        @Query("university") university: String? = null
    ): List<ProgramDto>

    @GET("programs/{id}")
    suspend fun get(@Path("id") id: String): ProgramDto
}

