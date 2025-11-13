package com.example.kleos.ui.admission

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.example.kleos.data.admissions.AdmissionsRepository
import com.example.kleos.data.model.AdmissionApplication
import com.example.kleos.databinding.FragmentAdmissionFormBinding
import java.util.UUID
import com.example.kleos.ui.language.t

class AdmissionFormFragment : Fragment() {

    private var _binding: FragmentAdmissionFormBinding? = null
    private val binding get() = _binding!!

    private lateinit var admissionsRepository: AdmissionsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdmissionFormBinding.inflate(inflater, container, false)
        admissionsRepository = AdmissionsRepository.Local(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyDateMask(binding.dateOfBirthEditText)
        applyDateMask(binding.passportExpiryEditText)

        // Phone mask per language
        val lang = resources.configuration.locales[0]?.language ?: "en"
        binding.phoneEditText.addTextChangedListener(
            com.example.kleos.ui.common.PhoneMaskTextWatcher(binding.phoneEditText, lang)
        )
        // Apply dynamic i18n overrides to hints (if provided from admin)
        binding.fullNameEditText.hint = requireContext().t(com.example.kleos.R.string.label_full_name)
        binding.emailEditText.hint = requireContext().t(com.example.kleos.R.string.label_email)

        binding.submitButton.setOnClickListener {
            val fullName = binding.fullNameEditText.text?.toString().orEmpty()
            val phone = binding.phoneEditText.text?.toString().orEmpty()
            val email = binding.emailEditText.text?.toString().orEmpty()
            val program = binding.programEditText.text?.toString().orEmpty()
            val comment = binding.commentEditText.text?.toString()
            if (fullName.isBlank() || phone.isBlank() || email.isBlank() || program.isBlank()) {
                Toast.makeText(requireContext(), "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val application = AdmissionApplication(
                id = UUID.randomUUID().toString(),
                fullName = fullName,
                phone = phone,
                email = email,
                program = program,
                comment = comment
            )
            admissionsRepository.submit(application)
            Toast.makeText(requireContext(), "Заявка отправлена", Toast.LENGTH_SHORT).show()
            binding.fullNameEditText.setText("")
            binding.phoneEditText.setText("")
            binding.emailEditText.setText("")
            binding.programEditText.setText("")
            binding.commentEditText.setText("")
        }
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


