package com.kleos.education.ui.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.kleos.education.databinding.DialogFaqDetailBinding

object FaqExpandAnimation {
    
    // Сохраняем ссылку на корневой View фрагмента для размытия
    private var fragmentRootView: View? = null
    
    /**
     * Показывает модальное окно с ответом FAQ с затемнением и размытием фона
     */
    fun expandFaqCard(
        fragment: Fragment,
        cardView: View,
        question: String,
        answer: String,
        onDismiss: () -> Unit
    ) {
        val activity = fragment.activity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        
        // Сохраняем корневой View фрагмента для размытия
        fragmentRootView = fragment.view
        
        // Размеры экрана
        val screenWidth = rootView.width.toFloat()
        val screenHeight = rootView.height.toFloat()
        
        // Создаем контейнер для диалога
        val dialogCard = FrameLayout(activity)
        val dialogParams = FrameLayout.LayoutParams(
            (screenWidth * 0.9f).toInt(),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            val margin = (screenWidth * 0.05f).toInt()
            setMargins(margin, 0, margin, 0)
        }
        dialogCard.layoutParams = dialogParams
        dialogCard.elevation = 24f
        dialogCard.alpha = 0f
        dialogCard.scaleX = 0.9f
        dialogCard.scaleY = 0.9f
        
        // Создаем overlay для затемнения и размытия фона
        val overlay = FrameLayout(activity)
        val overlayParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        overlay.layoutParams = overlayParams
        overlay.setBackgroundColor(0xCC000000.toInt()) // Полупрозрачный черный с большей непрозрачностью
        overlay.alpha = 0f
        overlay.setOnClickListener {
            dismissDialog(overlay, dialogCard, onDismiss)
        }
        
        // Загружаем layout с ответом
        val binding = DialogFaqDetailBinding.inflate(activity.layoutInflater)
        binding.questionText.text = question
        binding.answerText.text = answer
        
        // Обработчик закрытия через кнопку
        binding.closeButton.setOnClickListener {
            dismissDialog(overlay, dialogCard, onDismiss)
        }
        
        dialogCard.addView(binding.root)
        rootView.addView(overlay)
        rootView.addView(dialogCard)
        
        // Применяем размытие к фону фрагмента
        applyBlurToFragment(fragmentRootView)
        
        // Центрируем диалог вертикально после измерения
        dialogCard.post {
            val dialogHeight = dialogCard.height
            val topMargin = ((screenHeight - dialogHeight) / 2f).toInt().coerceAtLeast(0)
            val params = dialogCard.layoutParams as FrameLayout.LayoutParams
            params.topMargin = topMargin
            dialogCard.layoutParams = params
            dialogCard.requestLayout()
        }
        
        // Анимация появления overlay (затемнение фона)
        overlay.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
        
        // Анимация появления диалога
        dialogCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Применяет размытие к корневому View фрагмента
     */
    private fun applyBlurToFragment(view: View?) {
        if (view == null) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Для Android 12+ используем RenderEffect
            val blurRadius = 20f * view.resources.displayMetrics.density
            view.setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    android.graphics.Shader.TileMode.CLAMP
                )
            )
        }
        // Для старых версий Android просто затемнение (размытие через RenderEffect недоступно)
    }
    
    /**
     * Убирает размытие с корневого View фрагмента
     */
    private fun removeBlurFromFragment(view: View?) {
        if (view == null) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null)
        }
    }
    
    private fun dismissDialog(
        overlay: View,
        dialogCard: View,
        onEnd: () -> Unit
    ) {
        // Убираем размытие с фона фрагмента
        removeBlurFromFragment(fragmentRootView)
        
        // Анимация скрытия диалога
        dialogCard.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()
        
        // Анимация скрытия overlay
        overlay.animate()
            .alpha(0f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val parent = overlay.parent as? ViewGroup
                    parent?.removeView(overlay)
                    parent?.removeView(dialogCard)
                    fragmentRootView = null
                    onEnd()
                }
            })
            .start()
    }
}


