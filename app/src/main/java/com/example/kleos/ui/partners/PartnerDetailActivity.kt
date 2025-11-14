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
        val logoUrl = intent.getStringExtra("logoUrl")
        val siteUrl = intent.getStringExtra("url")

        binding.title.text = name
        binding.description.text = description
        if (!logoUrl.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val stream = URL(logoUrl).openStream()
                    val bmp = BitmapFactory.decodeStream(stream)
                    withContext(Dispatchers.Main) {
                        binding.logo.setImageBitmap(bmp)
                    }
                } catch (_: Exception) {}
            }
        }
        binding.openSiteButton.setOnClickListener {
            try {
                val i = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(siteUrl))
                startActivity(i)
            } catch (_: Exception) {
                Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            }
        }
        binding.backButton.setOnClickListener { finish() }
    }
}


