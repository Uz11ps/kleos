package com.example.kleos.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.kleos.data.auth.AuthRepository
import com.example.kleos.databinding.ActivityAuthBinding
import android.content.Intent
import com.example.kleos.MainActivity
import com.example.kleos.databinding.DialogInviteBinding
import androidx.lifecycle.lifecycleScope
import com.example.kleos.data.auth.SessionManager
import android.util.Patterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.kleos.ui.language.t

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

        binding.forgotPasswordText.setOnClickListener {
            Toast.makeText(this, getString(com.example.kleos.R.string.forgot_password_not_impl), Toast.LENGTH_SHORT).show()
        }
        binding.guestText.setOnClickListener {
            // Вход как гость: создаём сессию с именем guest и техническим токеном
            val session = SessionManager(this)
            session.saveUser(fullName = "guest", email = "guest@local")
            session.saveToken("guest_token")
            proceedToMain()
        }

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

    private fun proceedToMain(showInviteFallback: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java)
        if (showInviteFallback) {
            intent.putExtra("show_invite", true)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun renderMode() {
        val showFullName = isRegisterMode
        binding.fullNameInputLayout.visibility = if (showFullName) View.VISIBLE else View.GONE
        val underline = binding.root.findViewById<View>(com.example.kleos.R.id.fullNameUnderline)
        underline?.visibility = if (showFullName) View.VISIBLE else View.GONE
        binding.submitButton.text = if (isRegisterMode) {
            getString(com.example.kleos.R.string.action_register)
        } else {
            getString(com.example.kleos.R.string.action_login)
        }
        // Кнопка переключения режима показывает короткий текст целевого режима
        binding.toggleModeButton.text = if (isRegisterMode) {
            getString(com.example.kleos.R.string.action_login) // "Войти"
        } else {
            getString(com.example.kleos.R.string.action_register) // "Зарегистрироваться"
        }
        binding.titleText.text = if (isRegisterMode) this@AuthActivity.t(com.example.kleos.R.string.title_sign_up) else this@AuthActivity.t(com.example.kleos.R.string.title_sign_in)
        // По требованию: левую подсказку не показываем ни на Sign In, ни на Sign Up
        binding.leftHintText.visibility = View.GONE
    }

    private fun performLogin() {
        val email = binding.emailEditText.text?.toString().orEmpty()
        val password = binding.passwordEditText.text?.toString().orEmpty()
        binding.submitButton.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { authRepository.login(email, password) }
            binding.submitButton.isEnabled = true
            if (result.isSuccess) {
                proceedToMain(true)
            } else {
                // Фоллбэк в оффлайн-режим (локальная авторизация)
                val localRepo = AuthRepository.Local(this@AuthActivity)
                val localResult = withContext(Dispatchers.IO) { localRepo.login(email, password) }
                if (localResult.isSuccess) {
                    proceedToMain(true)
                } else {
                    Toast.makeText(this@AuthActivity, result.exceptionOrNull()?.message ?: "Ошибка входа", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performRegister() {
        val fullName = binding.fullNameEditText.text?.toString().orEmpty()
        val email = binding.emailEditText.text?.toString().orEmpty()
        val password = binding.passwordEditText.text?.toString().orEmpty()
        if (fullName.isBlank()) {
            Toast.makeText(this, "Введите ФИО", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }
        binding.submitButton.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { authRepository.register(fullName, email, password) }
            binding.submitButton.isEnabled = true
            if (result.isSuccess) {
                Toast.makeText(this@AuthActivity, "Проверьте почту и подтвердите email", Toast.LENGTH_LONG).show()
            } else {
                val msg = result.exceptionOrNull()?.let { ex ->
                    try {
                        val http = ex as? retrofit2.HttpException
                        val body = http?.response()?.errorBody()?.string()
                        val obj = if (body.isNullOrEmpty()) null else org.json.JSONObject(body)
                        when (obj?.optString("error")) {
                            "email_taken" -> "Email уже зарегистрирован"
                            "bad_request" -> "Некорректные данные"
                            else -> obj?.optString("error") ?: ex.message ?: "Ошибка регистрации"
                        }
                    } catch (_: Exception) {
                        ex.message ?: "Ошибка регистрации"
                    }
                } ?: "Ошибка регистрации"
                Toast.makeText(this@AuthActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInviteDialog(onClose: () -> Unit) {
        val dialogBinding = DialogInviteBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
            onClose()
        }
        dialogBinding.inviteButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getString(com.example.kleos.R.string.invite_share_text))
            }
            startActivity(Intent.createChooser(shareIntent, getString(com.example.kleos.R.string.invite_share_title)))
            dialog.dismiss()
            onClose()
        }
        dialog.show()
    }
}


