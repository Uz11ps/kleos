package com.example.kleos.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleos.ui.universities.UniversitiesAdapter
import com.example.kleos.databinding.FragmentGalleryBinding

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
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
        val adapter = UniversitiesAdapter(emptyList())
        binding.universitiesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.universitiesRecycler.adapter = adapter
        val demo = listOf(
            "Moscow Polytechnic University",
            "The Russian State Social University (RSSU)",
            "Minin University in Nizhny Novgorod",
            "Krasnoyarsk State Medical University",
            "MSUT \"STANKIN\"",
            "MEPhI (National Research Nuclear University)"
        )
        adapter.submitList(demo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}