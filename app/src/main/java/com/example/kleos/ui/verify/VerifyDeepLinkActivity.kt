package com.example.kleos.ui.verify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.MainActivity
import com.example.kleos.data.auth.SessionManager
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.AuthApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyDeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Uri? = intent?.data
        val host = data?.host.orEmpty()
        if (host == "verified") {
            // Приложение вызвано из HTML после успешного POST /auth/verify/consume — принимаем JWT
            val jwt = data?.getQueryParameter("jwt")
            if (jwt.isNullOrEmpty()) {
                Toast.makeText(this, "Missing session", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            val session = SessionManager(this@VerifyDeepLinkActivity)
            // email и имя на этом шаге неизвестны — оставим как было
            session.saveToken(jwt)
            startActivity(Intent(this@VerifyDeepLinkActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
            return
        }

        val token = data?.getQueryParameter("token")
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Missing token", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        val api = ApiClient.retrofit.create(AuthApi::class.java)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = api.verifyConsume(mapOf("token" to token))
                val session = SessionManager(this@VerifyDeepLinkActivity)
                session.saveToken(resp.token)
                session.saveUser(resp.user.fullName, resp.user.email)
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@VerifyDeepLinkActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VerifyDeepLinkActivity, "Verification failed", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}


