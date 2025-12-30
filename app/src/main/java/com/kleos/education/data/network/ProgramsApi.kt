package com.kleos.education.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class ProgramDto(
    val id: String,
    val title: String,
    val description: String?,
    val language: String?,
    val level: String?, // "Bachelor's degree", "Master's degree", "Research degree", "Speciality degree"
    val university: String?, // Legacy field for backward compatibility
    val universityId: String?, // New field linking to University model
    val tuition: Double?,
    val durationYears: Double?, // Changed from durationMonths to durationYears
    val active: Boolean?,
    val order: Int?
)

interface ProgramsApi {
    @GET("programs")
    suspend fun list(
        @Query("q") q: String? = null,
        @Query("language") language: String? = null,
        @Query("level") level: String? = null,
        @Query("university") university: String? = null,
        @Query("universityId") universityId: String? = null
    ): List<ProgramDto>

    @GET("programs/{id}")
    suspend fun get(@Path("id") id: String): ProgramDto
}


