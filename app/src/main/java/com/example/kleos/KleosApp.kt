package com.example.kleos

import android.app.Application
import android.util.Log
import com.example.kleos.ui.language.LocaleManager
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.UsersApi
import com.example.kleos.data.auth.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KleosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Применяем сохранённую локаль один раз на старте приложения,
        // чтобы не делать тяжёлые операции в каждой Activity.
        LocaleManager.applySavedLocale(this)

        // Инициализируем сетевой клиент с applicationContext для AuthInterceptor
        ApiClient.init(this)
        
        // Получаем и отправляем FCM токен на сервер
        registerFcmToken()
    }
    
    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("KleosApp", "Failed to get FCM token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("KleosApp", "FCM token: $token")
            
            // Отправляем токен на сервер только если пользователь залогинен
            val sessionManager = SessionManager(this)
            if (sessionManager.isLoggedIn() && !token.isNullOrBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val api = ApiClient.retrofit.create(com.example.kleos.data.network.UsersApi::class.java)
                        val response = api.saveFcmToken(com.example.kleos.data.network.FcmTokenRequest(token))
                        if (response.ok == true) {
                            Log.d("KleosApp", "FCM token saved successfully")
                        } else {
                            Log.w("KleosApp", "Failed to save FCM token: ${response.error}")
                        }
                    } catch (e: Exception) {
                        Log.e("KleosApp", "Error saving FCM token", e)
                    }
                }
            }
        }
    }
}



