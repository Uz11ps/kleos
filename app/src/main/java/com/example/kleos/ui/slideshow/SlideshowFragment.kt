package com.example.kleos.ui.slideshow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.example.kleos.databinding.FragmentSlideshowBinding
import com.example.kleos.data.programs.ProgramsRepository
import com.example.kleos.ui.programs.ProgramDetailActivity
import com.example.kleos.ui.programs.ProgramsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    private val repository = ProgramsRepository()
    private lateinit var adapter: ProgramsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Устанавливаем подсказку с обрезкой текста для длинного hint
        val searchHint = getString(com.example.kleos.R.string.filter_search_program)
        val parentLayout = binding.filterQueryEditText.parent?.parent as? com.google.android.material.textfield.TextInputLayout
        parentLayout?.hint = searchHint
        
        // Анимации появления элементов
        val filterQueryEditText = binding.filterQueryEditText
        val parentView = filterQueryEditText.parent as? View
        parentView?.let {
            com.example.kleos.ui.utils.AnimationUtils.slideUpFade(it, 400, 0)
        }
        
        adapter = ProgramsAdapter(emptyList()) { p ->
            com.example.kleos.ui.utils.AnimationUtils.pressButton(binding.programsRecycler)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(requireContext(), ProgramDetailActivity::class.java)
                    .putExtra("title", p.title)
                    .putExtra("description", p.description ?: "")
                    .putExtra("university", p.university ?: "")
                    .putExtra("tuition", (p.tuition ?: 0.0).toString())
                    .putExtra("duration", (p.durationMonths ?: 0).toString())
                startActivity(intent)
            }, 150)
        }
        binding.programsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.programsRecycler.adapter = adapter

        // Анимация для кнопки поиска
        binding.searchButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.example.kleos.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.example.kleos.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }
        
        binding.searchButton.setOnClickListener { 
            com.example.kleos.ui.utils.AnimationUtils.shake(binding.searchButton)
            loadPrograms() 
        }
        // первичная загрузка без фильтров
        loadPrograms()
    }

    private fun loadPrograms() {
        val q = binding.filterQueryEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val language = binding.filterLanguageEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val level = binding.filterLevelEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val university = binding.filterUniversityEditText.text?.toString()?.trim().orEmpty().ifBlank { null }

        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching { repository.list(q, language, level, university) }.getOrElse { emptyList() }
            }
            adapter.submitList(items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}