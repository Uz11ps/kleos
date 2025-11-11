package com.example.kleos.ui.admission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kleos.data.admissions.AdmissionsRepository
import com.example.kleos.data.model.AdmissionApplication
import com.example.kleos.databinding.FragmentAdmissionFormBinding
import java.util.UUID

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
}


