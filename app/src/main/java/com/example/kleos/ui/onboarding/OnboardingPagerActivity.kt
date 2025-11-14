package com.example.kleos.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.kleos.ui.auth.AuthActivity
import com.example.kleos.R
import com.example.kleos.databinding.ActivityOnboardingPagerBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.example.kleos.ui.language.t

data class OnboardingPage(
    val imageRes: Int,
    val title: String,
    val subtitle: String
)

class OnboardingPagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingPagerBinding
    private lateinit var adapter: OnboardingPagerAdapter

    private val pages by lazy {
        listOf(
            OnboardingPage(
                R.drawable.ic_menu_gallery,
                getString(R.string.ob_title_1),
                getString(R.string.ob_sub_1)
            ),
            OnboardingPage(
                R.drawable.ic_menu_slideshow,
                getString(R.string.ob_title_2),
                getString(R.string.ob_sub_2)
            ),
            OnboardingPage(
                R.drawable.ic_menu_camera,
                getString(R.string.ob_title_3),
                getString(R.string.ob_sub_3)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = OnboardingPagerAdapter(pages)
        binding.pager.adapter = adapter
        binding.pager.offscreenPageLimit = 1
        // Отключаем лишние эффекты и вложенный скролл у внутреннего RecyclerView
        (binding.pager.getChildAt(0) as? RecyclerView)?.let { rv ->
            rv.itemAnimator = null
            rv.overScrollMode = View.OVER_SCROLL_NEVER
            rv.isNestedScrollingEnabled = false
        }
        // Переносим инициализацию индикатора и первичный апдейт на следующую петлю
        binding.pager.post {
            TabLayoutMediator(binding.indicator, binding.pager) { _, _ -> }.attach()
            updateControls(0)
        }

        binding.skip.setOnClickListener { finishFlow() }
        binding.btnNext.setOnClickListener { goNext() }
        binding.btnBack.setOnClickListener { goBack() }

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateControls(position)
            }
        })
    }

    private fun updateControls(position: Int) {
        binding.btnBack.isEnabled = position > 0
        binding.btnBack.alpha = if (binding.btnBack.isEnabled) 1f else 0.5f
        binding.btnNext.text = if (position == pages.lastIndex) this@OnboardingPagerActivity.t(R.string.start) else this@OnboardingPagerActivity.t(R.string.next)
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


