package com.kleos.education.data.network

import android.content.Context
import com.kleos.education.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val logging: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private class AuthInterceptor(private val context: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val sessionManager = com.kleos.education.data.auth.SessionManager(context)
            val token = try {
                sessionManager.getToken()
            } catch (_: Throwable) { null }
            
            // Не отправляем заголовок, если пользователь — гость (у гостя токен — это UUID, а не JWT)
            val currentUser = sessionManager.getCurrentUser()
            val isGuest = currentUser?.email == "guest@local"
            
            val req = if (!token.isNullOrBlank() && !isGuest) {
                original.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else original
            return chain.proceed(req)
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS) // Увеличено с 15 до 30
            .readTimeout(60, TimeUnit.SECONDS) // Увеличено с 30 до 60
            .writeTimeout(60, TimeUnit.SECONDS) // Увеличено с 30 до 60
        // Всегда добавляем AuthInterceptor, используя appContext или fallback
        val context = appContext ?: throw IllegalStateException("ApiClient not initialized. Call ApiClient.init(context) first.")
        builder.addInterceptor(AuthInterceptor(context))
        builder.build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}


