package com.example.kleos.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleos.data.auth.SessionManager
import com.example.kleos.data.model.NewsItem
import com.example.kleos.data.news.NewsRepository
import com.example.kleos.databinding.FragmentHomeBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private var currentTab = "all" // all, news, interesting
    private val newsRepository = NewsRepository()
    private lateinit var adapter: ContentCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTabs()
        setupRecyclerView()
        loadUserData()
        loadContent()
        
        // Обработка клика на меню
        binding.menuButton.setOnClickListener {
            // Открываем drawer menu
            (activity as? com.example.kleos.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
    }
    
    private fun setupTabs() {
        // Устанавливаем активный таб "Все"
        updateTabStyles("all")
        
        binding.tabAll.setOnClickListener {
            currentTab = "all"
            updateTabStyles("all")
            loadContent()
        }
        
        binding.tabNews.setOnClickListener {
            currentTab = "news"
            updateTabStyles("news")
            loadContent()
        }
        
        binding.tabInteresting.setOnClickListener {
            currentTab = "interesting"
            updateTabStyles("interesting")
            loadContent()
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
        binding.tabNews.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (activeTab == "news") activeBackgroundColor else inactiveBackgroundColor
        )
        binding.tabNews.setTextColor(
            if (activeTab == "news") activeTextColor else inactiveTextColor
        )
        
        // Кнопка "Интересное"
        binding.tabInteresting.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (activeTab == "interesting") activeBackgroundColor else inactiveBackgroundColor
        )
        binding.tabInteresting.setTextColor(
            if (activeTab == "interesting") activeTextColor else inactiveTextColor
        )
    }
    
    private fun setupRecyclerView() {
        adapter = ContentCardAdapter(emptyList()) { item ->
            // Переход на детальную страницу
            val bundle = Bundle().apply {
                putString("newsId", item.id)
                putString("title", item.title)
            }
            findNavController().navigate(
                com.example.kleos.R.id.newsDetailFragment,
                bundle
            )
        }
        binding.contentRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.contentRecycler.adapter = adapter
    }
    
    private fun loadUserData() {
        val session = SessionManager(requireContext())
        val user = session.getCurrentUser()
        val name = user?.fullName?.takeIf { it.isNotBlank() } ?: "Данил"
        
        binding.greetingText.text = "С возвращением,"
        binding.userNameText.text = name
    }
    
    private fun loadContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newsItems = withContext(Dispatchers.IO) {
                runCatching { newsRepository.fetch() }.getOrElse { emptyList() }
            }
            
            val contentCards = newsItems.mapIndexed { index, news ->
                val category = when {
                    currentTab == "news" -> "Новости"
                    currentTab == "interesting" -> "Интересное"
                    index % 2 == 0 -> "Новости"
                    else -> "Интересное"
                }
                
                val backgroundColor = when {
                    category == "Новости" -> android.graphics.Color.parseColor("#E8D5FF") // Лавандовый
                    else -> android.graphics.Color.parseColor("#FFD700") // Желтый
                }
                
                ContentCard(
                    id = news.id,
                    category = category,
                    title = news.title,
                    date = news.dateText,
                    backgroundColor = backgroundColor
                )
            }.filter { card ->
                when (currentTab) {
                    "news" -> card.category == "Новости"
                    "interesting" -> card.category == "Интересное"
                    else -> true
                }
            }
            
            adapter.submitList(contentCards)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
