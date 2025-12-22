package com.example.kleos.ui.admission

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.example.kleos.data.admissions.AdmissionsRepository
import com.example.kleos.data.model.AdmissionApplication
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.SettingsApi
import com.example.kleos.databinding.FragmentAdmissionFormBinding
import com.example.kleos.ui.utils.AnimationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import com.example.kleos.ui.language.t

class AdmissionFormFragment : Fragment() {

    private var _binding: FragmentAdmissionFormBinding? = null
    private val binding get() = _binding!!

    private lateinit var admissionsRepository: AdmissionsRepository
    private var countriesList: List<String> = emptyList()
    private var consentTextRu: String = ""
    private var consentTextEn: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdmissionFormBinding.inflate(inflater, container, false)
        admissionsRepository = AdmissionsRepository.Http(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (!isAdded) return
        
        // Скрываем bottom navigation на этой странице
        hideBottomNavigation()
        
        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        
        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.example.kleos.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }

        // Анимация появления заголовка
        AnimationUtils.slideUpFade(binding.titleText, 0)
        
        // Анимация появления полей
        val editTexts = listOf(
            binding.firstNameEditText,
            binding.lastNameEditText,
            binding.patronymicEditText,
            binding.dateOfBirthEditText,
            binding.placeOfBirthEditText,
            binding.nationalityEditText,
            binding.emailEditText,
            binding.phoneEditText,
            binding.passportNumberEditText,
            binding.passportIssueEditText,
            binding.passportExpiryEditText,
            binding.visaCityEditText,
            binding.programEditText,
            binding.commentEditText
        )
        
        editTexts.forEachIndexed { index, editText ->
            editText.parent?.let { parent ->
                if (parent is View) {
                    AnimationUtils.slideUpFade(parent, (index * 50).toLong())
                }
            }
        }
        
        // Анимация появления кнопки
        view.postDelayed({
            if (isAdded && _binding != null) {
                AnimationUtils.bounceIn(binding.submitButton, editTexts.size * 50L)
            }
        }, 100)

        applyDateMask(binding.dateOfBirthEditText)
        applyDateMask(binding.passportIssueEditText)
        applyDateMask(binding.passportExpiryEditText)

        // Phone mask per language
        val lang = resources.configuration.locales[0]?.language ?: "en"
        binding.phoneEditText.addTextChangedListener(
            com.example.kleos.ui.common.PhoneMaskTextWatcher(binding.phoneEditText, lang)
        )
        
        // Заполняем email из сессии пользователя, если он залогинен
        val sessionManager = com.example.kleos.data.auth.SessionManager(requireContext())
        val currentUser = sessionManager.getCurrentUser()
        if (currentUser != null && currentUser.email.isNotBlank()) {
            binding.emailEditText.setText(currentUser.email)
        }

        // Load countries for nationality dropdown
        loadCountriesAndConsent()
        
        // Обработка клика на ссылку согласия
        binding.consentLinkText.setOnClickListener {
            showConsentDialog()
        }
        
        // Включаем кнопку отправки по умолчанию
        binding.submitButton.isEnabled = true
        
