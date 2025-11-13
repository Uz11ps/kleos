package com.example.kleos.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.kleos.data.auth.AuthRepository
import com.example.kleos.data.auth.SessionManager
import com.example.kleos.databinding.FragmentProfileBinding
import com.example.kleos.ui.auth.AuthActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
        val user = session.getCurrentUser()
        binding.nameEditText.setText(user?.fullName ?: "")
        binding.notesEditText.setText("")

        // Phone mask per language
        val lang = resources.configuration.locales[0]?.language ?: "en"
        binding.phoneEditText.addTextChangedListener(
            com.example.kleos.ui.common.PhoneMaskTextWatcher(binding.phoneEditText, lang)
        )

        binding.logoutButton.setOnClickListener {
            AuthRepository.Local(requireContext()).logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


