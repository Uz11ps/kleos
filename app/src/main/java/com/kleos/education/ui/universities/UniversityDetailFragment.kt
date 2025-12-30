package com.kleos.education.ui.universities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kleos.education.databinding.FragmentUniversityDetailBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kleos.education.data.universities.UniversitiesRepository

class UniversityDetailFragment : Fragment() {

    private var _binding: FragmentUniversityDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUniversityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Устанавливаем фон программно для гарантии
        binding.root.setBackgroundColor(resources.getColor(com.kleos.education.R.color.onboarding_background, null))
        
        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)
        
        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.kleos.education.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
        
        val universityId = arguments?.getString("universityId") ?: ""
        
        if (universityId.isNotEmpty()) {
            loadUniversityDetail(universityId)
        } else {
            // Fallback: use arguments if passed directly
            val name = arguments?.getString("universityName") ?: ""
            displayUniversity(name, "", "", "", "", null, null)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Устанавливаем цвет статус-бара при возврате на экран
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)
    }
    
    private fun loadUniversityDetail(universityId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val university = withContext(Dispatchers.IO) {
                runCatching {
                    val repo = UniversitiesRepository()
                    repo.get(universityId)
                }.getOrNull()
            }
            
            if (university != null) {
                val location = listOfNotNull(university.city, university.country).joinToString(", ")
                val socialLinks = university.socialLinks
                val degreePrograms = university.degreePrograms
                displayUniversity(
                    university.name,
                    location,
                    university.description ?: "",
                    university.website ?: "",
                    university.logoUrl ?: "",
                    socialLinks,
                    degreePrograms
                )
            }
        }
    }
    
    private fun displayUniversity(
        name: String,
        location: String,
        description: String,
        website: String,
        logoUrl: String,
        socialLinks: com.kleos.education.data.network.SocialLinksDto?,
        degreePrograms: List<com.kleos.education.data.network.DegreeProgramDto>?
    ) {
        binding.nameText.text = name
        binding.locationText.text = location.ifBlank { "Местоположение не указано" }
        binding.descriptionText.text = description.ifBlank { "Описание отсутствует" }
        
        // Веб-сайт
        if (website.isNotEmpty()) {
            binding.websiteText.text = website
            binding.websiteText.visibility = View.VISIBLE
            binding.websiteText.setOnClickListener {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(website))
                    startActivity(intent)
                } catch (e: Exception) {
                    // Игнорируем ошибки открытия браузера
                }
            }
        } else {
            binding.websiteText.visibility = View.GONE
        }
        
        // Загрузка логотипа
        if (logoUrl.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val fullUrl = if (logoUrl.startsWith("http")) logoUrl else {
                        "${com.kleos.education.BuildConfig.API_BASE_URL}${logoUrl.removePrefix("/")}"
                    }
                    val conn = java.net.URL(fullUrl).openConnection()
                    conn.connectTimeout = 4000
                    conn.readTimeout = 6000
                    conn.getInputStream().use { input ->
                        val bmp = android.graphics.BitmapFactory.decodeStream(input)
                        withContext(Dispatchers.Main) {
                            binding.logoImageView.setImageBitmap(bmp)
                            binding.logoImageView.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.logoImageView.visibility = View.GONE
                    }
                }
            }
        } else {
            binding.logoImageView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Восстанавливаем цвет статус-бара
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.dark_background, null)
        _binding = null
    }
}


