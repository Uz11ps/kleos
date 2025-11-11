package com.example.kleos.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kleos.data.auth.AuthRepository
import com.example.kleos.databinding.ActivityAuthBinding
import android.content.Intent
import com.example.kleos.MainActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var authRepository: AuthRepository
    private var isRegisterMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Try HTTP repository first; fallback to Local if it fails later.
        authRepository = AuthRepository.Http(this)

        binding.toggleModeButton.setOnClickListener {
            isRegisterMode = !isRegisterMode
            renderMode()
        }

        binding.submitButton.setOnClickListener {
            if (isRegisterMode) {
                performRegister()
            } else {
                performLogin()
            }
        }

        renderMode()
    }

    private fun proceedToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun renderMode() {
        binding.fullNameInputLayout.visibility = if (isRegisterMode) View.VISIBLE else View.GONE
        binding.submitButton.text = if (isRegisterMode) {
            getString(com.example.kleos.R.string.action_register)
        } else {
            getString(com.example.kleos.R.string.action_login)
        }
        binding.toggleModeButton.text = if (isRegisterMode) {
            getString(com.example.kleos.R.string.switch_to_login)
        } else {
            getString(com.example.kleos.R.string.switch_to_register)
        }
    }

    private fun performLogin() {
        val email = binding.emailEditText.text?.toString().orEmpty()
        val password = binding.passwordEditText.text?.toString().orEmpty()
        val result = authRepository.login(email, password)
        if (result.isSuccess) {
            proceedToMain()
        } else {
            Toast.makeText(this, result.exceptionOrNull()?.message ?: "Ошибка входа", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performRegister() {
        val fullName = binding.fullNameEditText.text?.toString().orEmpty()
        val email = binding.emailEditText.text?.toString().orEmpty()
        val password = binding.passwordEditText.text?.toString().orEmpty()
        val result = authRepository.register(fullName, email, password)
        if (result.isSuccess) {
            proceedToMain()
        } else {
            Toast.makeText(this, result.exceptionOrNull()?.message ?: "Ошибка регистрации", Toast.LENGTH_SHORT).show()
        }
    }
}


