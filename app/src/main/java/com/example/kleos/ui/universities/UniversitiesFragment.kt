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
        
        // Устанавливаем фон программно для гарантии
        binding.root.setBackgroundColor(resources.getColor(com.example.kleos.R.color.onboarding_background, null))
        
        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.example.kleos.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }

        adapter = UniversitiesAdapter(emptyList()) { university ->
            // Navigate to university detail
            val bundle = Bundle().apply {
                putString("universityId", university.id)
                putString("universityName", university.name)
            }
            findNavController().navigate(R.id.universityDetailFragment, bundle)
        }

        binding.universitiesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.universitiesRecycler.adapter = adapter

        loadUniversities()
    }
    
    override fun onResume() {
        super.onResume()
        // Устанавливаем цвет статус-бара при возврате на страницу
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Восстанавливаем цвет статус-бара
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.dark_background, null)
        _binding = null
    }

    private fun loadUniversities() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching { repository.list() }.getOrElse { emptyList() }
            }
            adapter.submitList(items)
        }
    }
}

