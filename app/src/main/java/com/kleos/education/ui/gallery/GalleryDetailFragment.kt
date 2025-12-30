package com.kleos.education.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kleos.education.databinding.FragmentGalleryDetailBinding

class GalleryDetailFragment : Fragment() {

    private var _binding: FragmentGalleryDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Управление цветом статус-бара
        val originalStatusBarColor = activity?.window?.statusBarColor
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)
        
        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.kleos.education.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
        
        val itemTitle = arguments?.getString("itemTitle") ?: ""
        val itemDescription = arguments?.getString("itemDescription") ?: ""
        val itemMediaUrl = arguments?.getString("itemMediaUrl") ?: ""
        
        binding.titleText.text = itemTitle
        binding.descriptionText.text = itemDescription
        
        // TODO: Load and display image/video using itemMediaUrl and itemMediaType
        // For now, just show the URL
        binding.mediaUrlText.text = itemMediaUrl
    }
    
    override fun onResume() {
        super.onResume()
        // Устанавливаем цвет статус-бара при возврате на страницу
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Восстанавливаем цвет статус-бара
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)
        _binding = null
    }
}


