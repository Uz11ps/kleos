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

        // Анимации появления элементов
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.titleText, 600, 0)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.emailInputLayout, 500, 100)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.passwordInputLayout, 500, 200)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.forgotPasswordText, 400, 300)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.bottomBar, 500, 400)
        
        // Анимации для кнопок при нажатии
        binding.submitButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.example.kleos.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.example.kleos.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }
        
        binding.toggleModeButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    com.example.kleos.ui.utils.AnimationUtils.pressButton(view)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    com.example.kleos.ui.utils.AnimationUtils.releaseButton(view)
                }
            }
            false
        }

        // Try HTTP repository first; fallback to Local if it fails later.
        authRepository = AuthRepository.Http(this)

        binding.forgotPasswordText.setOnClickListener {
            Toast.makeText(this, getString(com.example.kleos.R.string.forgot_password_not_impl), Toast.LENGTH_SHORT).show()
        }
        binding.guestText.setOnClickListener {
            // Вход как гость: создаём сессию с именем guest и техническим токеном
            val session = SessionManager(this)
            session.saveUser(fullName = getString(com.example.kleos.R.string.guest), email = "guest@local")
            session.saveToken(java.util.UUID.randomUUID().toString())
            proceedToMain()
        }
        // Клик по всей строке ссылок на гостевой вход
        binding.linksRow.setOnClickListener { binding.guestText.performClick() }

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

    override fun onResume() {
        super.onResume()
        // Если уже авторизованы (включая гостя), сразу на главную
        if (SessionManager(this).isLoggedIn()) {
            proceedToMain()
        }
    }

    private fun proceedToMain(showInviteFallback: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java)
        if (showInviteFallback) {
            intent.putExtra("show_invite", true)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        overridePendingTransition(com.example.kleos.R.anim.slide_in_from_right, com.example.kleos.R.anim.fade_out)
    }

    private fun renderMode() {
        val showFullName = isRegisterMode
        
        // Анимация переключения режима
        if (showFullName && binding.fullNameInputLayout.visibility != View.VISIBLE) {
            binding.fullNameInputLayout.alpha = 0f
            binding.fullNameInputLayout.visibility = View.VISIBLE
            com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.fullNameInputLayout, 400)
        } else if (!showFullName && binding.fullNameInputLayout.visibility == View.VISIBLE) {
            com.example.kleos.ui.utils.AnimationUtils.fadeOutScale(binding.fullNameInputLayout, 300) {
                binding.fullNameInputLayout.visibility = View.GONE
            }
        }
        
        val underline = binding.root.findViewById<View>(com.example.kleos.R.id.fullNameUnderline)
        underline?.visibility = if (showFullName) View.VISIBLE else View.GONE
        
        // Анимация изменения заголовка
        com.example.kleos.ui.utils.AnimationUtils.rotateIn(binding.titleText, 400)
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
        
        // Валидация перед отправкой запроса
        if (email.isBlank()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isBlank()) {
            Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.submitButton.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { authRepository.login(email, password) }
            binding.submitButton.isEnabled = true
            if (result.isSuccess) {
                proceedToMain(true)
            } else {
                // Если почта не подтверждена — отправляем на экран подтверждения
                val httpEx = result.exceptionOrNull() as? retrofit2.HttpException
                val serverError = try {
                    val body = httpEx?.response()?.errorBody()?.string()
                    if (!body.isNullOrEmpty()) org.json.JSONObject(body).optString("error") else ""
                } catch (_: Exception) { "" }
                if (serverError == "email_not_verified") {
                    val intent = Intent(this@AuthActivity, com.example.kleos.ui.verify.VerifyEmailActivity::class.java)
                        .putExtra("email", email)
                    startActivity(intent)
                } else {
                    // Фоллбэк в оффлайн-режим допустим для прочих ошибок
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
                // Переходим на экран ожидания подтверждения почты
                val intent = Intent(this@AuthActivity, com.example.kleos.ui.verify.VerifyEmailActivity::class.java)
                    .putExtra("email", email)
                startActivity(intent)
                finish()
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


