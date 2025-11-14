package com.example.kleos.ui.verify

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.MainActivity
import com.example.kleos.data.auth.SessionManager
import com.example.kleos.databinding.ActivityVerifyEmailBinding
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.AuthApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyEmailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent?.getStringExtra("email").orEmpty()
        binding.emailText.text = email

        binding.openMailButton.setOnClickListener {
            // Попытка открыть почтовый клиент
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("mailto:")
                }
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Нет установленного почтового клиента", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backToLoginButton.setOnClickListener {
            finish()
        }

        binding.resendButton.setOnClickListener {
            if (email.isBlank()) {
                Toast.makeText(this, "Email не указан", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val api = ApiClient.retrofit.create(AuthApi::class.java)
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        api.resendVerify(mapOf("email" to email))
                    }
                    Toast.makeText(this@VerifyEmailActivity, "Письмо отправлено повторно", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@VerifyEmailActivity, "Не удалось отправить письмо", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Если пользователь уже подтвердил email (JWT сохранён) — уводим на главный экран
        val session = SessionManager(this)
        if (session.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }
}


