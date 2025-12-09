package com.example.kleos.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.kleos.databinding.FragmentGalleryDetailBinding

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
        
        val itemTitle = arguments?.getString("itemTitle") ?: ""
        val itemDescription = arguments?.getString("itemDescription") ?: ""
        val itemMediaUrl = arguments?.getString("itemMediaUrl") ?: ""
        
        binding.titleText.text = itemTitle
        binding.descriptionText.text = itemDescription
        
        // TODO: Load and display image/video using itemMediaUrl and itemMediaType
        // For now, just show the URL
        binding.mediaUrlText.text = itemMediaUrl
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

