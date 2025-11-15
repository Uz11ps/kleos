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
        val tuition = intent.getStringExtra("tuition").orEmpty()
        val duration = intent.getStringExtra("duration").orEmpty()

        binding.titleText.text = title
        binding.universityText.text = university
        binding.tuitionText.text = tuition
        binding.durationText.text = duration
        binding.descriptionText.text = description

        binding.applyButton.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra("prefill_program", title)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }
    }
}



