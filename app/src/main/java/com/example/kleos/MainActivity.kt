package com.example.kleos

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import com.example.kleos.ui.common.CustomBottomNavView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.kleos.databinding.ActivityMainBinding
import com.example.kleos.databinding.DialogInviteBinding
import com.example.kleos.data.auth.SessionManager
import com.example.kleos.ui.auth.AuthActivity
import com.example.kleos.ui.language.LocaleManager
import com.example.kleos.data.auth.AuthRepository
import com.example.kleos.ui.language.t
import com.example.kleos.data.profile.ProfileRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayUseLogoEnabled(false)
        supportActionBar?.hide() // Скрываем toolbar для нового дизайна
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        
        // Устанавливаем ширину NavigationView равной ширине экрана, чтобы убрать щель справа
        navView.post {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val layoutParams = navView.layoutParams
            layoutParams.width = screenWidth
            navView.layoutParams = layoutParams
        }
        
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_news,
                R.id.nav_gallery,
                R.id.nav_universities,
                R.id.nav_slideshow,
                R.id.nav_profile,
                R.id.nav_partners,
                R.id.nav_chat,
                R.id.nav_admission
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Убеждаемся, что галерея и университеты видны в боковом меню сразу после настройки
        navView.post {
            navView.menu.findItem(R.id.nav_gallery)?.isVisible = true
            navView.menu.findItem(R.id.nav_universities)?.isVisible = true
        }
        // Обработка кнопки закрытия в header
        runCatching {
            val header = navView.getHeaderView(0)
            val closeButton = header.findViewById<android.widget.ImageButton>(R.id.closeButton)
            closeButton?.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }.onFailure { /* ignore */ }
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_sign_out -> {
                    AuthRepository.Local(this).logout()
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_lang_ru -> {
                    LocaleManager.setLocale(this, "ru")
                    restartToMain()
                    true
                }
                R.id.menu_lang_en -> {
                    LocaleManager.setLocale(this, "en")
                    restartToMain()
                    true
                }
                R.id.menu_lang_zh -> {
                    LocaleManager.setLocale(this, "zh")
                    restartToMain()
                    true
                }
                R.id.nav_chat, R.id.nav_profile, R.id.nav_admission -> {
                    // Проверяем доступ перед навигацией
                    val isLoggedIn = sessionManager.isLoggedIn()
                    val currentUser = sessionManager.getCurrentUser()
                    val isGuest = !isLoggedIn || currentUser?.email == "guest@local"
                    val userRole = sessionManager.getUserRole()
                    
                    val hasChatAccess = !isGuest && (userRole == "user" || userRole == "student")
                    val hasProfileAccess = !isGuest && userRole == "student"
                    val hasAdmissionAccess = !isGuest && (userRole == "user" || userRole == "student")
                    
                    when (item.itemId) {
                        R.id.nav_chat -> {
                            if (hasChatAccess) {
                                val handled = item.onNavDestinationSelected(navController)
                                if (handled) {
                                    binding.drawerLayout.closeDrawers()
                                }
                                handled
                            } else {
                                Snackbar.make(binding.root, "Доступ запрещен. Пожалуйста, войдите в систему.", Snackbar.LENGTH_SHORT).show()
                                binding.drawerLayout.closeDrawers()
                                false
                            }
                        }
                        R.id.nav_profile -> {
                            if (hasProfileAccess) {
                                val handled = item.onNavDestinationSelected(navController)
                                if (handled) {
                                    binding.drawerLayout.closeDrawers()
                                }
                                handled
                            } else {
                                Snackbar.make(binding.root, "Доступ только для студентов.", Snackbar.LENGTH_SHORT).show()
                                binding.drawerLayout.closeDrawers()
                                false
                            }
                        }
                        R.id.nav_admission -> {
                            if (hasAdmissionAccess) {
                                val handled = item.onNavDestinationSelected(navController)
                                if (handled) {
                                    binding.drawerLayout.closeDrawers()
                                }
                                handled
                            } else {
                                Snackbar.make(binding.root, "Доступ запрещен. Пожалуйста, войдите в систему.", Snackbar.LENGTH_SHORT).show()
                                binding.drawerLayout.closeDrawers()
                                false
                            }
                        }
                        else -> false
                    }
                }
                else -> {
                    val handled = item.onNavDestinationSelected(navController)
                    if (handled) {
                        binding.drawerLayout.closeDrawers()
                    }
                    handled
                }
            }
        }

        val bottomNav: CustomBottomNavView = findViewById(R.id.bottom_nav)
        
        // Определяем, является ли пользователь гостем
        val isLoggedIn = sessionManager.isLoggedIn()
        val currentUser = sessionManager.getCurrentUser()
        val isGuest = !isLoggedIn || currentUser?.email == "guest@local"
        
        // Устанавливаем режим гостя для навбара
        bottomNav.setGuestMode(isGuest)
        
        // Настраиваем обработчик выбора элементов навигации
        bottomNav.setOnItemSelectedListener { position ->
            val userRole = sessionManager.getUserRole()
            val hasAdmissionAccess = !isGuest && (userRole == "user" || userRole == "student")
            
            if (isGuest) {
                // Для гостя: позиция 0 = дом, позиция 1 = галерея
                when (position) {
                    0 -> navController.navigate(R.id.nav_home)
                    1 -> navController.navigate(R.id.nav_gallery)
                }
            } else {
                // Для зарегистрированных пользователей: позиция 0 = университет, 1 = дом, 2 = галерея
                when (position) {
                    0 -> navController.navigate(R.id.nav_universities)
                    1 -> navController.navigate(R.id.nav_home)
                    2 -> navController.navigate(R.id.nav_gallery)
                }
            }
        }
        
        // Синхронизируем выбранный элемент с текущим destination и управляем видимостью нижней навигации
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isLoggedInNow = sessionManager.isLoggedIn()
            val currentUserNow = sessionManager.getCurrentUser()
            val isGuestNow = !isLoggedInNow || currentUserNow?.email == "guest@local"
            
            // Обновляем режим гостя при изменении destination
            bottomNav.setGuestMode(isGuestNow)
            
            // Скрываем нижнюю навигацию только на странице профиля
            if (destination.id == R.id.nav_profile) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
                if (isGuestNow) {
                    // Для гостя: логическая позиция 0 = дом, логическая позиция 1 = галерея
                    when (destination.id) {
                        R.id.nav_home -> bottomNav.setSelectedItem(0) // Логическая позиция 0 для дома
                        R.id.nav_gallery -> bottomNav.setSelectedItem(1) // Логическая позиция 1 для галереи
                    }
                } else {
                    // Для зарегистрированных пользователей
                    when (destination.id) {
                        R.id.nav_universities -> bottomNav.setSelectedItem(0)
                        R.id.nav_home -> bottomNav.setSelectedItem(1)
                        R.id.nav_gallery -> bottomNav.setSelectedItem(2)
                    }
                }
            }
        }
        
        // Синхронизируем начальную позицию с текущим destination
        val currentDestination = navController.currentDestination?.id
        
        if (currentDestination == R.id.nav_profile) {
            bottomNav.visibility = View.GONE
        } else {
            bottomNav.visibility = View.VISIBLE
            if (isGuest) {
                // Для гостя: логическая позиция 0 = дом, логическая позиция 1 = галерея
                when (currentDestination) {
                    R.id.nav_home -> bottomNav.setSelectedItem(0) // Логическая позиция 0 для дома
                    R.id.nav_gallery -> bottomNav.setSelectedItem(1) // Логическая позиция 1 для галереи
                }
            } else {
                // Для зарегистрированных пользователей
                when (currentDestination) {
                    R.id.nav_universities -> bottomNav.setSelectedItem(0)
                    R.id.nav_home -> bottomNav.setSelectedItem(1)
                    R.id.nav_gallery -> bottomNav.setSelectedItem(2)
                }
            }
        }
        
        // Загружаем профиль пользователя, если он залогинен, и обновляем видимость меню
        if (sessionManager.isLoggedIn()) {
            // Отправляем FCM токен на сервер (если еще не отправлен)
            com.example.kleos.utils.FcmTokenManager.registerToken(this)
            
            lifecycleScope.launch {
                runCatching {
                    val profile = ProfileRepository().getProfile()
                    sessionManager.saveRole(profile.role)
                }.onFailure { /* ignore errors */ }
                updateMenuVisibility()
            }
        } else {
            updateMenuVisibility()
        }

        // Показ приглашения после входа отключен

        // Автопереход на форму поступления с выбранной программой
        val prefillProgram = intent.getStringExtra("prefill_program")
        if (!prefillProgram.isNullOrBlank()) {
            // Перейдём на экран подачи заявки
            navController.navigate(R.id.nav_admission)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
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
                putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_share_text))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.invite_share_title)))
            dialog.dismiss()
            onClose()
        }
        dialog.show()
    }

    private fun restartToMain() {
        binding.drawerLayout.closeDrawers()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем видимость меню при возврате на экран (на случай изменения роли)
        updateMenuVisibility()
    }
    
    fun updateMenuVisibility() {
        val navView: NavigationView = binding.navView
        val bottomNav: CustomBottomNavView = findViewById(R.id.bottom_nav)
        val isLoggedIn = sessionManager.isLoggedIn()
        val currentUser = sessionManager.getCurrentUser()
        
        // Проверяем, является ли пользователь гостем
        val isGuest = !isLoggedIn || currentUser?.email == "guest@local"
        val userRole = sessionManager.getUserRole()
        
        // Логика доступа:
        // - Гость (не залогинен или guest@local): нет доступа к чату, профилю и поступлению
        // - Пользователь (role = "user"): есть доступ к чату и поступлению, но нет доступа к профилю
        // - Студент (role = "student"): есть доступ и к чату, и к профилю, и к поступлению
        
        val hasChatAccess = !isGuest && (userRole == "user" || userRole == "student")
        val hasProfileAccess = !isGuest && userRole == "student"
        val hasAdmissionAccess = !isGuest && (userRole == "user" || userRole == "student")
        
        // Управление видимостью пунктов меню в боковом меню (drawer)
        // Галерея, Новости, Университеты, Программы, Партнеры всегда видны для всех пользователей
        navView.menu.findItem(R.id.nav_gallery)?.isVisible = true
        navView.menu.findItem(R.id.nav_news)?.isVisible = true
        navView.menu.findItem(R.id.nav_universities)?.isVisible = true
        navView.menu.findItem(R.id.nav_slideshow)?.isVisible = true
        navView.menu.findItem(R.id.nav_partners)?.isVisible = true
        
        // Поддержка (чат) - только для залогиненных пользователей (user или student)
        navView.menu.findItem(R.id.nav_chat)?.isVisible = hasChatAccess
        
        // Профиль - только для студентов
        navView.menu.findItem(R.id.nav_profile)?.isVisible = hasProfileAccess
        
        // Допуск (поступление) - только для залогиненных пользователей (user или student)
        navView.menu.findItem(R.id.nav_admission)?.isVisible = hasAdmissionAccess
        
        // Управление видимостью нижней навигации
        // Нижнее меню показываем всем пользователям (включая гостей), кроме страницы профиля
        val currentDestination = findNavController(R.id.nav_host_fragment_content_main).currentDestination?.id
        
        // Устанавливаем режим гостя для навбара
        bottomNav.setGuestMode(isGuest)
        
        if (currentDestination == R.id.nav_profile) {
            bottomNav.visibility = View.GONE
        } else {
            bottomNav.visibility = View.VISIBLE
            // Синхронизируем выбранный элемент
            if (isGuest) {
                when (currentDestination) {
                    R.id.nav_home -> bottomNav.setSelectedItem(0) // Логическая позиция 0 для дома
                    R.id.nav_gallery -> bottomNav.setSelectedItem(1) // Логическая позиция 1 для галереи
                }
            } else {
                when (currentDestination) {
                    R.id.nav_universities -> bottomNav.setSelectedItem(0)
                    R.id.nav_home -> bottomNav.setSelectedItem(1)
                    R.id.nav_gallery -> bottomNav.setSelectedItem(2)
                }
            }
        }
    }
    
    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }
}