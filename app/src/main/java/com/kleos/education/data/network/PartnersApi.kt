package com.kleos.education.data.network

import retrofit2.http.GET

data class PartnerDto(
    val id: String,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val url: String?,
    val city: String?,
    val country: String?,
    val contactEmail: String?,
    val contactPhone: String?
)

interface PartnersApi {
    @GET("partners")
    suspend fun list(): List<PartnerDto>
}


