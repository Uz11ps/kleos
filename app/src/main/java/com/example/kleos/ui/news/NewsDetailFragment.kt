package com.example.kleos.ui.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kleos.databinding.FragmentNewsDetailBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.kleos.data.news.NewsRepository
import java.text.SimpleDateFormat
import java.util.Locale

class NewsDetailFragment : Fragment() {

    private var _binding: FragmentNewsDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)
        
        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.example.kleos.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
        
        val newsId = arguments?.getString("newsId") ?: ""
        
        if (newsId.isNotEmpty()) {
            loadNewsDetail(newsId)
        } else {
            // Fallback: use arguments if passed directly
            val title = arguments?.getString("title") ?: ""
            val content = arguments?.getString("content") ?: ""
            val dateText = arguments?.getString("dateText") ?: ""
            val imageUrl = arguments?.getString("imageUrl") ?: ""
            
            displayNews(title, content, dateText, imageUrl)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Устанавливаем цвет статус-бара при возврате на экран
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)
    }
    
    private fun loadNewsDetail(newsId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val newsItem = withContext(Dispatchers.IO) {
                runCatching {
                    val repo = NewsRepository()
                    repo.get(newsId)
                }.getOrNull()
            }
            
            if (newsItem != null) {
                displayNews(
                    newsItem.title,
                    newsItem.content ?: "",
                    newsItem.dateText,
                    newsItem.imageUrl ?: ""
                )
            }
        }
    }
    
    private fun displayNews(title: String, content: String, dateText: String, imageUrl: String) {
        binding.titleText.text = title
        binding.contentText.text = content.ifBlank { "Нет дополнительной информации" }
        binding.dateText.text = dateText
        
        // Load image if available
        if (imageUrl.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val conn = java.net.URL(imageUrl).openConnection()
                    conn.connectTimeout = 4000
                    conn.readTimeout = 6000
                    conn.getInputStream().use { input ->
                        val bmp = android.graphics.BitmapFactory.decodeStream(input)
                        withContext(Dispatchers.Main) {
                            binding.imageView.setImageBitmap(bmp)
                            binding.imageView.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.imageView.visibility = View.GONE
                    }
                }
            }
        } else {
            binding.imageView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Восстанавливаем цвет статус-бара
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.dark_background, null)
        _binding = null
    }
}