        binding.submitButton.setOnClickListener {
            // Анимация нажатия на кнопку
            AnimationUtils.pressButton(binding.submitButton)
            
            if (!isAdded || _binding == null) return@setOnClickListener
            
            val firstName = binding.firstNameEditText.text?.toString().orEmpty()
            val lastName = binding.lastNameEditText.text?.toString().orEmpty()
            val patronymic = binding.patronymicEditText.text?.toString()?.takeIf { it.isNotBlank() }
            val dateOfBirth = binding.dateOfBirthEditText.text?.toString()
            val placeOfBirth = binding.placeOfBirthEditText.text?.toString()
            val nationality = (binding.nationalityEditText as? MaterialAutoCompleteTextView)?.text?.toString()
            val email = binding.emailEditText.text?.toString().orEmpty()
            val phone = binding.phoneEditText.text?.toString().orEmpty()
            val passportNumber = binding.passportNumberEditText.text?.toString()?.takeIf { it.isNotBlank() }
            val passportIssue = binding.passportIssueEditText.text?.toString()?.takeIf { it.isNotBlank() }
            val passportExpiry = binding.passportExpiryEditText.text?.toString()?.takeIf { it.isNotBlank() }
            val visaCity = binding.visaCityEditText.text?.toString()?.takeIf { it.isNotBlank() }
            val program = binding.programEditText.text?.toString().orEmpty()
            val comment = binding.commentEditText.text?.toString()?.takeIf { it.isNotBlank() }
            
            // Валидация обязательных полей
            if (firstName.isBlank() || lastName.isBlank() || dateOfBirth.isNullOrBlank() || placeOfBirth.isNullOrBlank() || nationality.isNullOrBlank() || email.isBlank() || phone.isBlank()) {
                if (isAdded && _binding != null) {
                    AnimationUtils.releaseButton(binding.submitButton)
                    AnimationUtils.shake(binding.submitButton)
                    Toast.makeText(requireContext(), "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }
            
            // Валидация формата email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (isAdded && _binding != null) {
                    AnimationUtils.releaseButton(binding.submitButton)
                    AnimationUtils.shake(binding.emailEditText)
                    Toast.makeText(requireContext(), "Введите корректный email", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }
            
            // Валидация формата телефона (минимум 10 цифр)
            val phoneDigits = phone.filter { it.isDigit() }
            if (phoneDigits.length < 10) {
                if (isAdded && _binding != null) {
                    AnimationUtils.releaseButton(binding.submitButton)
                    AnimationUtils.shake(binding.phoneEditText)
                    Toast.makeText(requireContext(), "Введите корректный номер телефона", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }
            
            // Валидация чекбокса согласия
            if (!binding.consentCheckBox.isChecked) {
                if (isAdded && _binding != null) {
                    AnimationUtils.releaseButton(binding.submitButton)
                    AnimationUtils.shake(binding.consentCheckBox)
                    Toast.makeText(requireContext(), "Необходимо согласие на обработку персональных данных", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }
            
            val application = AdmissionApplication(
                id = UUID.randomUUID().toString(),
                firstName = firstName,
                lastName = lastName,
                patronymic = patronymic,
                phone = phone,
                email = email,
                dateOfBirth = dateOfBirth,
                placeOfBirth = placeOfBirth,
                nationality = nationality,
                passportNumber = passportNumber,
                passportIssue = passportIssue,
                passportExpiry = passportExpiry,
                visaCity = visaCity,
                program = program,
                comment = comment
            )
            admissionsRepository.submit(application)
            
            if (!isAdded || _binding == null) return@setOnClickListener
            
            // Анимация успешной отправки
            AnimationUtils.releaseButton(binding.submitButton)
            AnimationUtils.pulse(binding.submitButton, 300)
            
            Toast.makeText(requireContext(), "Заявка отправлена", Toast.LENGTH_SHORT).show()
            
            // Очистка полей после отправки
            editTexts.forEach { editText ->
                if (!editText.text.isNullOrBlank() && isAdded) {
                    editText.setText("")
                }
            }
            // Сбрасываем чекбокс согласия
            binding.consentCheckBox.isChecked = false
        }
    }

    private fun loadCountriesAndConsent() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsApi = ApiClient.retrofit.create(SettingsApi::class.java)
                val countriesResponse = settingsApi.getCountries()
                countriesList = countriesResponse.countries
                
                val lang = resources.configuration.locales[0]?.language ?: "en"
                val consentLang = if (lang == "ru") "ru" else "en"
                val consentResponse = settingsApi.getConsentText(consentLang)
                
                if (lang == "ru") {
                    consentTextRu = consentResponse.text
                } else {
                    consentTextEn = consentResponse.text
                }
                
                // Also load the other language for dialog
                val otherLang = if (lang == "ru") "en" else "ru"
                val otherConsentResponse = settingsApi.getConsentText(otherLang)
                if (otherLang == "ru") {
                    consentTextRu = otherConsentResponse.text
                } else {
                    consentTextEn = otherConsentResponse.text
                }
                
                // Setup nationality dropdown on main thread
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        setupNationalityDropdown()
                    }
                }
            } catch (e: Exception) {
                // Fallback: use default countries if API fails
                countriesList = listOf("Russia", "USA", "China", "Germany", "France", "UK", "Italy", "Spain", "Japan", "South Korea")
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        setupNationalityDropdown()
                    }
                }
            }
        }
    }
    
    private fun setupNationalityDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countriesList)
        (binding.nationalityEditText as? MaterialAutoCompleteTextView)?.setAdapter(adapter)
    }
    
    private fun showConsentDialog() {
        val lang = resources.configuration.locales[0]?.language ?: "en"
        val consentText = if (lang == "ru") {
            if (consentTextRu.isNotBlank()) consentTextRu else consentTextEn
        } else {
            if (consentTextEn.isNotBlank()) consentTextEn else consentTextRu
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireContext().t(com.example.kleos.R.string.consent_label))
            .setMessage(consentText.ifBlank { "Consent text not available" })
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Показываем bottom navigation обратно при выходе со страницы
        showBottomNavigation()
        _binding = null
    }
    
    override fun onResume() {
        super.onResume()
        // Скрываем bottom navigation при возврате на страницу
        hideBottomNavigation()
    }
    
    override fun onPause() {
        super.onPause()
        // Показываем bottom navigation при уходе со страницы
        showBottomNavigation()
    }
    
    private fun hideBottomNavigation() {
        activity?.findViewById<com.example.kleos.ui.common.CustomBottomNavView>(com.example.kleos.R.id.bottom_nav)?.visibility = android.view.View.GONE
    }

    private fun showBottomNavigation() {
        activity?.findViewById<com.example.kleos.ui.common.CustomBottomNavView>(com.example.kleos.R.id.bottom_nav)?.visibility = android.view.View.VISIBLE
    }

    private fun applyDateMask(editText: TextInputEditText) {
        editText.filters = arrayOf(InputFilter.LengthFilter(10))
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val raw = s?.toString()?.replace(Regex("[^\\d]"), "").orEmpty()
                val sb = StringBuilder()
                for (i in raw.indices) {
                    if (i >= 8) break
                    sb.append(raw[i])
                    if ((i == 1 || i == 3) && i != raw.lastIndex) {
                        sb.append('.')
                    }
                }
                val formatted = sb.toString()
                if (formatted == s?.toString()) return
                isUpdating = true
                editText.setText(formatted)
                editText.setSelection(formatted.length.coerceAtMost(editText.text?.length ?: 0).coerceAtLeast(0))
                isUpdating = false
            }
        })
    }
}


