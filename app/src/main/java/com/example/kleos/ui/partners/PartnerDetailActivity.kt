package com.example.kleos.ui.partners

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.R
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

        // Устанавливаем цвет статус-бара для однородного фона
        window.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)

        val name = intent.getStringExtra("name").orEmpty()
        val description = intent.getStringExtra("description").orEmpty()
        val siteUrl = intent.getStringExtra("url")

        // Устанавливаем название партнера
        binding.title.text = name.ifBlank { getString(R.string.partner_title) }
        
        // Устанавливаем описание партнера
        binding.description.text = description.ifBlank { getString(R.string.partner_description) }
        
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
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            // В отдельной Activity кнопка меню просто закрывает экран
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Устанавливаем цвет статус-бара при возврате на экран
        window.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Восстанавливаем цвет статус-бара
        window.statusBarColor = resources.getColor(com.example.kleos.R.color.dark_background, null)
    }
}


