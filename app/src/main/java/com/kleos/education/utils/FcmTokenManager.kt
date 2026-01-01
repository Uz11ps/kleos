package com.kleos.education.utils

import android.content.Context
import android.util.Log
import com.kleos.education.data.auth.SessionManager
import com.kleos.education.data.network.ApiClient
import com.kleos.education.data.network.FcmTokenRequest
import com.kleos.education.data.network.UsersApi
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmTokenManager {
    private const val TAG = "FcmTokenManager"
    
    /**
     * Получить и отправить FCM токен на сервер
     */
    fun registerToken(context: Context) {
        val sessionManager = SessionManager(context)
        
        // Отправляем токен только если пользователь залогинен
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping FCM token registration")
            return
        }
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Failed to get FCM token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            if (token.isNullOrBlank()) {
                Log.w(TAG, "FCM token is empty")
                return@addOnCompleteListener
            }
            
            Log.d(TAG, "FCM token received: ${token.take(20)}...")
            
            // Отправляем токен на сервер
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val api = ApiClient.retrofit.create(UsersApi::class.java)
                    val response = api.saveFcmToken(FcmTokenRequest(token))
                    if (response.ok == true) {
                        Log.d(TAG, "FCM token saved successfully")
                    } else {
                        Log.w(TAG, "Failed to save FCM token: ${response.error}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving FCM token", e)
                }
            }
        }
    }
    
    /**
     * Обновить FCM токен (вызывается при изменении токена)
     */
    fun refreshToken(context: Context) {
        registerToken(context)
    }
}


