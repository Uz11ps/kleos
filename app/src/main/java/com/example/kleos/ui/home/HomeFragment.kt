package com.example.kleos.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleos.data.model.NewsItem
import com.example.kleos.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
        val adapter = NewsAdapter(emptyList())
        binding.newsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecycler.adapter = adapter

        val demo = listOf(
            NewsItem("1", "Lobachevsky University with KLEOS", "March 6, 2024"),
            NewsItem("2", "دراسة الطب ... سيتشينوف", "August 10, 2023"),
            NewsItem("3", "Summer Camp in Sochi", "June 20, 2023")
        )
        adapter.submitList(demo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}