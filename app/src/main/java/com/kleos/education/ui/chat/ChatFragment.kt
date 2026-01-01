package com.kleos.education.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kleos.education.databinding.FragmentChatBinding
import com.kleos.education.data.auth.SessionManager
import android.content.Intent
import com.kleos.education.ui.auth.AuthActivity

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var faqAdapter: FaqAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)

        // Скрываем bottom navigation на этой странице
        hideBottomNavigation()

        // Обработка кнопки назад
        binding.backButton.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Обработка кнопки меню
        binding.menuButton.setOnClickListener {
            (activity as? com.kleos.education.MainActivity)?.let { mainActivity ->
                mainActivity.openDrawer()
            }
        }

        // Применяем локализацию для кнопки CTA
        binding.ctaButton.text = com.kleos.education.ui.language.TranslationManager.getOverride(
            requireContext(),
            com.kleos.education.R.string.leave_request
        ) ?: requireContext().getString(com.kleos.education.R.string.leave_request)
        
        // Обработка кнопки CTA
        binding.ctaButton.setOnClickListener {
            val session = SessionManager(requireContext())
            if (!session.isLoggedIn()) {
                com.kleos.education.ui.utils.AnimationUtils.shake(binding.ctaButton)
                startActivity(Intent(requireContext(), AuthActivity::class.java))
            } else {
                // Плавный переход от FAQ к чату
                animateFaqToChatTransition()
            }
        }

        setupFaqRecycler()
        // Показать блок FAQ, скрыть чат до выбора
        binding.faqRecycler.visibility = View.VISIBLE
        binding.messagesList.visibility = View.GONE
        binding.messageInputBar.visibility = View.GONE
        // Показать заголовок для FAQ
        binding.titleText.visibility = View.VISIBLE
        binding.subtitleText.visibility = View.VISIBLE
        
        // Крутые анимации появления FAQ списка
        animateFaqRecycler()

        val adapter = ChatMessageAdapter()
        binding.messagesList.adapter = adapter

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            val previousCount = adapter.count
            adapter.updateMessages(msgs)
            
            // Анимация появления новых сообщений
            if (msgs.size > previousCount && binding.messagesList.visibility == View.VISIBLE) {
                animateNewMessage()
            }
            
            binding.messagesList.post { 
                binding.messagesList.setSelection(adapter.count - 1)
            }
        }

        // Кнопка отправки без анимаций при нажатии
        
        // Анимация для поля ввода при фокусе
        binding.messageEditText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .setDuration(200)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            } else {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
        }
        
        binding.sendButton.setOnClickListener {
            val text = binding.messageEditText.text?.toString().orEmpty()
            val session = SessionManager(requireContext())
            if (!session.isLoggedIn()) {
                com.kleos.education.ui.utils.AnimationUtils.shake(binding.sendButton)
                com.kleos.education.ui.utils.AnimationUtils.shake(binding.messageInputBar)
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                return@setOnClickListener
            }
            if (text.isBlank()) {
                // Анимация при попытке отправить пустое сообщение
                com.kleos.education.ui.utils.AnimationUtils.shake(binding.messageEditText)
                return@setOnClickListener
            }
            // Отправка сообщения без анимаций кнопки
            viewModel.sendUserMessage(text)
            binding.messageEditText.setText("")
        }
    }
    
    private fun animateFaqRecycler() {
        // Анимация появления RecyclerView
        if (!isAdded || _binding == null) return
        
        if (binding.faqRecycler.visibility == View.VISIBLE) {
            binding.faqRecycler.alpha = 0f
            binding.faqRecycler.translationY = 30f
            binding.faqRecycler.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    if (isAdded && _binding != null) {
                        // Запускаем анимацию элементов после появления RecyclerView
                        binding.faqRecycler.postDelayed({
                            if (isAdded && _binding != null) {
                                animateFaqCards()
                            }
                        }, 200)
                    }
                }
                .start()
        }
    }
    
    private fun animateFaqCards() {
        if (!isAdded || _binding == null) return
        
        val layoutManager = binding.faqRecycler.layoutManager
        if (layoutManager != null && binding.faqRecycler.visibility == View.VISIBLE) {
            val childCount = layoutManager.childCount
            for (i in 0 until childCount) {
                val child = layoutManager.getChildAt(i)
                child?.let {
                    // Проверяем, что view еще не анимирована
                    val tag = it.tag as? String
                    if (tag != "animated") {
                        it.tag = "animated"
                        it.alpha = 0f
                        it.translationY = 20f
                        
                        val delay = i * 50L
                        it.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .setStartDelay(delay)
                            .setInterpolator(android.view.animation.DecelerateInterpolator())
                            .start()
                    }
                }
            }
        }
    }
    
    
    private fun animateNewMessage() {
        // Анимация появления нового сообщения в списке с эффектом "всплытия"
        binding.messagesList.alpha = 0.7f
        binding.messagesList.scaleX = 0.98f
        binding.messagesList.translationY = 10f
        binding.messagesList.animate()
            .alpha(1f)
            .scaleX(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
            .start()
        
        // Кнопка отправки остается статичной без анимаций
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Восстанавливаем цвет статус-бара
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.dark_background, null)
        // Показываем bottom navigation обратно при выходе со страницы
        showBottomNavigation()
        _binding = null
    }

    private fun hideBottomNavigation() {
        activity?.findViewById<com.kleos.education.ui.common.CustomBottomNavView>(com.kleos.education.R.id.bottom_nav)?.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        activity?.findViewById<com.kleos.education.ui.common.CustomBottomNavView>(com.kleos.education.R.id.bottom_nav)?.visibility = View.VISIBLE
    }

    private fun setupFaqRecycler() {
        val rv: RecyclerView = binding.faqRecycler
        rv.layoutManager = LinearLayoutManager(requireContext())
        faqAdapter = FaqAdapter(loadFaqItems()) { item, cardView ->
            // Плавная анимация расширения карточки в центр экрана
            if (isAdded && _binding != null) {
                com.kleos.education.ui.utils.FaqExpandAnimation.expandFaqCard(
                    this@ChatFragment,
                    cardView,
                    item.question,
                    item.answer
                ) {
                    // Callback после закрытия
                }
            }
        }
        rv.adapter = faqAdapter
        rv.setHasFixedSize(true)
        
        // Анимации при скролле RecyclerView
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Параллакс эффект при скролле
                val layoutManager = recyclerView.layoutManager
                if (layoutManager != null) {
                    for (i in 0 until layoutManager.childCount) {
                        val child = layoutManager.getChildAt(i)
                        child?.let {
                            val position = recyclerView.getChildAdapterPosition(it)
                            val factor = (position % 3) * 0.1f
                            com.kleos.education.ui.utils.AnimationUtils.parallaxScroll(it, dy, factor)
                        }
                    }
                }
            }
        })
        
        // Анимация при первом появлении на экране будет вызвана из animateFaqRecycler()
    }

    override fun onResume() {
        super.onResume()
        // Устанавливаем цвет статус-бара для однородного фона
        activity?.window?.statusBarColor = resources.getColor(com.kleos.education.R.color.onboarding_background, null)
        // Скрываем bottom navigation при возврате на страницу
        hideBottomNavigation()
        
        if (binding.messagesList.visibility == View.VISIBLE) {
            viewModel.startPolling()
            // Анимация появления чата при возврате на экран
            binding.messagesList.alpha = 0.8f
            binding.messagesList.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        } else if (binding.faqRecycler.visibility == View.VISIBLE) {
            // Переанимация FAQ элементов при возврате (только если видимы)
            binding.faqRecycler.postDelayed({
                animateFaqCards()
            }, 100)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }

    private fun animateFaqToChatTransition() {
        // Анимация скрытия FAQ и кнопки CTA
        binding.ctaButton.animate()
            .alpha(0f)
            .translationY(-20f)
            .setDuration(300)
            .start()
        
        // Скрываем заголовок и подзаголовок
        binding.titleText.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.titleText.visibility = View.GONE
            }
            .start()
        
        binding.subtitleText.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.subtitleText.visibility = View.GONE
            }
            .start()
        
        binding.faqRecycler.animate()
            .alpha(0f)
            .translationY(20f)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                binding.faqRecycler.visibility = View.GONE
                binding.ctaButton.visibility = View.GONE
                
                // Добавляем начальное сообщение "Что вас интересует?"
                val initialMessage = com.kleos.education.data.model.Message(
                    id = "initial",
                    sender = "support",
                    text = requireContext().getString(com.kleos.education.R.string.what_interests_you),
                    timestampMillis = System.currentTimeMillis()
                )
                val currentMessages = viewModel.messages.value ?: emptyList()
                if (currentMessages.isEmpty() || currentMessages.none { it.id == "initial" }) {
                    viewModel.addInitialMessage(initialMessage)
                }
                
                // Анимация появления чата
                binding.messagesList.alpha = 0f
                binding.messagesList.translationX = 100f
                binding.messagesList.visibility = View.VISIBLE
                binding.messagesList.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(500)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
                
                // Анимация появления поля ввода снизу
                binding.messageInputBar.alpha = 0f
                binding.messageInputBar.translationY = 100f
                binding.messageInputBar.visibility = View.VISIBLE
                binding.messageInputBar.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
                
                viewModel.refresh()
                viewModel.startPolling()
            }
            .start()
    }

    private fun loadFaqItems(): List<FaqItem> {
        val questions = resources.getStringArray(com.kleos.education.R.array.faq_questions)
        val answers = resources.getStringArray(com.kleos.education.R.array.faq_answers)
        val size = minOf(questions.size, answers.size)
        val list = ArrayList<FaqItem>(size)
        for (i in 0 until size) {
            val id = "faq_%03d".format(i + 1)
            list.add(FaqItem(id, "", questions[i], answers[i]))
        }
        return list
    }
}



