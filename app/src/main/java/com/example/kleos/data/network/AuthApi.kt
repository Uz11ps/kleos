package com.example.kleos.data.network

import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(val fullName: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthUserDto(val id: String, val fullName: String, val email: String, val role: String)
data class AuthResponse(val token: String, val user: AuthUserDto)

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse
}


