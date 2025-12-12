package com.example.kleos.ui.language

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.MainActivity
import com.example.kleos.databinding.ActivityLanguageBinding

class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        com.example.kleos.ui.language.LocaleManager.applySavedLocale(this)
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Анимации появления элементов
        com.example.kleos.ui.utils.AnimationUtils.bounceIn(binding.root.findViewById(com.example.kleos.R.id.btnRu), 600)
        com.example.kleos.ui.utils.AnimationUtils.bounceIn(binding.root.findViewById(com.example.kleos.R.id.btnEn), 700)
        com.example.kleos.ui.utils.AnimationUtils.bounceIn(binding.root.findViewById(com.example.kleos.R.id.btnZh), 800)
        
        // Анимации для кнопок при нажатии
        binding.btnRu.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.example.kleos.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.example.kleos.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }
        
        binding.btnEn.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.example.kleos.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.example.kleos.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }
        
        binding.btnZh.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.example.kleos.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.example.kleos.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }

        binding.btnRu.setOnClickListener { 
            com.example.kleos.ui.utils.AnimationUtils.shake(binding.btnRu)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                setLangAndStart("ru")
            }, 300)
        }
        binding.btnEn.setOnClickListener { 
            com.example.kleos.ui.utils.AnimationUtils.shake(binding.btnEn)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                setLangAndStart("en")
            }, 300)
        }
        binding.btnZh.setOnClickListener { 
            com.example.kleos.ui.utils.AnimationUtils.shake(binding.btnZh)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                setLangAndStart("zh")
            }, 300)
        }
    }

    private fun setLangAndStart(code: String) {
        LocaleManager.setLocale(this, code)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        overridePendingTransition(com.example.kleos.R.anim.scale_in, com.example.kleos.R.anim.fade_out)
    }
}


