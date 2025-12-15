package com.example.kleos.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kleos.MainActivity
import com.example.kleos.data.auth.AuthRepository
import com.example.kleos.data.auth.SessionManager
import com.example.kleos.data.profile.ProfileRepository
import com.example.kleos.databinding.FragmentProfileBinding
import com.example.kleos.ui.auth.AuthActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileRepository = ProfileRepository()
    private var refreshJob: Job? = null
    private var saveJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = SessionManager(requireContext())
        
        // Проверяем авторизацию
        if (!session.isLoggedIn()) {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            return
        }

        // Phone mask per language
        val lang = resources.configuration.locales[0]?.language ?: "en"
        binding.phoneEditText.addTextChangedListener(
            com.example.kleos.ui.common.PhoneMaskTextWatcher(binding.phoneEditText, lang)
        )

        // Загружаем данные профиля
        loadProfile()

        // Автосохранение при изменении полей
        setupAutoSave()

        // Периодическое обновление данных с сервера
        startPeriodicRefresh()

        binding.logoutButton.setOnClickListener {
            AuthRepository.Local(requireContext()).logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    profileRepository.getProfile()
                }
                // Обновляем роль пользователя в сессии
                val sessionManager = SessionManager(requireContext())
                sessionManager.saveRole(profile.role)
                
                binding.nameEditText.setText(profile.fullName)
                binding.phoneEditText.setText(profile.phone ?: "")
                binding.notesEditText.setText(profile.notes ?: "")
                binding.paymentEditText.setText(profile.payment ?: "")
                binding.penaltiesEditText.setText(profile.penalties ?: "")
                binding.courseEditText.setText(profile.course ?: "")
                binding.specialityEditText.setText(profile.speciality ?: "")
                binding.statusEditText.setText(profile.status ?: "")
                binding.universityEditText.setText(profile.university ?: "")
                binding.studentIdEditText.setText(profile.studentId ?: "")
                
                // Обновляем видимость меню в MainActivity после обновления роли
                (activity as? MainActivity)?.updateMenuVisibility()
            } catch (e: Exception) {
                // Если не удалось загрузить с сервера, используем локальные данные
                val user = SessionManager(requireContext()).getCurrentUser()
                binding.nameEditText.setText(user?.fullName ?: "")
            }
        }
    }

    private fun setupAutoSave() {
        val fields = listOf(
            binding.nameEditText,
            binding.phoneEditText,
            binding.notesEditText,
            binding.paymentEditText,
            binding.penaltiesEditText,
            binding.courseEditText,
            binding.specialityEditText,
            binding.statusEditText,
            binding.universityEditText
        )

        fields.forEach { editText ->
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    // Для телефона проверяем минимальную длину перед сохранением
                    if (editText == binding.phoneEditText) {
                        val phoneText = editText.text?.toString().orEmpty()
                        val phoneDigits = phoneText.filter { it.isDigit() }
                        // Сохраняем только если номер пустой или содержит минимум 10 цифр
                        if (phoneText.isEmpty() || phoneDigits.length >= 10) {
                            saveProfile()
                        }
                    } else {
                        saveProfile()
                    }
                }
            }
        }
        
        // Дополнительно сохраняем телефон при изменении текста (с задержкой)
        var phoneSaveJob: Job? = null
        binding.phoneEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                phoneSaveJob?.cancel()
                val phoneText = s?.toString().orEmpty()
                val phoneDigits = phoneText.filter { it.isDigit() }
                // Сохраняем только если номер содержит минимум 10 цифр
                if (phoneDigits.length >= 10) {
                    phoneSaveJob = lifecycleScope.launch {
                        delay(2000) // Ждем 2 секунды после последнего изменения
                        if (isActive) {
                            saveProfile()
                        }
                    }
                }
            }
        })
    }

    private fun saveProfile() {
        // Отменяем предыдущую задачу сохранения
        saveJob?.cancel()
        
        saveJob = lifecycleScope.launch {
            delay(1000) // Ждем 1 секунду после последнего изменения
            if (!isActive) return@launch
            
            // Валидация номера телефона: минимум 10 цифр (без учета форматирования)
            val phoneText = binding.phoneEditText.text?.toString().orEmpty()
            val phoneDigits = phoneText.filter { it.isDigit() }
            val isValidPhone = phoneDigits.length >= 10 || phoneText.isEmpty()
            
            val result = withContext(Dispatchers.IO) {
                profileRepository.updateProfile(
                    fullName = binding.nameEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    phone = phoneText.takeIf { isValidPhone && it.isNotBlank() },
                    notes = binding.notesEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    payment = binding.paymentEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    penalties = binding.penaltiesEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    course = binding.courseEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    speciality = binding.specialityEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    status = binding.statusEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    university = binding.universityEditText.text?.toString()?.takeIf { it.isNotBlank() }
                )
            }
            
            if (result.isFailure) {
                // Ошибка сохранения - можно показать toast или просто игнорировать
            }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(5000) // Обновляем каждые 5 секунд
                if (!isActive) break
                
                try {
                    val profile = withContext(Dispatchers.IO) {
                        profileRepository.getProfile()
                    }
                    // Обновляем только если поля не в фокусе (чтобы не мешать вводу)
                    if (!binding.nameEditText.hasFocus()) {
                        binding.nameEditText.setText(profile.fullName)
                    }
                    if (!binding.phoneEditText.hasFocus()) {
                        binding.phoneEditText.setText(profile.phone ?: "")
                    }
                    if (!binding.notesEditText.hasFocus()) {
                        binding.notesEditText.setText(profile.notes ?: "")
                    }
                    if (!binding.paymentEditText.hasFocus()) {
                        binding.paymentEditText.setText(profile.payment ?: "")
                    }
                    if (!binding.penaltiesEditText.hasFocus()) {
                        binding.penaltiesEditText.setText(profile.penalties ?: "")
                    }
                    if (!binding.courseEditText.hasFocus()) {
                        binding.courseEditText.setText(profile.course ?: "")
                    }
                    if (!binding.specialityEditText.hasFocus()) {
                        binding.specialityEditText.setText(profile.speciality ?: "")
                    }
                    if (!binding.statusEditText.hasFocus()) {
                        binding.statusEditText.setText(profile.status ?: "")
                    }
                    if (!binding.universityEditText.hasFocus()) {
                        binding.universityEditText.setText(profile.university ?: "")
                    }
                    binding.studentIdEditText.setText(profile.studentId ?: "")
                } catch (e: Exception) {
                    // Игнорируем ошибки обновления
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        refreshJob?.cancel()
        refreshJob = null
        saveProfile() // Сохраняем при уходе с экрана
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshJob?.cancel()
        saveJob?.cancel()
        _binding = null
    }
}


