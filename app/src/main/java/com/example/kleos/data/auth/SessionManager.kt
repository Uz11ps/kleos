package com.example.kleos.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.kleos.data.model.User
import java.util.UUID
import java.security.SecureRandom

class SessionManager(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean {
        return preferences.contains(KEY_USER_EMAIL) && !getToken().isNullOrEmpty()
    }

    fun getCurrentUser(): User? {
        val email = preferences.getString(KEY_USER_EMAIL, null) ?: return null
        val fullName = preferences.getString(KEY_USER_FULL_NAME, "") ?: ""
        var id = preferences.getString(KEY_USER_ID, null)
        if (id.isNullOrBlank()) {
            id = generateNumericId()
            preferences.edit().putString(KEY_USER_ID, id).apply()
        }
        return User(id = id, fullName = fullName, email = email)
    }

    fun saveUser(fullName: String, email: String) {
        val id = preferences.getString(KEY_USER_ID, null) ?: generateNumericId()
        preferences.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_FULL_NAME, fullName)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    private fun generateNumericId(length: Int = 6): String {
        val random = SecureRandom()
        val bound = Math.pow(10.0, length.toDouble()).toInt() // 10^length
        val value = random.nextInt(bound)
        return String.format("%0${length}d", value)
    }

    fun saveToken(token: String) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(): String? {
        return preferences.getString(KEY_TOKEN, null)
    }

    fun logout() {
        preferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_FULL_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_TOKEN)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "kleos_session_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_TOKEN = "auth_token"
    }
}


