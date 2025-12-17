package com.example.kleos.ui.universities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kleos.R
import com.example.kleos.data.universities.UniversitiesRepository
import com.example.kleos.databinding.FragmentUniversitiesBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UniversitiesFragment : Fragment() {

    private var _binding: FragmentUniversitiesBinding? = null
    private val binding get() = _binding!!

    private val repository = UniversitiesRepository()
    private lateinit var adapter: UniversitiesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUniversitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UniversitiesAdapter(emptyList()) { university ->
            // Navigate to university detail or show programs
            val bundle = Bundle().apply {
                putString("universityId", university.id)
                putString("universityName", university.name)
            }
            // TODO: Navigate to university detail fragment when created
            // findNavController().navigate(R.id.universityDetailFragment, bundle)
        }

        binding.universitiesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.universitiesRecycler.adapter = adapter

        loadUniversities()
    }

    private fun loadUniversities() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching { repository.list() }.getOrElse { emptyList() }
            }
            adapter.submitList(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

