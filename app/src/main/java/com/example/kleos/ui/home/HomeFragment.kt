package com.example.kleos.ui.home

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.kleos.data.model.NewsItem
import com.example.kleos.databinding.FragmentHomeBinding
import com.example.kleos.databinding.DialogInviteBinding
import com.example.kleos.data.auth.SessionManager
import com.example.kleos.ui.language.t
import androidx.lifecycle.lifecycleScope
import com.example.kleos.data.news.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Анимация появления заголовка
        com.example.kleos.ui.utils.AnimationUtils.slideUpFade(binding.helloText, 500, 0)
        
        // Анимация появления карточки пользователя
        com.example.kleos.ui.utils.AnimationUtils.bounceIn(binding.userCard, 600)
        
        val adapter = NewsAdapter(emptyList()) { item ->
            // Показываем диалог с деталями новости
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.title)
                .setMessage("${item.dateText}\n\n${item.content ?: "Нет дополнительной информации"}")
                .setPositiveButton("OK", null)
                .show()
        }
        binding.newsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecycler.adapter = adapter

        // Загрузка новостей из API
        val repo = NewsRepository()
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                runCatching { repo.fetch() }.getOrElse { emptyList() }
            }
            adapter.submitList(items)
        }

        // Greeting and user card binding
        val session = SessionManager(requireContext())
        val user = session.getCurrentUser()
        val name = (user?.fullName?.takeIf { it.isNotBlank() } ?: getString(com.example.kleos.R.string.guest)).trim()
        binding.helloText.text = getString(com.example.kleos.R.string.hello_name, name)
        val idNumeric = user?.id
            ?.filter { it.isDigit() }
            ?.padStart(6, '0')
            ?.takeLast(6) ?: "000000"
        val idTemplate = requireContext().t(com.example.kleos.R.string.id_prefix)
        binding.userIdText.text = String.format(idTemplate, idNumeric)
        binding.userNameText.text = name.ifBlank { requireContext().t(com.example.kleos.R.string.guest) }
    }

    override fun onResume() {
        super.onResume()
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val shown = prefs.getBoolean("invite_dialog_shown", false)
        if (!shown && isAdded) {
            val dialogBinding = DialogInviteBinding.inflate(layoutInflater)
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()

            dialogBinding.closeButton.setOnClickListener {
                dialog.dismiss()
                prefs.edit().putBoolean("invite_dialog_shown", true).apply()
            }
            dialogBinding.inviteButton.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getString(com.example.kleos.R.string.invite_share_text))
                }
                startActivity(Intent.createChooser(shareIntent, getString(com.example.kleos.R.string.invite_share_title)))
                dialog.dismiss()
                prefs.edit().putBoolean("invite_dialog_shown", true).apply()
            }
            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}