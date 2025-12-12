package com.example.kleos

import android.os.Bundle
import android.view.Menu
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
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
        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setLogo(R.drawable.kleos_fon)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_news,
                R.id.nav_gallery,
                R.id.nav_slideshow,
                R.id.nav_profile,
                R.id.nav_partners,
                R.id.nav_chat,
                R.id.nav_admission
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        // Заполним шапку бургера логином
        runCatching {
            val header = navView.getHeaderView(0)
            val greetTv = header.findViewById<android.widget.TextView>(R.id.headerGreetingText)
            val emailTv = header.findViewById<android.widget.TextView>(R.id.headerEmailText)
            val userNow = sessionManager.getCurrentUser()
            val nameNow = (userNow?.fullName?.takeIf { it.isNotBlank() } ?: getString(R.string.guest)).trim()
            val greetTmpl = this@MainActivity.t(R.string.hi_name)
            greetTv?.text = String.format(greetTmpl, nameNow)
            emailTv?.text = userNow?.email ?: ""
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

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        
        // Загружаем профиль пользователя, если он залогинен, и обновляем видимость меню
        if (sessionManager.isLoggedIn()) {
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

        // Показ приглашения после входа (если передан флаг из AuthActivity)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val shouldShowInvite = intent.getBooleanExtra("show_invite", false)
        if (shouldShowInvite) {
            showInviteDialog {
                prefs.edit().putBoolean("invite_dialog_shown", true).apply()
            }
        }

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
    
    private fun updateMenuVisibility() {
        val navView: NavigationView = binding.navView
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
        val isLoggedIn = sessionManager.isLoggedIn()
        val userRole = sessionManager.getUserRole()
        val isStudent = userRole == "student"
        
        // Управление видимостью пунктов меню в боковом меню
        navView.menu.findItem(R.id.nav_chat)?.isVisible = isLoggedIn
        navView.menu.findItem(R.id.nav_profile)?.isVisible = isStudent
        
        // Управление видимостью пунктов меню в нижней навигации
        bottomNav.menu.findItem(R.id.nav_chat)?.isVisible = isLoggedIn
        bottomNav.menu.findItem(R.id.nav_profile)?.isVisible = isStudent
    }
}