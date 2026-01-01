package com.kleos.education.ui.programs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kleos.education.databinding.FragmentProgramsFiltersBinding
import com.kleos.education.ui.utils.AnimationUtils
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.kleos.education.data.programs.ProgramsRepository
import com.kleos.education.data.universities.UniversitiesRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import com.kleos.education.ui.programs.ProgramDetailActivity
import com.kleos.education.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.CornerFamily
import android.graphics.Color
import com.kleos.education.ui.utils.BottomSheetManager

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
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)

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
            (activity as? com.kleos.education.MainActivity)?.let { mainActivity ->
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
                android.util.Log.d("ProgramsFiltersFragment", "Loading filter options...")
                // Загружаем все программы для получения уникальных языков, уровней и названий
                val allPrograms = programsRepository.list(null, null, null, null)
                android.util.Log.d("ProgramsFiltersFragment", "Loaded ${allPrograms.size} programs")
                
                val languages = allPrograms.mapNotNull { it.language }.distinct().sorted()
                val levels = allPrograms.mapNotNull { it.level }.distinct().sorted()
                val programTitles = allPrograms.mapNotNull { it.title }.distinct().sorted()
                
                android.util.Log.d("ProgramsFiltersFragment", "Languages: $languages")
                android.util.Log.d("ProgramsFiltersFragment", "Levels: $levels")
                android.util.Log.d("ProgramsFiltersFragment", "Program titles count: ${programTitles.size}")
                
                // Загружаем список университетов
                val universities = universitiesRepository.list().map { it.name }.sorted()
                android.util.Log.d("ProgramsFiltersFragment", "Universities count: ${universities.size}")
                
                // Обновляем UI на главном потоке
                requireActivity().runOnUiThread {
                    setupDropdowns(languages, levels, universities, programTitles)
                    android.util.Log.d("ProgramsFiltersFragment", "Dropdowns setup completed")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProgramsFiltersFragment", "Error loading filter options", e)
                e.printStackTrace()
                // В случае ошибки используем пустые списки
                requireActivity().runOnUiThread {
                    setupDropdowns(emptyList(), emptyList(), emptyList(), emptyList())
                }
            }
        }
    }
    
    // Маппинги для переводов
    private val languageMap = mapOf(
        "ru" to "Русский",
        "en" to "Английский",
        "zh" to "Китайский"
    )
    
    private val reverseLanguageMap = mapOf(
        "Русский" to "ru",
        "Английский" to "en",
        "Китайский" to "zh"
    )
    
    private val levelMap = mapOf(
        "Bachelor's degree" to "Бакалавриат",
        "Master's degree" to "Магистратура",
        "Research degree" to "Докторантура",
        "Speciality degree" to "Специалитет"
    )
    
    private val reverseLevelMap = mapOf(
        "Бакалавриат" to "Bachelor's degree",
        "Магистратура" to "Master's degree",
        "Докторантура" to "Research degree",
        "Специалитет" to "Speciality degree"
    )
    
    private fun setupDropdowns(languages: List<String>, levels: List<String>, universities: List<String>, programTitles: List<String>) {
        // Переводы для языков
        val translatedLanguages = languages.map { languageMap[it] ?: it }
        
        // Переводы для уровней образования
        val translatedLevels = levels.map { levelMap[it] ?: it }
        
        // Настройка выпадающего списка для языка
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, translatedLanguages)
        (binding.languageEditText as? MaterialAutoCompleteTextView)?.apply {
            setAdapter(languageAdapter)
            threshold = 0 // Показывать список сразу при фокусе
        }
        
        // Настройка выпадающего списка для уровня обучения
        val levelAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, translatedLevels)
        (binding.levelEditText as? MaterialAutoCompleteTextView)?.apply {
            setAdapter(levelAdapter)
            threshold = 0 // Показывать список сразу при фокусе
        }
        
        // Настройка выпадающего списка для университета
        val universityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, universities)
        (binding.universityEditText as? MaterialAutoCompleteTextView)?.apply {
            setAdapter(universityAdapter)
            threshold = 0 // Показывать список сразу при фокусе
        }
        
        // Настройка выпадающего списка для названия программы
        val programTitleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, programTitles)
        (binding.searchEditText as? MaterialAutoCompleteTextView)?.apply {
            setAdapter(programTitleAdapter)
            threshold = 1 // Показывать список после ввода 1 символа
        }
    }

    private fun resetFilters() {
        (binding.searchEditText as? MaterialAutoCompleteTextView)?.setText("")
        (binding.languageEditText as? MaterialAutoCompleteTextView)?.setText("")
        (binding.levelEditText as? MaterialAutoCompleteTextView)?.setText("")
        (binding.universityEditText as? MaterialAutoCompleteTextView)?.setText("")
    }

    private fun applyFilters() {
        val searchQuery = (binding.searchEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        
        // Получаем значения и конвертируем переводы обратно в оригинальные значения
        val languageText = (binding.languageEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        val language = reverseLanguageMap[languageText] ?: languageText
        
        val levelText = (binding.levelEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        val level = reverseLevelMap[levelText] ?: levelText
        
        val university = (binding.universityEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()

        // Переход на экран результатов с параметрами фильтров
        val bundle = Bundle().apply {
            if (searchQuery.isNotEmpty()) putString("searchQuery", searchQuery)
            if (language.isNotEmpty()) putString("language", language)
            if (level.isNotEmpty()) putString("level", level)
            if (university.isNotEmpty()) putString("university", university)
        }
        findNavController().navigate(com.kleos.education.R.id.programsResultsFragment, bundle)
    }

    override fun onResume() {
        super.onResume()
        // Скрываем bottom navigation при возврате на страницу
        hideBottomNavigation()
        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)
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
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.dark_background, null)
        _binding = null
    }
    
    private fun hideBottomNavigation() {
        activity?.findViewById<com.kleos.education.ui.common.CustomBottomNavView>(com.kleos.education.R.id.bottom_nav)?.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        activity?.findViewById<com.kleos.education.ui.common.CustomBottomNavView>(com.kleos.education.R.id.bottom_nav)?.visibility = View.VISIBLE
    }
    
    private fun showResultsBottomSheet() {
        val searchQuery = (binding.searchEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        
        // Получаем значения и конвертируем переводы обратно в оригинальные значения
        val languageText = (binding.languageEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        val language = reverseLanguageMap[languageText] ?: languageText
        
        val levelText = (binding.levelEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        val level = reverseLevelMap[levelText] ?: levelText
        
        val university = (binding.universityEditText as? MaterialAutoCompleteTextView)?.text?.toString()?.trim().orEmpty()
        
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
                    .setTopLeftCorner(CornerFamily.ROUNDED, resources.getDimension(com.kleos.education.R.dimen.bottom_sheet_corner_radius))
                    .setTopRightCorner(CornerFamily.ROUNDED, resources.getDimension(com.kleos.education.R.dimen.bottom_sheet_corner_radius))
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
        
        val recyclerView = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(com.kleos.education.R.id.programsRecyclerView)
        val summaryText = bottomSheetView.findViewById<android.widget.TextView>(com.kleos.education.R.id.resultsSummary)
        
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
                
                android.util.Log.d("ProgramsFiltersFragment", "Searching programs with filters: q=$q, lang=$lang, level=$lvl, university=$univ")
                
                val programs = programsRepository.list(q, lang, lvl, univ)
                
                android.util.Log.d("ProgramsFiltersFragment", "Found ${programs.size} programs")
                
                requireActivity().runOnUiThread {
                    adapter.submitList(programs)
                    val count = programs.size
                    summaryText.text = resources.getString(R.string.admission_found_programs, count)
                    BottomSheetManager.showDialog(bottomSheetDialog)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProgramsFiltersFragment", "Error loading programs in bottom sheet", e)
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    adapter.submitList(emptyList())
                    summaryText.text = resources.getString(R.string.admission_found_programs, 0)
                    BottomSheetManager.showDialog(bottomSheetDialog)
                }
            }
        }
    }
}


