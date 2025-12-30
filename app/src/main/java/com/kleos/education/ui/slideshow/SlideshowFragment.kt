package com.kleos.education.ui.slideshow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.kleos.education.databinding.FragmentSlideshowBinding
import com.kleos.education.data.programs.ProgramsRepository
import com.kleos.education.ui.programs.ProgramDetailActivity
import com.kleos.education.ui.programs.ProgramsAdapter
import com.kleos.education.ui.programs.ProgramResultsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kleos.education.ui.utils.BottomSheetManager
import com.kleos.education.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    private val repository = ProgramsRepository()
    private lateinit var adapter: ProgramsAdapter
    private var currentBottomSheetDialog: BottomSheetDialog? = null

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

        // Анимации появления элементов
        val filterQueryEditText = binding.filterQueryEditText
        val parentView = filterQueryEditText.parent as? View
        parentView?.let {
            com.kleos.education.ui.utils.AnimationUtils.slideUpFade(it, 400, 0)
        }
        
        adapter = ProgramsAdapter(emptyList()) { p ->
            com.kleos.education.ui.utils.AnimationUtils.pressButton(binding.programsRecycler)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(requireContext(), ProgramDetailActivity::class.java)
                    .putExtra("title", p.title)
                    .putExtra("description", p.description ?: "")
                    .putExtra("university", p.university ?: "")
                    .putExtra("tuition", (p.tuition ?: 0.0).toString())
                    .putExtra("duration", (p.durationYears ?: 0.0).toString())
                    .putExtra("language", p.language ?: "")
                    .putExtra("level", p.level ?: "")
                    .putExtra("durationYears", p.durationYears ?: 0.0)
                startActivity(intent)
            }, 150)
        }
        binding.programsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.programsRecycler.adapter = adapter

        // Анимация для кнопки поиска
        binding.searchButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.kleos.education.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.kleos.education.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }
        
        binding.searchButton.setOnClickListener { 
            com.kleos.education.ui.utils.AnimationUtils.shake(binding.searchButton)
            // Если это же окно уже открыто, закрываем его
            if (currentBottomSheetDialog?.isShowing == true && currentBottomSheetDialog == BottomSheetManager.getCurrentDialog()) {
                BottomSheetManager.dismissCurrent()
                currentBottomSheetDialog = null
            } else {
                showResultsBottomSheet()
            }
        }
        
        // Загружаем программы с фильтрами из аргументов или без фильтров
        loadPrograms()
    }

    private fun loadPrograms() {
        // Получаем параметры фильтров из arguments (если перешли с экрана фильтров)
        val q = arguments?.getString("searchQuery")?.takeIf { it.isNotEmpty() }
            ?: binding.filterQueryEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val language = arguments?.getString("language")?.takeIf { it.isNotEmpty() }
            ?: binding.filterLanguageEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val level = arguments?.getString("level")?.takeIf { it.isNotEmpty() }
            ?: binding.filterLevelEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val university = arguments?.getString("university")?.takeIf { it.isNotEmpty() }
            ?: binding.filterUniversityEditText.text?.toString()?.trim().orEmpty().ifBlank { null }

        // Если параметры пришли из arguments, заполняем поля фильтров
        if (arguments != null) {
            q?.let { binding.filterQueryEditText.setText(it) }
            language?.let { binding.filterLanguageEditText.setText(it) }
            level?.let { binding.filterLevelEditText.setText(it) }
            university?.let { binding.filterUniversityEditText.setText(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching { 
                    repository.list(q, language, level, university)
                }.onFailure { e ->
                    android.util.Log.e("SlideshowFragment", "Error loading programs", e)
                    e.printStackTrace()
                }.getOrElse { emptyList() }
            }
            android.util.Log.d("SlideshowFragment", "Loaded ${items.size} programs")
            adapter.submitList(items)
        }
    }
    
    private fun showResultsBottomSheet() {
        val q = binding.filterQueryEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val language = binding.filterLanguageEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val level = binding.filterLevelEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        val university = binding.filterUniversityEditText.text?.toString()?.trim().orEmpty().ifBlank { null }
        
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        currentBottomSheetDialog = bottomSheetDialog
        
        // Очищаем ссылку при закрытии диалога
        bottomSheetDialog.setOnDismissListener {
            if (currentBottomSheetDialog == bottomSheetDialog) {
                currentBottomSheetDialog = null
            }
        }
        val bottomSheetView = layoutInflater.inflate(com.kleos.education.R.layout.bottom_sheet_programs_results, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Убираем затемнение фона
        bottomSheetDialog.window?.setDimAmount(0f)
        
        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { view ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(view)
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.75).toInt()
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        
        val recyclerView = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(com.kleos.education.R.id.programsRecyclerView)
        val summaryText = bottomSheetView.findViewById<android.widget.TextView>(com.kleos.education.R.id.resultsSummary)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val resultsAdapter = ProgramResultsAdapter(emptyList()) { program ->
            bottomSheetDialog.dismiss()
            val intent = Intent(requireContext(), ProgramDetailActivity::class.java)
                .putExtra("title", program.title)
                .putExtra("description", program.description ?: "")
                .putExtra("university", program.university ?: "")
                .putExtra("tuition", (program.tuition ?: 0.0).toString())
                .putExtra("duration", (program.durationYears ?: 0.0).toString())
                .putExtra("language", program.language ?: "")
                .putExtra("level", program.level ?: "")
                .putExtra("durationYears", program.durationYears ?: 0.0)
            startActivity(intent)
        }
        recyclerView.adapter = resultsAdapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching { 
                    repository.list(q, language, level, university)
                }.onFailure { e ->
                    android.util.Log.e("SlideshowFragment", "Error loading programs in bottom sheet", e)
                    e.printStackTrace()
                }.getOrElse { emptyList() }
            }
            android.util.Log.d("SlideshowFragment", "Loaded ${items.size} programs in bottom sheet")
            resultsAdapter.submitList(items)
            val count = items.size
            summaryText.text = resources.getString(R.string.admission_found_programs, count)
            BottomSheetManager.showDialog(bottomSheetDialog)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Закрываем открытое окно при уничтожении view
        currentBottomSheetDialog?.dismiss()
        currentBottomSheetDialog = null
        _binding = null
    }
}
