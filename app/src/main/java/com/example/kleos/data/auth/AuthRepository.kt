package com.example.kleos.data.auth

import android.content.Context
import com.example.kleos.data.model.User

interface AuthRepository {
    fun isLoggedIn(): Boolean
    fun currentUser(): User?
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(fullName: String, email: String, password: String): Result<User>
    fun logout()

    class Local(context: Context) : AuthRepository {
        private val sessionManager = SessionManager(context)

        override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

        override fun currentUser(): User? = sessionManager.getCurrentUser()

        override suspend fun login(email: String, password: String): Result<User> {
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Email и пароль обязательны"))
            }
            val existing = sessionManager.getCurrentUser()
            val fullName = existing?.fullName ?: email.substringBefore("@")
            sessionManager.saveUser(fullName = fullName, email = email)
            sessionManager.saveToken("local_token")
            return Result.success(sessionManager.getCurrentUser()!!)
        }

        override suspend fun register(fullName: String, email: String, password: String): Result<User> {
            if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Заполните все поля"))
            }
            sessionManager.saveUser(fullName = fullName, email = email)
            sessionManager.saveToken("local_token")
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

        override suspend fun login(email: String, password: String): Result<User> = runCatching {
            val resp = api.login(com.example.kleos.data.network.LoginRequest(email, password))
            sessionManager.saveToken(resp.token)
            sessionManager.saveUser(resp.user.fullName, resp.user.email)
            sessionManager.saveRole(resp.user.role)
            sessionManager.getCurrentUser()!!
        }

        override suspend fun register(fullName: String, email: String, password: String): Result<User> = runCatching {
            val resp = api.register(com.example.kleos.data.network.RegisterRequest(fullName, email, password))
            // If backend returned AuthResponse-like map, handle it; otherwise expect requiresVerification=true
            val token = (resp["token"] as? String)
            val userMap = resp["user"] as? Map<*, *>
            return@runCatching if (token != null && userMap != null) {
                val uFullName = userMap["fullName"] as? String ?: fullName
                val uEmail = userMap["email"] as? String ?: email
                sessionManager.saveToken(token)
                sessionManager.saveUser(uFullName, uEmail)
                sessionManager.getCurrentUser()!!
            } else {
                // Save minimal local info (without token) so we can show it in UI if needed
                // isLoggedIn() will still be false until verification consumes token
                sessionManager.saveUser(fullName, email)
                User(id = "pending", fullName = fullName, email = email)
            }
        }

        override fun logout() {
            sessionManager.logout()
        }
    }
}

