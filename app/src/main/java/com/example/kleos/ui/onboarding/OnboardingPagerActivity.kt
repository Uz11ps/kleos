package com.example.kleos.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.kleos.ui.auth.AuthActivity
import com.example.kleos.R
import com.example.kleos.databinding.ActivityOnboardingPagerBinding
import com.google.android.material.tabs.TabLayoutMediator

data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val subtitle: String
)

class OnboardingPagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingPagerBinding
    private lateinit var adapter: OnboardingPagerAdapter

    private val pages = listOf(
        OnboardingPage(
            R.drawable.ic_menu_gallery,
            "Discover and explore famed Russian universities",
            "Through our mobile application, you will be able to discover first‑rate & notable Russian universities and all their programs"
        ),
        OnboardingPage(
            R.drawable.ic_menu_slideshow,
            "StudyInRussia Awesome\nTalk Show and Broadcast",
            "The distinctive feature of our mobile application which enables you to watch live talk show & broadcast with leading Russian universities to discuss and obtain the latest"
        ),
        OnboardingPage(
            R.drawable.ic_menu_camera,
            "Forum, the excellent venue for study discussion and social interaction",
            "Forum, the outstanding and designated space created with the intention for you to join, share and post the brilliant ideas in your mind to interact with each other"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = OnboardingPagerAdapter(pages)
        binding.pager.adapter = adapter
        TabLayoutMediator(binding.indicator, binding.pager) { _, _ -> }.attach()

        binding.skip.setOnClickListener { finishFlow() }
        binding.btnNext.setOnClickListener { goNext() }
        binding.btnBack.setOnClickListener { goBack() }

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateControls(position)
            }
        })
        updateControls(0)
    }

    private fun updateControls(position: Int) {
        binding.btnBack.isEnabled = position > 0
        binding.btnBack.alpha = if (binding.btnBack.isEnabled) 1f else 0.5f
        binding.btnNext.text = if (position == pages.lastIndex) getString(R.string.start) else getString(R.string.next)
    }

    private fun goNext() {
        val pos = binding.pager.currentItem
        if (pos < pages.lastIndex) {
            binding.pager.currentItem = pos + 1
        } else {
            finishFlow()
        }
    }

    private fun goBack() {
        val pos = binding.pager.currentItem
        if (pos > 0) {
            binding.pager.currentItem = pos - 1
        }
    }

    @Suppress("DEPRECATION")
    private fun finishFlow() {
        startActivity(Intent(this, AuthActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}


