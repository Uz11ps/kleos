package com.example.kleos

import android.os.Bundle
import android.view.Menu
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
        
        // Настраиваем обработчик выбора элементов навигации
        bottomNav.setOnItemSelectedListener { position ->
            when (position) {
                0 -> navController.navigate(R.id.nav_admission)
                1 -> navController.navigate(R.id.nav_home)
                2 -> navController.navigate(R.id.nav_gallery)
            }
        }
        
        // Синхронизируем выбранный элемент с текущим destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_admission -> bottomNav.setSelectedItem(0)
                R.id.nav_home -> bottomNav.setSelectedItem(1)
                R.id.nav_gallery -> bottomNav.setSelectedItem(2)
            }
        }
        
        // Синхронизируем начальную позицию с текущим destination
        val currentDestination = navController.currentDestination?.id
        when (currentDestination) {
            R.id.nav_admission -> bottomNav.setSelectedItem(0)
            R.id.nav_home -> bottomNav.setSelectedItem(1)
            R.id.nav_gallery -> bottomNav.setSelectedItem(2)
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
        val userRole = sessionManager.getUserRole()
        
        // Логика доступа:
        // - Гость (не залогинен): нет доступа к чату, профилю и поступлению
        // - Пользователь (role = "user"): есть доступ к чату и поступлению, но нет доступа к профилю
        // - Студент (role = "student"): есть доступ и к чату, и к профилю, и к поступлению
        
        val hasChatAccess = isLoggedIn && (userRole == "user" || userRole == "student")
        val hasProfileAccess = isLoggedIn && userRole == "student"
        val hasAdmissionAccess = isLoggedIn && (userRole == "user" || userRole == "student")
        
        // Управление видимостью пунктов меню в боковом меню
        // Галерея и Университеты всегда видны для всех пользователей
        val galleryItem = navView.menu.findItem(R.id.nav_gallery)
        if (galleryItem != null) {
            galleryItem.isVisible = true
            galleryItem.isEnabled = true
        }
        val universitiesItem = navView.menu.findItem(R.id.nav_universities)
        if (universitiesItem != null) {
            universitiesItem.isVisible = true
            universitiesItem.isEnabled = true
        }
        // Поддержка и Допуск всегда видны в меню
        navView.menu.findItem(R.id.nav_chat)?.isVisible = true
        navView.menu.findItem(R.id.nav_profile)?.isVisible = hasProfileAccess
        navView.menu.findItem(R.id.nav_admission)?.isVisible = true
        
        // Управление видимостью пунктов меню в нижней навигации
        // Новый CustomBottomNavView всегда показывает все три элемента (кисть, дом, картинка)
        // Видимость управляется через навигацию - если пользователь не имеет доступа,
        // он просто не сможет перейти на соответствующий экран
    }
    
    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }
}