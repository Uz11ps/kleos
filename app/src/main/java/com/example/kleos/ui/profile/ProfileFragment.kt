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
import okhttp3.Request

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
        
        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }
        
        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        
        val session = SessionManager(requireContext())
        
        // Проверяем авторизацию
        if (!session.isLoggedIn()) {
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            return
        }

        // Проверяем, является ли пользователь гостем
        val currentUser = session.getCurrentUser()
        val isGuest = currentUser?.email == "guest@local"
        
        if (isGuest) {
            // Показываем экран входа для гостей
            showGuestLoginScreen()
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

    }

    private fun showGuestLoginScreen() {
        // Скрываем обычный профиль
        binding.scrollView.visibility = View.GONE
        binding.backButton.visibility = View.GONE
        binding.menuButton.visibility = View.GONE
        
        // Показываем экран входа для гостей
        binding.guestLoginContainer.visibility = View.VISIBLE
        
        // Обработчик клика на кнопку входа
        binding.guestLoginButton.setOnClickListener {
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
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
                
                // Отображаем имя и роль в верхней части профиля
                binding.profileNameText.text = profile.fullName
                val roleText = when (profile.role) {
                    "student" -> getString(com.example.kleos.R.string.role_student)
                    "admin" -> getString(com.example.kleos.R.string.role_admin)
                    else -> getString(com.example.kleos.R.string.role_user)
                }
                binding.profileRoleText.text = roleText
                
                // Загружаем аватарку, если она есть
                if (!profile.avatarUrl.isNullOrEmpty()) {
                    loadAvatar(binding.profileAvatar, profile.avatarUrl)
                } else {
                    // Сбрасываем на дефолтную иконку
                    binding.profileAvatar.setImageResource(com.example.kleos.R.drawable.ic_profile)
                    binding.profileAvatar.background = resources.getDrawable(com.example.kleos.R.drawable.bg_user_card, null)
                }
                
                // Заполняем все поля
                binding.nameEditText.setText(profile.fullName)
                binding.emailEditText.setText(profile.email)
                binding.phoneEditText.setText(profile.phone ?: "")
                binding.courseEditText.setText(profile.course ?: "")
                binding.specialityEditText.setText(profile.speciality ?: "")
                binding.universityEditText.setText(profile.university ?: "")
                binding.studentIdEditText.setText(profile.studentId ?: "")
                binding.statusEditText.setText(profile.status ?: "")
                binding.paymentEditText.setText(profile.payment ?: "")
                binding.penaltiesEditText.setText(profile.penalties ?: "")
                binding.notesEditText.setText(profile.notes ?: "")
                
                // Блокируем редактирование для студентов и обычных пользователей (только админы могут редактировать)
                val isStudent = profile.role == "student"
                val isAdmin = profile.role == "admin"
                val canEdit = isAdmin // Только админы могут редактировать
                // Делаем все поля только для чтения, если пользователь не админ
                val fields = listOf(
                    binding.nameEditText,
                    binding.phoneEditText,
                    binding.courseEditText,
                    binding.specialityEditText,
                    binding.universityEditText,
                    binding.studentIdEditText,
                    binding.statusEditText,
                    binding.paymentEditText,
                    binding.penaltiesEditText,
                    binding.notesEditText
                )
                
                fields.forEach { editText ->
                    editText.isEnabled = canEdit
                    editText.isFocusable = canEdit
                    editText.isFocusableInTouchMode = canEdit
                }
                
                // Email всегда только для чтения
                binding.emailEditText.isEnabled = false
                binding.emailEditText.isFocusable = false
                binding.emailEditText.isFocusableInTouchMode = false
                
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
        // Проверяем роль пользователя - только админы могут редактировать
        val sessionManager = SessionManager(requireContext())
        val userRole = sessionManager.getUserRole()
        val isAdmin = userRole == "admin"
        
        if (!isAdmin) {
            // Для не-админов отключаем автосохранение
            return
        }
        
        val fields = listOf(
            binding.nameEditText,
            binding.phoneEditText,
            binding.courseEditText,
            binding.specialityEditText,
            binding.universityEditText,
            binding.studentIdEditText,
            binding.statusEditText,
            binding.paymentEditText,
            binding.penaltiesEditText,
            binding.notesEditText
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
        // Проверяем роль пользователя - только админы могут редактировать
        val sessionManager = SessionManager(requireContext())
        val userRole = sessionManager.getUserRole()
        val isAdmin = userRole == "admin"
        
        if (!isAdmin) {
            // Для не-админов блокируем сохранение
            return
        }
        
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
                    course = binding.courseEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    speciality = binding.specialityEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    university = binding.universityEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    status = binding.statusEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    payment = binding.paymentEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    penalties = binding.penaltiesEditText.text?.toString()?.takeIf { it.isNotBlank() },
                    notes = binding.notesEditText.text?.toString()?.takeIf { it.isNotBlank() }
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
                    if (!binding.courseEditText.hasFocus()) {
                        binding.courseEditText.setText(profile.course ?: "")
                    }
                    if (!binding.specialityEditText.hasFocus()) {
                        binding.specialityEditText.setText(profile.speciality ?: "")
                    }
                    if (!binding.universityEditText.hasFocus()) {
                        binding.universityEditText.setText(profile.university ?: "")
                    }
                    if (!binding.studentIdEditText.hasFocus()) {
                        binding.studentIdEditText.setText(profile.studentId ?: "")
                    }
                    if (!binding.statusEditText.hasFocus()) {
                        binding.statusEditText.setText(profile.status ?: "")
                    }
                    if (!binding.paymentEditText.hasFocus()) {
                        binding.paymentEditText.setText(profile.payment ?: "")
                    }
                    if (!binding.penaltiesEditText.hasFocus()) {
                        binding.penaltiesEditText.setText(profile.penalties ?: "")
                    }
                    if (!binding.notesEditText.hasFocus()) {
                        binding.notesEditText.setText(profile.notes ?: "")
                    }
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

    override fun onResume() {
        super.onResume()
        // Устанавливаем цвет статус-бара при возврате на экран
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.onboarding_background, null)
    }
    
    private fun loadAvatar(imageView: android.widget.ImageView, imageUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Формируем полный URL, если он относительный
                val fullUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    imageUrl
                } else {
                    val baseUrl = com.example.kleos.BuildConfig.API_BASE_URL.trimEnd('/')
                    if (imageUrl.startsWith("/")) {
                        "$baseUrl$imageUrl"
                    } else {
                        "$baseUrl/$imageUrl"
                    }
                }
                
                // Используем OkHttp клиент из ApiClient для правильной авторизации
                val okHttpClient = com.example.kleos.data.network.ApiClient.okHttpClient
                val request = Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful && response.body != null) {
                    response.body?.use { body ->
                        val inputStream = body.byteStream()
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        withContext(Dispatchers.Main) {
                            if (bitmap != null && !bitmap.isRecycled) {
                                imageView.setImageBitmap(bitmap)
                                imageView.background = null // Убираем фон, чтобы показать изображение
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileFragment", "Error loading avatar: ${e.message}", e)
                // В случае ошибки оставляем дефолтную иконку
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Восстанавливаем цвет статус-бара
        activity?.window?.statusBarColor = resources.getColor(com.example.kleos.R.color.dark_background, null)
        refreshJob?.cancel()
        saveJob?.cancel()
        _binding = null
    }
}


