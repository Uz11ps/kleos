package com.kleos.education.ui.language

import android.content.Context
import androidx.annotation.StringRes
import com.kleos.education.data.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.ConcurrentHashMap

interface I18nApi {
    @GET("i18n/{lang}")
    suspend fun get(@Path("lang") lang: String): Map<String, String>
}

object TranslationManager {
    private val overrides = ConcurrentHashMap<String, String>()

    fun initAsync(context: Context, lang: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val api = ApiClient.retrofit.create(I18nApi::class.java)
                val map = api.get(lang)
                overrides.clear()
                overrides.putAll(map)
            }
        }
    }

    fun getOverride(context: Context, @StringRes id: Int): String {
        return try {
            val key = context.resources.getResourceEntryName(id)
            overrides[key] ?: context.getString(id)
        } catch (_: Exception) {
            context.getString(id)
        }
    }
}

fun Context.t(@StringRes id: Int): String = TranslationManager.getOverride(this, id)



