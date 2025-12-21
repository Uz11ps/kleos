package com.example.kleos.ui.programs

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.MainActivity
import com.example.kleos.databinding.ActivityProgramDetailBinding

class ProgramDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProgramDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgramDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("title").orEmpty()
        val description = intent.getStringExtra("description").orEmpty()
        val university = intent.getStringExtra("university").orEmpty()
        val tuition = intent.getStringExtra("tuition")
        val duration = intent.getStringExtra("duration")
        val language = intent.getStringExtra("language")
        val level = intent.getStringExtra("level")
        val durationYearsValue = intent.getDoubleExtra("durationYears", 0.0)

        // Заголовок "Программа" всегда одинаковый
        binding.titleText.text = "Программа"
        
        // Название университета
        binding.universityText.text = university.ifBlank { "Название универа" }
        
        // Получаем элементы напрямую из layout
        val languageText = binding.languageText
        val levelText = binding.levelText
        val durationText = binding.durationText
        val tuitionText = binding.tuitionText
        val applyButton = binding.applyButton
        
        // Язык программы
        languageText.text = language?.ifBlank { "English" } ?: "English"
        
        // Степень (level)
        levelText.text = when {
            level.isNullOrBlank() -> "Бакалавриат"
            level.contains("Bachelor", ignoreCase = true) -> "Бакалавриат"
            level.contains("Master", ignoreCase = true) -> "Магистратура"
            level.contains("Research", ignoreCase = true) -> "Докторантура"
            level.contains("Speciality", ignoreCase = true) -> "Специалитет"
            else -> level
        }
        
        // Длительность в годах
        val durationYearsFormatted = if (durationYearsValue > 0) {
            val years = durationYearsValue.toInt()
            val hasHalfYear = durationYearsValue % 1.0 >= 0.5
            when {
                hasHalfYear && years > 0 -> "${years}.5 ${getYearWord(years)}"
                years > 0 -> "$years ${getYearWord(years)}"
                else -> "${durationYearsValue} ${getYearWord(1)}"
            }
        } else {
            duration?.ifBlank { "4 года" } ?: "4 года"
        }
        durationText.text = durationYearsFormatted
        
        // Стоимость
        val tuitionFormatted = if (!tuition.isNullOrBlank()) {
            try {
                val amount = tuition.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    "от $${String.format("%.0f", amount)} / год"
                } else {
                    "от $5,000 / год"
                }
            } catch (e: Exception) {
                if (tuition.contains("$")) tuition else "от $5,000 / год"
            }
        } else {
            "от $5,000 / год"
        }
        tuitionText.text = tuitionFormatted

        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            finish()
        }

        // Обработка кнопки "Подать заявку"
        applyButton.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra("prefill_program", title)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }
    }
    
    private fun getYearWord(years: Int): String {
        return when {
            years % 10 == 1 && years % 100 != 11 -> "год"
            years % 10 in 2..4 && years % 100 !in 12..14 -> "года"
            else -> "лет"
        }
    }
}



