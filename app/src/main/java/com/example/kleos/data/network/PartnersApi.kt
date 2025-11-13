package com.example.kleos.data.network

import retrofit2.http.GET

data class PartnerDto(
    val id: String,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val url: String?
)

interface PartnersApi {
    @GET("partners")
    suspend fun list(): List<PartnerDto>
}


