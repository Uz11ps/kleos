package com.kleos.education.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST

data class UserProfileDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val phone: String?,
    val course: String?,
    val speciality: String?,
    val status: String?,
    val university: String?,
    val payment: String?,
    val penalties: String?,
    val notes: String?,
    val studentId: String?,
    val emailVerified: Boolean,
    val avatarUrl: String? = null
)

data class UpdateProfileRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val course: String? = null,
    val speciality: String? = null,
    val status: String? = null,
    val university: String? = null,
    val payment: String? = null,
    val penalties: String? = null,
    val notes: String? = null
)

data class FcmTokenRequest(
    val token: String
)

data class ApiResponse(
    val ok: Boolean?,
    val error: String?
)

interface UsersApi {
    @GET("users/me")
    suspend fun getProfile(): UserProfileDto

    @PUT("users/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Map<String, Any?>
    
    @POST("users/fcm-token")
    suspend fun saveFcmToken(@Body request: FcmTokenRequest): ApiResponse
}

