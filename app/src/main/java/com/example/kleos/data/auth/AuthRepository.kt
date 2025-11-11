package com.example.kleos.data.auth

import android.content.Context
import com.example.kleos.data.model.User

interface AuthRepository {
    fun isLoggedIn(): Boolean
    fun currentUser(): User?
    fun login(email: String, password: String): Result<User>
    fun register(fullName: String, email: String, password: String): Result<User>
    fun logout()

    class Local(context: Context) : AuthRepository {
        private val sessionManager = SessionManager(context)

        override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

        override fun currentUser(): User? = sessionManager.getCurrentUser()

        override fun login(email: String, password: String): Result<User> {
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Email и пароль обязательны"))
            }
            val existing = sessionManager.getCurrentUser()
            val fullName = existing?.fullName ?: email.substringBefore("@")
            sessionManager.saveUser(fullName = fullName, email = email)
            return Result.success(sessionManager.getCurrentUser()!!)
        }

        override fun register(fullName: String, email: String, password: String): Result<User> {
            if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Заполните все поля"))
            }
            sessionManager.saveUser(fullName = fullName, email = email)
            return Result.success(sessionManager.getCurrentUser()!!)
        }

        override fun logout() {
            sessionManager.logout()
        }
    }

    class Http(private val context: Context) : AuthRepository {
        private val sessionManager = SessionManager(context)
        private val api = com.example.kleos.data.network.ApiClient.retrofit
            .create(com.example.kleos.data.network.AuthApi::class.java)

        override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

        override fun currentUser(): User? = sessionManager.getCurrentUser()

        override fun login(email: String, password: String): Result<User> = runCatching {
            val resp = kotlinx.coroutines.runBlocking {
                api.login(com.example.kleos.data.network.LoginRequest(email, password))
            }
            sessionManager.saveToken(resp.token)
            sessionManager.saveUser(resp.user.fullName, resp.user.email)
            sessionManager.getCurrentUser()!!
        }

        override fun register(fullName: String, email: String, password: String): Result<User> = runCatching {
            val resp = kotlinx.coroutines.runBlocking {
                api.register(com.example.kleos.data.network.RegisterRequest(fullName, email, password))
            }
            sessionManager.saveToken(resp.token)
            sessionManager.saveUser(resp.user.fullName, resp.user.email)
            sessionManager.getCurrentUser()!!
        }

        override fun logout() {
            sessionManager.logout()
        }
    }
}


