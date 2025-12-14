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
        
        // Получаем и отправляем FCM токен на сервер (если пользователь залогинен)
        com.example.kleos.utils.FcmTokenManager.registerToken(this)
        
        // Слушаем обновления токена
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            com.example.kleos.utils.FcmTokenManager.refreshToken(this)
        }
    }
}



