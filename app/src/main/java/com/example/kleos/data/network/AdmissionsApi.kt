package com.example.kleos.data.network

import retrofit2.http.Body
import retrofit2.http.POST

data class AdmissionRequest(
    val fullName: String,
    val phone: String,
    val email: String,
    val program: String,
    val comment: String?
)

data class AdmissionResponse(val ok: Boolean)

interface AdmissionsApi {
    @POST("admissions")
    suspend fun create(@Body body: AdmissionRequest): AdmissionResponse
}


