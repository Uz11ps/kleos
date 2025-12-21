package com.example.kleos.ui.programs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kleos.databinding.FragmentProgramsFiltersBinding
import com.example.kleos.ui.utils.AnimationUtils
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.example.kleos.data.programs.ProgramsRepository
import com.example.kleos.data.universities.UniversitiesRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import com.example.kleos.ui.programs.ProgramDetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.CornerFamily
import android.graphics.Color
import com.example.kleos.ui.utils.BottomSheetManager

class ProgramsFiltersFragment : Fragment() {

    private var _binding: FragmentProgramsFiltersBinding? = null
    private val binding get() = _binding!!
    
    private val programsRepository = ProgramsRepository()
    private val universitiesRepository = UniversitiesRepository()
    private var currentBottomSheetDialog: BottomSheetDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramsFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Скрываем bottom navigation на этой странице
        hideBottomNavigation()

        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)

        // Анимация появления элементов
        AnimationUtils.slideUpFade(binding.root, 300, 0)

        // Кнопка назад
        binding.backButton.setOnClickListener {
            AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                AnimationUtils.releaseButton(it)
                findNavController().popBackStack()
            }, 150)
        }

        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.example.kleos.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
        
        // Загружаем варианты для выпадающих списков
        loadFilterOptions()
        
        // Обработка кнопки "Найти варианты"
        binding.findButton.setOnClickListener {
            AnimationUtils.pressButton(it)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                AnimationUtils.releaseButton(it)
                // Если это же окно уже открыто, закрываем его
                if (currentBottomSheetDialog?.isShowing == true && currentBottomSheetDialog == BottomSheetManager.getCurrentDialog()) {
                    BottomSheetManager.dismissCurrent()
                    currentBottomSheetDialog = null
                } else {
                    showResultsBottomSheet()
                }
            }, 150)
        }
    }
    
    private fun loadFilterOptions() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Загружаем все программы для получения уникальных языков и уровней
                val allPrograms = programsRepository.list(null, null, null, null)
                val languages = allPrograms.mapNotNull { it.language }.distinct().sorted()
                val levels = allPrograms.mapNotNull { it.level }.distinct().sorted()
                
                // Загружаем список университетов
                val universities = universitiesRepository.list().map { it.name }.sorted()
                
                // Обновляем UI на главном потоке
                requireActivity().runOnUiThread {
                    setupDropdowns(languages, levels, universities)
                }
            } catch (e: Exception) {
                // В случае ошибки используем пустые списки
                requireActivity().runOnUiThread {
                    setupDropdowns(emptyList(), emptyList(), emptyList())
                }
            }
        }
    }
    
    private fun setupDropdowns(languages: List<String>, levels: List<String>, universities: List<String>) {
        // Настройка выпадающего списка для языка
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, languages)
        (binding.languageEditText as? MaterialAutoCompleteTextView)?.setAdapter(languageAdapter)
        
        // Настройка выпадающего списка для уровня обучения
        val levelAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, levels)
        (binding.levelEditText as? MaterialAutoCompleteTextView)?.setAdapter(levelAdapter)
        
        // Настройка выпадающего списка для университета
        val universityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, universities)
        (binding.universityEditText as? MaterialAutoCompleteTextView)?.setAdapter(universityAdapter)
    }

    private fun resetFilters() {
        binding.searchEditText.setText("")
        binding.languageEditText.setText("")
        binding.levelEditText.setText("")
        binding.universityEditText.setText("")
    }

    private fun applyFilters() {
        val searchQuery = binding.searchEditText.text?.toString()?.trim().orEmpty()
        val language = binding.languageEditText.text?.toString()?.trim().orEmpty()
        val level = binding.levelEditText.text?.toString()?.trim().orEmpty()
        val university = binding.universityEditText.text?.toString()?.trim().orEmpty()

        // Переход на экран результатов с параметрами фильтров
        val bundle = Bundle().apply {
            if (searchQuery.isNotEmpty()) putString("searchQuery", searchQuery)
            if (language.isNotEmpty()) putString("language", language)
            if (level.isNotEmpty()) putString("level", level)
            if (university.isNotEmpty()) putString("university", university)
        }
        findNavController().navigate(com.example.kleos.R.id.programsResultsFragment, bundle)
    }

    override fun onResume() {
        super.onResume()
        // Скрываем bottom navigation при возврате на страницу
        hideBottomNavigation()
        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)
    }

    override fun onPause() {
        super.onPause()
        // Показываем bottom navigation при уходе со страницы
        showBottomNavigation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Показываем bottom navigation обратно при выходе со страницы
        showBottomNavigation()
        // Восстанавливаем цвет статус-бара
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.dark_background, null)
        _binding = null
    }
    
    private fun hideBottomNavigation() {
        activity?.findViewById<com.example.kleos.ui.common.CustomBottomNavView>(com.example.kleos.R.id.bottom_nav)?.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        activity?.findViewById<com.example.kleos.ui.common.CustomBottomNavView>(com.example.kleos.R.id.bottom_nav)?.visibility = View.VISIBLE
    }
    
    private fun showResultsBottomSheet() {
        val searchQuery = binding.searchEditText.text?.toString()?.trim().orEmpty()
        val language = (binding.languageEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        val level = (binding.levelEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        val university = (binding.universityEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        currentBottomSheetDialog = bottomSheetDialog
        
        // Очищаем ссылку при закрытии диалога
        bottomSheetDialog.setOnDismissListener {
            if (currentBottomSheetDialog == bottomSheetDialog) {
                currentBottomSheetDialog = null
            }
        }
        val bottomSheetView = layoutInflater.inflate(com.example.kleos.R.layout.bottom_sheet_programs_results, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Убираем затемнение фона и устанавливаем прозрачный фон окна
        bottomSheetDialog.window?.setDimAmount(0f)
        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Настраиваем поведение bottom sheet для правильной высоты (больше половины экрана)
        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { view ->
                // Устанавливаем правильный фон для bottom sheet с закругленными углами
                // #240A24 с прозрачностью 64% = #A3240A24
                val shapeAppearanceModel = ShapeAppearanceModel.builder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, resources.getDimension(com.example.kleos.R.dimen.bottom_sheet_corner_radius))
                    .setTopRightCorner(CornerFamily.ROUNDED, resources.getDimension(com.example.kleos.R.dimen.bottom_sheet_corner_radius))
                    .build()
                val materialShapeDrawable = MaterialShapeDrawable(shapeAppearanceModel).apply {
                    fillColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#A3240A24"))
                }
                ViewCompat.setBackground(view, materialShapeDrawable)
                
                // Устанавливаем высоту окна на 100% страницы
                val fullHeight = resources.displayMetrics.heightPixels
                
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(view)
                behavior.peekHeight = fullHeight
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        
        val recyclerView = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(com.example.kleos.R.id.programsRecyclerView)
        val summaryText = bottomSheetView.findViewById<android.widget.TextView>(com.example.kleos.R.id.resultsSummary)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ProgramResultsAdapter(emptyList()) { program ->
            bottomSheetDialog.dismiss()
            // Закрываем фрагмент фильтров перед открытием детальной страницы
            findNavController().popBackStack()
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
        recyclerView.adapter = adapter
        
        // Загружаем программы
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val q = searchQuery.takeIf { it.isNotEmpty() }
                val lang = language.takeIf { it.isNotEmpty() }
                val lvl = level.takeIf { it.isNotEmpty() }
                val univ = university.takeIf { it.isNotEmpty() }
                
                val programs = programsRepository.list(q, lang, lvl, univ)
                
                requireActivity().runOnUiThread {
                    adapter.submitList(programs)
                    val count = programs.size
                    summaryText.text = "Найдено: $count ${if (count == 1) "программа" else if (count in 2..4) "программы" else "программ"}"
                    BottomSheetManager.showDialog(bottomSheetDialog)
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    adapter.submitList(emptyList())
                    summaryText.text = "Найдено: 0 программ"
                    BottomSheetManager.showDialog(bottomSheetDialog)
                }
            }
        }
    }
}

