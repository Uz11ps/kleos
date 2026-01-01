package com.kleos.education.data.network

import retrofit2.http.GET
import retrofit2.http.Path

data class SocialLinksDto(
    val facebook: String? = null,
    val twitter: String? = null,
    val instagram: String? = null,
    val youtube: String? = null,
    val whatsapp: String? = null,
    val phone: String? = null,
    val email: String? = null
)

data class DegreeProgramDto(
    val type: String,
    val description: String?
)

data class ContentBlockDto(
    val type: String,
    val content: String?,
    val order: Int?
)

data class UniversityDto(
    val id: String,
    val name: String,
    val city: String?,
    val country: String?,
    val description: String?,
    val website: String?,
    val logoUrl: String?,
    val socialLinks: SocialLinksDto? = null,
    val degreePrograms: List<DegreeProgramDto>? = null,
    val contentBlocks: List<ContentBlockDto>? = null
)

interface UniversitiesApi {
    @GET("universities")
    suspend fun list(): List<UniversityDto>

    @GET("universities/{id}")
    suspend fun get(@Path("id") id: String): UniversityDto
}


