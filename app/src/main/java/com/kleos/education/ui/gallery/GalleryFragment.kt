package com.kleos.education.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.kleos.education.R
import com.kleos.education.data.gallery.GalleryRepository
import com.kleos.education.databinding.FragmentGalleryBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Управление цветом статус-бара
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.kleos.education.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
        
        val adapter = GalleryAdapter(emptyList()) { item ->
            val bundle = Bundle().apply {
                putString("itemId", item.id)
                putString("itemTitle", item.title)
                putString("itemDescription", item.description ?: "")
                putString("itemMediaUrl", item.mediaUrl)
                putString("itemMediaType", item.mediaType)
            }
            findNavController().navigate(com.kleos.education.R.id.galleryDetailFragment, bundle)
        }
        
        binding.galleryRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.galleryRecycler.adapter = adapter

        val repo = GalleryRepository()
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching { repo.fetch() }.getOrElse { emptyList() }
            }
            adapter.submitList(items)
        }
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
