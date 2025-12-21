package com.example.kleos.ui.partners

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.databinding.ActivityPartnerDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class PartnerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPartnerDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPartnerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra("name").orEmpty()
        val description = intent.getStringExtra("description").orEmpty()
        val siteUrl = intent.getStringExtra("url")

        // Устанавливаем название партнера
        binding.title.text = name.ifBlank { "Партнер" }
        
        // Устанавливаем описание партнера
        binding.description.text = description.ifBlank { "Описание партнера" }
        
        // Обработка кнопки "Открыть сайт"
        binding.openSiteButton.setOnClickListener {
            if (!siteUrl.isNullOrBlank()) {
                try {
                    val i = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(siteUrl))
                    startActivity(i)
                } catch (_: Exception) {
                    Toast.makeText(this, "Неверный URL", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Сайт недоступен", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Обработка кнопки назад
        binding.backButton.setOnClickListener { 
            finish() 
        }
    }
}


