package com.example.kleos.ui.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleos.data.news.NewsRepository
import com.example.kleos.databinding.FragmentNewsBinding
import com.example.kleos.ui.home.NewsAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.kleos.R

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    private var currentTab = "all" // all, news, interesting
    private lateinit var adapter: NewsAdapter
    private val newsRepository = NewsRepository()
    private var allItems = emptyList<com.example.kleos.data.model.NewsItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.example.kleos.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
        
        setupTabs()
        setupRecyclerView()
        loadNews()
    }

    private fun setupTabs() {
        updateTabStyles("all")

        binding.tabAll.setOnClickListener {
            currentTab = "all"
            updateTabStyles("all")
            filterItems()
        }

        binding.tabNews.setOnClickListener {
            currentTab = "news"
            updateTabStyles("news")
            filterItems()
        }

        binding.tabInteresting.setOnClickListener {
            currentTab = "interesting"
            updateTabStyles("interesting")
            filterItems()
        }
    }

    private fun updateTabStyles(activeTab: String) {
        // Белый цвет с прозрачностью 26% (#42FFFFFF)
        val inactiveBackgroundColor = android.graphics.Color.parseColor("#42FFFFFF")
        // Белый цвет без прозрачности для активного элемента
        val activeBackgroundColor = android.graphics.Color.WHITE
        // Черный цвет для активного текста
        val activeTextColor = android.graphics.Color.parseColor("#0E080F")
        // Белый цвет для неактивного текста
        val inactiveTextColor = android.graphics.Color.WHITE
        
        // Кнопка "Все"
        val tabAllBgColor = if (activeTab == "all") activeBackgroundColor else inactiveBackgroundColor
        binding.tabAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tabAllBgColor))
        binding.tabAll.setTextColor(
            if (activeTab == "all") activeTextColor else inactiveTextColor
        )
        
        // Кнопка "Новости"
        binding.tabNews.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            if (activeTab == "news") activeBackgroundColor else inactiveBackgroundColor
        ))
        binding.tabNews.setTextColor(
            if (activeTab == "news") activeTextColor else inactiveTextColor
        )
        
        // Кнопка "Интересное"
        binding.tabInteresting.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            if (activeTab == "interesting") activeBackgroundColor else inactiveBackgroundColor
        ))
        binding.tabInteresting.setTextColor(
            if (activeTab == "interesting") activeTextColor else inactiveTextColor
        )
    }

    private fun setupRecyclerView() {
        adapter = NewsAdapter(emptyList()) { item ->
            // Переход на детальную страницу новости
            val bundle = Bundle().apply {
                putString("newsId", item.id)
                putString("title", item.title)
                putString("content", item.content ?: "")
                putString("dateText", item.dateText)
                putString("imageUrl", item.imageUrl ?: "")
            }
            findNavController().navigate(
                com.example.kleos.R.id.newsDetailFragment,
                bundle
            )
        }
        binding.newsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecycler.adapter = adapter
    }

    private fun loadNews() {
        viewLifecycleOwner.lifecycleScope.launch {
            allItems = withContext(Dispatchers.IO) {
                runCatching { newsRepository.fetch() }.getOrElse { emptyList() }
            }
            filterItems()
        }
    }

    private fun filterItems() {
        val filtered = when (currentTab) {
            "news" -> allItems.filter { it.id.hashCode() % 2 == 0 } // Пример фильтрации - четные ID = новости
            "interesting" -> allItems.filter { it.id.hashCode() % 2 != 0 } // Нечетные ID = интересное
            else -> allItems
        }
        adapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

