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

        // Автозаполнение email из сессии пользователя
        val sessionManager = com.example.kleos.data.auth.SessionManager(requireContext())
        val currentUser = sessionManager.getCurrentUser()
        if (!currentUser?.email.isNullOrBlank() && binding.emailEditText.text.isNullOrBlank()) {
            binding.emailEditText.setText(currentUser?.email)
        }

        // Анимация появления заголовка
        val titleView = binding.root.findViewById<View>(com.example.kleos.R.id.titleText)
        titleView?.let { AnimationUtils.slideUpFade(it, 0) }
        
        // Безопасное получение parent элементов для анимации
        fun getParentLayout(editText: View): ViewGroup? {
            val parent = editText.parent
            return if (parent is com.google.android.material.textfield.TextInputLayout) {
                parent as ViewGroup
            } else {
                null
            }
        }
        
        // Анимация появления всех инпутов с задержкой
        val inputLayouts = mutableListOf<ViewGroup>()
        
        try {
            listOf(
                binding.firstNameEditText,
                binding.lastNameEditText,
                binding.patronymicEditText,
                binding.dateOfBirthEditText,
                binding.nationalityEditText,
                binding.passportNumberEditText,
                binding.phoneEditText,
                binding.emailEditText,
                binding.passportIssueEditText,
                binding.visaCityEditText,
                binding.passportExpiryEditText,
                binding.programEditText,
                binding.commentEditText
            ).forEach { editText ->
                getParentLayout(editText)?.let { inputLayouts.add(it) }
            }
        } catch (e: Exception) {
            // Игнорируем ошибки при получении parent элементов
        }
        
        inputLayouts.forEachIndexed { index, layout ->
            AnimationUtils.slideUpFade(layout, (index * 50).toLong())
        }
        
        // Анимация появления кнопки
        view.postDelayed({
            if (isAdded && _binding != null) {
                AnimationUtils.bounceIn(binding.submitButton, inputLayouts.size * 50L)
            }
        }, 100)

        applyDateMask(binding.dateOfBirthEditText)
        applyDateMask(binding.passportExpiryEditText)

        // Автоподстановка выбранной программы из Intent/аргументов
        val argProgram = arguments?.getString("program")
            ?: requireActivity().intent?.getStringExtra("prefill_program")
        if (!argProgram.isNullOrBlank() && binding.programEditText.text.isNullOrBlank()) {
            binding.programEditText.setText(argProgram)
        }

        if (!isAdded) return

        // Load countries and consent text
        loadCountriesAndConsent()

        // Phone mask per language
        val lang = resources.configuration.locales[0]?.language ?: "en"
        binding.phoneEditText.addTextChangedListener(
            com.example.kleos.ui.common.PhoneMaskTextWatcher(binding.phoneEditText, lang)
        )
        // Apply dynamic i18n overrides to hints (if provided from admin)
        if (isAdded) {
            binding.firstNameEditText.hint = requireContext().t(com.example.kleos.R.string.label_first_name)
            binding.lastNameEditText.hint = requireContext().t(com.example.kleos.R.string.label_last_name)
            binding.patronymicEditText.hint = requireContext().t(com.example.kleos.R.string.label_patronymic)
            binding.emailEditText.hint = requireContext().t(com.example.kleos.R.string.label_email)
        }
        
        // Setup consent checkbox listener
        binding.consentCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.submitButton.isEnabled = isChecked
        }
        
        // Setup consent link click listener
        binding.consentLinkText.setOnClickListener {
            showConsentDialog()
        }
        
        // Add underline to consent link text programmatically
        binding.consentLinkText.paintFlags = android.graphics.Paint.UNDERLINE_TEXT_FLAG

        // Анимация при фокусе на инпутах
        val editTexts = listOf(
            binding.firstNameEditText,
            binding.lastNameEditText,
            binding.patronymicEditText,
            binding.dateOfBirthEditText,
            binding.phoneEditText,
            binding.emailEditText,
            binding.passportExpiryEditText,
            binding.programEditText,
            binding.commentEditText,
            binding.nationalityEditText,
            binding.passportNumberEditText,
            binding.passportIssueEditText,
            binding.visaCityEditText
        )
        
        editTexts.forEach { editText ->
            editText.setOnFocusChangeListener { v, hasFocus ->
                if (!isAdded) return@setOnFocusChangeListener
                
                val parent = v.parent
                if (parent is View) {
                    if (hasFocus) {
                        // Плавное увеличение при фокусе
                        parent.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(300)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    } else {
                        // Плавное уменьшение при потере фокуса
                        parent.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                }
            }
        }
        
        binding.submitButton.setOnClickListener {
            // Анимация нажатия на кнопку
            AnimationUtils.pressButton(binding.submitButton)
            
            if (!isAdded || _binding == null) return@setOnClickListener
            
            val firstName = binding.firstNameEditText.text?.toString().orEmpty()
            val lastName = binding.lastNameEditText.text?.toString().orEmpty()
            val patronymic = binding.patronymicEditText.text?.toString()?.takeIf { it.isNotBlank() }
            val phone = binding.phoneEditText.text?.toString().orEmpty()
            val email = binding.emailEditText.text?.toString().orEmpty()
            val dateOfBirth = binding.dateOfBirthEditText.text?.toString()
            val placeOfBirth = binding.placeOfBirthEditText.text?.toString()
            val nationality = (binding.nationalityEditText as? MaterialAutoCompleteTextView)?.text?.toString()
            val passportNumber = binding.passportNumberEditText.text?.toString()
            val passportIssue = binding.passportIssueEditText.text?.toString()
            val program = binding.programEditText.text?.toString().orEmpty()
            val visaCity = binding.visaCityEditText.text?.toString()
            val comment = binding.commentEditText.text?.toString()
            if (firstName.isBlank() || lastName.isBlank() || phone.isBlank() || email.isBlank() || program.isBlank() || nationality.isNullOrBlank()) {
                if (isAdded && _binding != null) {
                    AnimationUtils.releaseButton(binding.submitButton)
                    AnimationUtils.shake(binding.submitButton)
                    Toast.makeText(requireContext(), "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }
            if (!binding.consentCheckBox.isChecked) {
                if (isAdded && _binding != null) {
                    AnimationUtils.releaseButton(binding.submitButton)
                    AnimationUtils.shake(binding.consentCheckBox)
                    Toast.makeText(requireContext(), "Необходимо согласие на обработку персональных данных", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (isAdded && _binding != null) {
                    AnimationUtils.releaseButton(binding.submitButton)
                    val emailParent = binding.emailEditText.parent
                    AnimationUtils.shake(if (emailParent is View) emailParent else binding.submitButton)
                    Toast.makeText(requireContext(), "Некорректный email", Toast.LENGTH_SHORT).show()
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
                passportExpiry = binding.passportExpiryEditText.text?.toString(),
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
            
            // Плавное скрытие заполненных полей
            editTexts.forEach { editText ->
                if (!editText.text.isNullOrBlank() && isAdded) {
                    editText.animate()
                        .alpha(0.3f)
                        .setDuration(200)
                        .withEndAction {
                            if (isAdded && _binding != null) {
                                editText.setText("")
                                editText.animate()
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start()
                            }
                        }
                        .start()
                }
            }
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
        _binding = null
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


