package com.example.kleos.ui.partners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.kleos.databinding.FragmentPartnersBinding
import androidx.lifecycle.lifecycleScope
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.PartnersApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.kleos.ui.language.t

class PartnersFragment : Fragment() {

    private var _binding: FragmentPartnersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartnersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPartners()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun addCard(p: com.example.kleos.data.network.PartnerDto) {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            radius = dp(10).toFloat()
            cardElevation = dp(2).toFloat()
            preventCornerOverlap = true
            useCompatPadding = true
            setContentPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val iv = android.widget.ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(dp(56), dp(56))
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
        }
        val texts = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val lp = ViewGroup.MarginLayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(12)
            lp.width = 0
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = lp
            weightSum = 1f
        }
        val title = com.google.android.material.textview.MaterialTextView(requireContext()).apply {
            text = p.name
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = com.google.android.material.textview.MaterialTextView(requireContext()).apply {
            text = p.description ?: ""
            textSize = 13f
            setTextColor(0xFF666666.toInt())
        }
        texts.addView(title)
        texts.addView(subtitle)
        container.addView(iv)
        container.addView(texts)
        card.addView(container)
        card.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.example.kleos.ui.partners.PartnerDetailActivity::class.java)
            intent.putExtra("id", p.id)
            intent.putExtra("name", p.name)
            intent.putExtra("description", p.description)
            intent.putExtra("logoUrl", p.logoUrl)
            intent.putExtra("url", p.url)
            startActivity(intent)
        }
        binding.partnersContainer.addView(card, ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })
        // Загрузка изображения без сторонних библиотек
        if (!p.logoUrl.isNullOrBlank()) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val conn = java.net.URL(p.logoUrl).openConnection()
                    conn.connectTimeout = 4000
                    conn.readTimeout = 6000
                    conn.getInputStream().use { input ->
                        val bmp = android.graphics.BitmapFactory.decodeStream(input)
                        withContext(Dispatchers.Main) { iv.setImageBitmap(bmp) }
                    }
                } catch (_: Exception) { /* ignore */ }
            }
        }
    }

    private fun loadPartners() {
        binding.partnersContainer.removeAllViews()
        val loading = com.google.android.material.textview.MaterialTextView(requireContext()).apply {
            text = getString(com.example.kleos.R.string.loading)
            textSize = 16f
        }
        binding.partnersContainer.addView(loading)
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val api = ApiClient.retrofit.create(PartnersApi::class.java)
                api.list()
            }.onSuccess { list ->
                withContext(Dispatchers.Main) {
                    binding.partnersContainer.removeAllViews()
                    if (list.isEmpty()) {
                        val tv = com.google.android.material.textview.MaterialTextView(requireContext())
                        tv.text = requireContext().t(com.example.kleos.R.string.no_partners_yet)
                        tv.textSize = 16f
                        binding.partnersContainer.addView(tv)
                    } else {
                        list.forEach { p -> addCard(p) }
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    binding.partnersContainer.removeAllViews()
                    val tv = com.google.android.material.textview.MaterialTextView(requireContext())
                    tv.text = requireContext().t(com.example.kleos.R.string.failed_to_load)
                    tv.textSize = 16f
                    binding.partnersContainer.addView(tv)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


