package com.example.kleos.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.kleos.data.auth.AuthRepository
import com.example.kleos.databinding.ActivityAuthBinding
import com.example.kleos.databinding.BottomSheetLoginBinding
import com.example.kleos.databinding.BottomSheetRegisterBinding
import com.example.kleos.databinding.BottomSheetForgotPasswordBinding
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.view.ViewCompat

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var authRepository: AuthRepository
    private var isRegisterMode: Boolean = false
    private var isWelcomeScreen: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Try HTTP repository first; fallback to Local if it fails later.
        authRepository = AuthRepository.Http(this)

        // Настройка welcome screen
        setupWelcomeScreen()
        
        // Настройка формы входа/регистрации
        setupFormScreen()
    }
    
    private fun setupWelcomeScreen() {
        // Анимации появления элементов welcome screen
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.welcomeTitle, 600, 0)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.welcomeSubtitle, 500, 100)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.bottomBar, 500, 200)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.menuButton, 400, 0)
        
        // Обработчик клика на бургер-меню
        binding.menuButton.setOnClickListener {
            val intent = Intent(this, com.example.kleos.ui.language.LanguageActivity::class.java)
            intent.putExtra("from_auth", true)
            startActivity(intent)
        }
        
        // Убеждаемся, что размытые круги видны
        binding.blurredCircleTop.visibility = View.VISIBLE
        binding.blurredCircle.visibility = View.VISIBLE
        
        // Настройка кнопок на welcome screen
        binding.submitButton.text = getString(com.example.kleos.R.string.action_login)
        binding.toggleModeButton.text = getString(com.example.kleos.R.string.action_register)
        
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
        
        // Обработчики кнопок на welcome screen
        binding.submitButton.setOnClickListener {
            // Открываем Bottom Sheet с формой входа
            showLoginBottomSheet()
        }
        
        binding.toggleModeButton.setOnClickListener {
            // Открываем Bottom Sheet с формой регистрации
            showRegisterBottomSheet()
        }
        
        // Обработчик клика на "Или войдите как гость"
        binding.guestLoginText.setOnClickListener {
            // Вход как гость: создаём сессию с именем guest и техническим токеном
            val session = SessionManager(this)
            session.saveUser(fullName = getString(com.example.kleos.R.string.guest), email = "guest@local")
            session.saveToken(java.util.UUID.randomUUID().toString())
            proceedToMain()
        }
    }
    
    private fun setupFormScreen() {
        binding.forgotPasswordText.setOnClickListener {
            showForgotPasswordBottomSheet()
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
    }
    
    private fun setupFormButtons() {
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
    }
    
    private fun showFormScreen() {
        // Скрываем welcome screen
        binding.welcomeContainer.visibility = View.GONE
        binding.gradientShape.visibility = View.GONE
        binding.blurredCircle.visibility = View.GONE
        binding.blurredCircleTop.visibility = View.GONE
        binding.menuButton.visibility = View.GONE
        
        // Показываем форму
        binding.scrollContent.visibility = View.VISIBLE
        binding.linksRow.visibility = View.VISIBLE
        
        // Меняем фон на светлый
        binding.root.setBackgroundColor(getColor(com.example.kleos.R.color.white))
        
        // Настраиваем обработчики кнопок для формы
        setupFormButtons()
        
        // Анимации появления элементов формы
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.titleText, 600, 0)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.emailInputLayout, 500, 100)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.passwordInputLayout, 500, 200)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.forgotPasswordText, 400, 300)
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.bottomBar, 500, 400)
        
        renderMode()
    }

    override fun onResume() {
        super.onResume()
        // Применяем сохраненный язык
        com.example.kleos.ui.language.LocaleManager.applySavedLocale(this)
        
        // Обновляем тексты интерфейса при возврате из LanguageActivity
        if (!isWelcomeScreen) {
            renderMode()
        } else {
            setupWelcomeScreen()
        }
        
        // Если уже авторизованы (включая гостя), сразу на главную
        if (SessionManager(this).isLoggedIn()) {
            proceedToMain()
        }
    }

    private fun proceedToMain(showInviteFallback: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java)
        // Убрано показ диалога приглашения
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        overridePendingTransition(com.example.kleos.R.anim.slide_in_from_right, com.example.kleos.R.anim.fade_out)
    }

    private fun renderMode() {
        if (isWelcomeScreen) return
        
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
        
        // Обновляем стили кнопок для формы
        binding.toggleModeButton.setStrokeColorResource(com.example.kleos.R.color.kleos_blue)
        binding.toggleModeButton.setTextColor(getColor(com.example.kleos.R.color.kleos_blue))
        binding.submitButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(com.example.kleos.R.color.kleos_blue)))
        binding.submitButton.setTextColor(getColor(android.R.color.white))
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
                proceedToMain()
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
                        proceedToMain()
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

    private fun applyBottomSheetStyle(bottomSheetDialog: BottomSheetDialog) {
        // Настройка стиля bottom sheet с закругленными углами и белым фоном
        // Убираем затемнение фона (overlay)
        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // Убираем затемнение через dimAmount
        bottomSheetDialog.window?.setDimAmount(0f)
        // Устанавливаем белый цвет для навигационной панели внизу
        bottomSheetDialog.window?.navigationBarColor = getColor(android.R.color.white)
        bottomSheetDialog.window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        // Применяем border radius сверху программно для гарантии
        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { view ->
                val cornerRadius = resources.getDimension(com.example.kleos.R.dimen.bottom_sheet_corner_radius)
                val shapeAppearanceModel = com.google.android.material.shape.ShapeAppearanceModel.builder()
                    .setTopLeftCorner(com.google.android.material.shape.CornerFamily.ROUNDED, cornerRadius)
                    .setTopRightCorner(com.google.android.material.shape.CornerFamily.ROUNDED, cornerRadius)
                    .setBottomLeftCorner(com.google.android.material.shape.CornerFamily.ROUNDED, 0f)
                    .setBottomRightCorner(com.google.android.material.shape.CornerFamily.ROUNDED, 0f)
                    .build()
                val materialShapeDrawable = com.google.android.material.shape.MaterialShapeDrawable(shapeAppearanceModel).apply {
                    setTint(getColor(android.R.color.white))
                }
                ViewCompat.setBackground(view, materialShapeDrawable)
            }
        }
    }

    private fun showLoginBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, com.example.kleos.R.style.Theme_Kleos_BottomSheetDialog)
        val sheetBinding = BottomSheetLoginBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)
        
        applyBottomSheetStyle(bottomSheetDialog)
        
        val behavior = bottomSheetDialog.behavior
        behavior.isDraggable = true
        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        
        // Настройка обработчиков
        sheetBinding.forgotPasswordText.setOnClickListener {
            bottomSheetDialog.dismiss()
            showForgotPasswordBottomSheet()
        }
        
        // Устанавливаем текст регистрации черным цветом
        sheetBinding.registerLink.text = getString(com.example.kleos.R.string.no_account_register)
        sheetBinding.registerLink.setTextColor(getColor(android.R.color.black))
        
        sheetBinding.registerLink.setOnClickListener {
            bottomSheetDialog.dismiss()
            // Открываем bottom sheet регистрации
            showRegisterBottomSheet()
        }
        
        sheetBinding.loginButton.setOnClickListener {
            val email = sheetBinding.emailEditText.text?.toString().orEmpty()
            val password = sheetBinding.passwordEditText.text?.toString().orEmpty()
            
            // Валидация
            if (email.isBlank()) {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isBlank()) {
                Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            sheetBinding.loginButton.isEnabled = false
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { authRepository.login(email, password) }
                    sheetBinding.loginButton.isEnabled = true
                    if (result.isSuccess) {
                        bottomSheetDialog.dismiss()
                        proceedToMain()
                    } else {
                        // Если почта не подтверждена — отправляем на экран подтверждения
                        val exception = result.exceptionOrNull()
                        val httpEx = exception as? retrofit2.HttpException
                        val serverError = try {
                            val body = httpEx?.response()?.errorBody()?.string()
                            if (!body.isNullOrEmpty()) org.json.JSONObject(body).optString("error") else ""
                        } catch (_: Exception) { "" }
                        
                        if (serverError == "email_not_verified") {
                            bottomSheetDialog.dismiss()
                            val intent = Intent(this@AuthActivity, com.example.kleos.ui.verify.VerifyEmailActivity::class.java)
                                .putExtra("email", email)
                            startActivity(intent)
                        } else if (serverError == "invalid_credentials") {
                            Toast.makeText(this@AuthActivity, "Неверный email или пароль", Toast.LENGTH_SHORT).show()
                        } else {
                            // Фоллбэк в оффлайн-режим допустим для прочих ошибок
                            val localRepo = AuthRepository.Local(this@AuthActivity)
                            val localResult = withContext(Dispatchers.IO) { localRepo.login(email, password) }
                            if (localResult.isSuccess) {
                                bottomSheetDialog.dismiss()
                                proceedToMain()
                            } else {
                                val errorMsg = when {
                                    !serverError.isNullOrEmpty() -> when (serverError) {
                                        "invalid_credentials" -> "Неверный email или пароль"
                                        else -> "Ошибка входа: $serverError"
                                    }
                                    exception != null -> exception.message ?: "Ошибка входа"
                                    else -> "Ошибка входа. Проверьте подключение к интернету"
                                }
                                Toast.makeText(this@AuthActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    sheetBinding.loginButton.isEnabled = true
                    Toast.makeText(this@AuthActivity, "Ошибка: ${e.message ?: "Неизвестная ошибка"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        bottomSheetDialog.show()
    }

    private fun showRegisterBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, com.example.kleos.R.style.Theme_Kleos_BottomSheetDialog)
        val sheetBinding = BottomSheetRegisterBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)
        
        applyBottomSheetStyle(bottomSheetDialog)
        
        val behavior = bottomSheetDialog.behavior
        behavior.isDraggable = true
        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        
        // Настройка обработчиков
        sheetBinding.loginLink.setOnClickListener {
            bottomSheetDialog.dismiss()
            // Открываем форму входа
            showLoginBottomSheet()
        }
        
        sheetBinding.registerButton.setOnClickListener {
            val login = sheetBinding.loginEditText.text?.toString().orEmpty()
            val email = sheetBinding.emailEditText.text?.toString().orEmpty()
            val password = sheetBinding.passwordEditText.text?.toString().orEmpty()
            val consent = sheetBinding.consentCheckbox.isChecked
            
            // Валидация
            if (login.isBlank()) {
                Toast.makeText(this, "Введите логин", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isBlank()) {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isBlank()) {
                Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!consent) {
                Toast.makeText(this, "Необходимо согласие на обработку данных", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            sheetBinding.registerButton.isEnabled = false
            lifecycleScope.launch {
                // Используем login как fullName для регистрации
                val result = withContext(Dispatchers.IO) { authRepository.register(login, email, password) }
                sheetBinding.registerButton.isEnabled = true
                if (result.isSuccess) {
                    bottomSheetDialog.dismiss()
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
        
        // Устанавливаем текст входа черным цветом
        sheetBinding.loginLink.text = getString(com.example.kleos.R.string.have_account_login)
        sheetBinding.loginLink.setTextColor(getColor(android.R.color.black))
        
        bottomSheetDialog.show()
    }

    private fun showForgotPasswordBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, com.example.kleos.R.style.Theme_Kleos_BottomSheetDialog)
        val sheetBinding = BottomSheetForgotPasswordBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)
        
        applyBottomSheetStyle(bottomSheetDialog)
        
        val behavior = bottomSheetDialog.behavior
        behavior.isDraggable = true
        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        
        // Обработчик кнопки сброса пароля
        sheetBinding.resetPasswordButton.setOnClickListener {
            val email = sheetBinding.emailEditText.text?.toString().orEmpty()
            
            // Валидация
            if (email.isBlank()) {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            sheetBinding.resetPasswordButton.isEnabled = false
            // TODO: Реализовать API вызов для сброса пароля
            Toast.makeText(this, getString(com.example.kleos.R.string.forgot_password_not_impl), Toast.LENGTH_SHORT).show()
            sheetBinding.resetPasswordButton.isEnabled = true
            // После успешной отправки можно закрыть окно
            // bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
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


