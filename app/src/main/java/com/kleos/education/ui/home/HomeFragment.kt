package com.kleos.education.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kleos.education.R
import com.kleos.education.data.auth.SessionManager
import com.kleos.education.data.model.NewsItem
import com.kleos.education.data.news.NewsRepository
import com.kleos.education.databinding.FragmentHomeBinding
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
            (activity as? com.kleos.education.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
        
        // Обработка клика на аватарку - переход на профиль
        binding.profileImage.setOnClickListener {
            navigateToProfile()
        }
        
        // Обработка клика на область профиля - переход на профиль
        binding.profileHeader.setOnClickListener {
            navigateToProfile()
        }
        
        // Обработка клика на никнейм - переход на профиль
        binding.userNameText.setOnClickListener {
            navigateToProfile()
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
                com.kleos.education.R.id.newsDetailFragment,
                bundle
            )
        }
        binding.contentRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.contentRecycler.adapter = adapter
    }
    
    private fun navigateToProfile() {
        findNavController().navigate(com.kleos.education.R.id.nav_profile)
    }
    
    private fun loadUserData() {
        val session = SessionManager(requireContext())
        val user = session.getCurrentUser()
        val name = user?.fullName?.takeIf { it.isNotBlank() } ?: "Данил"
        
        binding.greetingText.text = getString(R.string.home_welcome_back)
        binding.userNameText.text = name
        // Делаем никнейм кликабельным
        binding.userNameText.isClickable = true
        binding.userNameText.isFocusable = true
        
        // Загружаем аватарку пользователя (только для зарегистрированных)
        if (user != null && !user.email.contains("guest")) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val profile = withContext(Dispatchers.IO) {
                        com.kleos.education.data.profile.ProfileRepository().getProfile()
                    }
                    if (!profile.avatarUrl.isNullOrEmpty()) {
                        loadAvatar(binding.profileImage, profile.avatarUrl)
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки загрузки аватара
                }
            }
        }
    }
    
    private fun loadAvatar(imageView: android.widget.ImageView, imageUrl: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Формируем полный URL, если он относительный
                val fullUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    imageUrl
                } else {
                    val baseUrl = com.kleos.education.BuildConfig.API_BASE_URL.trimEnd('/')
                    if (imageUrl.startsWith("/")) {
                        "$baseUrl$imageUrl"
                    } else {
                        "$baseUrl/$imageUrl"
                    }
                }
                
                // Используем OkHttp клиент из ApiClient для правильной авторизации
                val okHttpClient = com.kleos.education.data.network.ApiClient.okHttpClient
                val request = okhttp3.Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful && response.body != null) {
                    response.body?.use { body ->
                        val inputStream = body.byteStream()
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        withContext(Dispatchers.Main) {
                            if (bitmap != null && !bitmap.isRecycled) {
                                imageView.setImageBitmap(bitmap)
                                imageView.background = null // Убираем фон, чтобы показать изображение
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error loading avatar: ${e.message}", e)
                // В случае ошибки оставляем дефолтную иконку
            }
        }
    }
    
    private fun loadContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val newsItems = withContext(Dispatchers.IO) {
                    runCatching { newsRepository.fetch() }.getOrElse { e ->
                        android.util.Log.e("HomeFragment", "Error loading news", e)
                        emptyList()
                    }
                }
                
                android.util.Log.d("HomeFragment", "Loaded ${newsItems.size} news items, currentTab: $currentTab")
                
                val newsCategory = getString(R.string.category_news)
                val interestingCategory = getString(R.string.category_interesting)
                
                val contentCards = newsItems.mapIndexed { index, news ->
                    val category = when {
                        currentTab == "news" -> newsCategory
                        currentTab == "interesting" -> interestingCategory
                        index % 2 == 0 -> newsCategory
                        else -> interestingCategory
                    }
                    
                    val backgroundColor = when {
                        category == newsCategory -> android.graphics.Color.parseColor("#E8D5FF") // Лавандовый
                        else -> android.graphics.Color.parseColor("#FFD700") // Желтый
                    }
                    
                    ContentCard(
                        id = news.id,
                        category = category,
                        title = news.title,
                        date = news.dateText,
                        backgroundColor = backgroundColor,
                        imageUrl = news.imageUrl
                    )
                }.filter { card ->
                    when (currentTab) {
                        "news" -> card.category == newsCategory
                        "interesting" -> card.category == interestingCategory
                        else -> true
                    }
                }
                
                android.util.Log.d("HomeFragment", "Filtered to ${contentCards.size} cards")
                adapter.submitList(contentCards)
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error in loadContent", e)
                adapter.submitList(emptyList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

