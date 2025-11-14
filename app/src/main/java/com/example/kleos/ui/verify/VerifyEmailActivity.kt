package com.example.kleos.ui.verify

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.databinding.ActivityVerifyEmailBinding

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
    }
}


