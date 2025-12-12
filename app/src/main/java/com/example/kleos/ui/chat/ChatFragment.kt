package com.example.kleos.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.kleos.databinding.FragmentChatBinding
import com.example.kleos.data.auth.SessionManager
import android.content.Intent
import com.example.kleos.ui.auth.AuthActivity

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

        setupFaqRecycler()
        // Показать блок FAQ, скрыть чат до выбора
        binding.faqRecycler.visibility = View.VISIBLE
        binding.messagesList.visibility = View.GONE
        binding.messageInputBar.visibility = View.GONE
        
        // Крутые анимации появления FAQ списка
        animateFaqRecycler()

        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        binding.messagesList.adapter = adapter

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            val previousCount = adapter.count
            adapter.clear()
            adapter.addAll(msgs.map { m ->
                val prefix = if (m.sender == "user") "Вы: " else "Поддержка: "
                "$prefix${m.text}"
            })
            adapter.notifyDataSetChanged()
            
            // Анимация появления новых сообщений
            if (msgs.size > previousCount && binding.messagesList.visibility == View.VISIBLE) {
                animateNewMessage()
            }
            
            binding.messagesList.post { 
                binding.messagesList.setSelection(adapter.count - 1)
            }
        }

        // Анимация для кнопки отправки
        binding.sendButton.setOnTouchListener { view, motionEvent ->
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
                com.example.kleos.ui.utils.AnimationUtils.shake(binding.sendButton)
                com.example.kleos.ui.utils.AnimationUtils.shake(binding.messageInputBar)
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                return@setOnClickListener
            }
            if (text.isBlank()) {
                // Анимация при попытке отправить пустое сообщение
                com.example.kleos.ui.utils.AnimationUtils.shake(binding.messageEditText)
                return@setOnClickListener
            }
            // Анимация отправки сообщения
            com.example.kleos.ui.utils.AnimationUtils.pulse(binding.sendButton, 200)
            viewModel.sendUserMessage(text)
            binding.messageEditText.setText("")
            
            // Анимация появления нового сообщения
            animateMessageSent()
        }
    }
    
    private fun animateFaqRecycler() {
        // Анимация появления RecyclerView с эффектом волны
        if (!isAdded || _binding == null) return
        
        if (binding.faqRecycler.visibility == View.VISIBLE) {
            binding.faqRecycler.alpha = 0f
            binding.faqRecycler.translationY = 50f
            binding.faqRecycler.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    if (isAdded && _binding != null) {
                        // Запускаем анимацию карточек после появления RecyclerView
                        binding.faqRecycler.postDelayed({
                            if (isAdded && _binding != null) {
                                animateFaqCards()
                            }
                        }, 300)
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
                        it.scaleX = 0.5f
                        it.scaleY = 0.5f
                        it.rotation = if (i % 2 == 0) -10f else 10f
                        it.translationY = 30f
                        
                        val delay = i * 80L
                        it.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .rotation(0f)
                            .translationY(0f)
                            .setDuration(500)
                            .setStartDelay(delay)
                            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                            .withEndAction {
                                if (!isAdded || _binding == null) return@withEndAction
                                // Дополнительный эффект "подпрыгивания" после появления
                                if (i % 3 == 0) {
                                    it.animate()
                                        .translationY(-5f)
                                        .setDuration(150)
                                        .withEndAction {
                                            if (isAdded && _binding != null) {
                                                it.animate()
                                                    .translationY(0f)
                                                    .setDuration(150)
                                                    .setInterpolator(android.view.animation.BounceInterpolator())
                                                    .start()
                                            }
                                        }
                                        .start()
                                }
                            }
                            .start()
                    }
                }
            }
        }
    }
    
    private fun animateMessageSent() {
        // Анимация поля ввода при отправке
        binding.messageEditText.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.messageEditText.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator())
                    .start()
            }
            .start()
        
        // Анимация кнопки отправки
        binding.sendButton.animate()
            .rotation(360f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                binding.sendButton.rotation = 0f
            }
            .start()
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
        
        // Анимация кнопки отправки при получении ответа
        binding.sendButton.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(150)
            .withEndAction {
                binding.sendButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFaqRecycler() {
        val rv: RecyclerView = binding.faqRecycler
        val grid = GridLayoutManager(requireContext(), 2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // Первая карточка (CTA) занимает две колонки, остальные — одну
                return if (position == 0) 2 else 1
            }
        }
        rv.layoutManager = grid
        faqAdapter = FaqAdapter(loadFaqItems(), { item, cardView ->
            // Плавная анимация расширения карточки в центр экрана
            if (isAdded && _binding != null) {
                com.example.kleos.ui.utils.FaqExpandAnimation.expandFaqCard(
                    this@ChatFragment,
                    cardView,
                    item.question,
                    item.answer
                ) {
                    // Callback после закрытия
                }
            }
        }) {
            // CTA: скрыть FAQ и показать чат с крутыми анимациями
            val session = SessionManager(requireContext())
            if (!session.isLoggedIn()) {
                // Анимация при попытке войти без авторизации
                com.example.kleos.ui.utils.AnimationUtils.shake(binding.faqRecycler)
                startActivity(Intent(requireContext(), AuthActivity::class.java))
            } else {
                // Плавный переход от FAQ к чату
                animateFaqToChatTransition()
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
                            com.example.kleos.ui.utils.AnimationUtils.parallaxScroll(it, dy, factor)
                        }
                    }
                }
            }
        })
        
        // Анимация при первом появлении на экране будет вызвана из animateFaqRecycler()
    }

    override fun onResume() {
        super.onResume()
        if (binding.messagesList.visibility == View.VISIBLE) {
            viewModel.startPolling()
            // Анимация появления чата при возврате на экран
            binding.messagesList.alpha = 0.8f
            binding.messagesList.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        } else if (binding.faqRecycler.visibility == View.VISIBLE) {
            // Переанимация FAQ карточек при возврате (только если видимы)
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
        // Анимация скрытия FAQ с эффектом масштабирования и вращения
        binding.faqRecycler.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .rotation(5f)
            .setDuration(400)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                binding.faqRecycler.visibility = View.GONE
                
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
        val questions = resources.getStringArray(com.example.kleos.R.array.faq_questions)
        val answers = resources.getStringArray(com.example.kleos.R.array.faq_answers)
        val size = minOf(questions.size, answers.size)
        val list = ArrayList<FaqItem>(size)
        for (i in 0 until size) {
            val id = "faq_%03d".format(i + 1)
            list.add(FaqItem(id, "", questions[i], answers[i]))
        }
        return list
    }
}


