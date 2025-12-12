package com.example.kleos.ui.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.example.kleos.databinding.DialogFaqDetailBinding

object FaqExpandAnimation {
    
    /**
     * Анимация расширения карточки FAQ в центр экрана с ответом
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
        
        // Получаем координаты карточки на экране
        val location = IntArray(2)
        cardView.getLocationOnScreen(location)
        
        // Получаем координаты rootView для правильного расчета
        val rootLocation = IntArray(2)
        rootView.getLocationOnScreen(rootLocation)
        
        val startX = (location[0] - rootLocation[0]).toFloat()
        val startY = (location[1] - rootLocation[1]).toFloat()
        val startWidth = cardView.width.toFloat()
        val startHeight = cardView.height.toFloat()
        
        // Размеры экрана
        val screenWidth = rootView.width.toFloat()
        val screenHeight = rootView.height.toFloat()
        
        // Целевые размеры (90% ширины, 70% высоты экрана)
        val targetWidth = screenWidth * 0.9f
        val targetHeight = (screenHeight * 0.7f).coerceAtMost(600f) // Максимум 600dp высоты
        val targetX = (screenWidth - targetWidth) / 2f
        val targetY = (screenHeight - targetHeight) / 2f
        
        // Создаем overlay для затемнения фона
        val overlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0x80000000.toInt()) // Полупрозрачный черный
            alpha = 0f
        }
        
        // Создаем контейнер для расширенной карточки
        val expandedCard = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                startWidth.toInt(),
                startHeight.toInt()
            )
            x = startX
            y = startY
            elevation = 16f
        }
        
        // Загружаем layout с ответом
        val binding = DialogFaqDetailBinding.inflate(activity.layoutInflater)
        binding.questionText.text = question
        binding.answerText.text = answer
        
        // Обработчик закрытия через кнопку
        binding.closeButton.setOnClickListener {
            collapseFaqCard(overlay, expandedCard, cardView, startX, startY, startWidth, startHeight) {
                onDismiss()
            }
        }
        
        expandedCard.addView(binding.root)
        rootView.addView(overlay)
        rootView.addView(expandedCard)
        
        // Анимация затемнения фона
        overlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
        
        // Анимация расширения карточки
        val widthAnimator = ValueAnimator.ofFloat(startWidth, targetWidth)
        val heightAnimator = ValueAnimator.ofFloat(startHeight, targetHeight)
        val xAnimator = ValueAnimator.ofFloat(startX, targetX)
        val yAnimator = ValueAnimator.ofFloat(startY, targetY)
        
        val duration = 500L
        
        widthAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            expandedCard.layoutParams.width = value.toInt()
            expandedCard.requestLayout()
        }
        
        heightAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            expandedCard.layoutParams.height = value.toInt()
            expandedCard.requestLayout()
        }
        
        xAnimator.addUpdateListener { animation ->
            expandedCard.x = animation.animatedValue as Float
        }
        
        yAnimator.addUpdateListener { animation ->
            expandedCard.y = animation.animatedValue as Float
        }
        
        val interpolator = DecelerateInterpolator()
        widthAnimator.interpolator = interpolator
        heightAnimator.interpolator = interpolator
        xAnimator.interpolator = interpolator
        yAnimator.interpolator = interpolator
        
        widthAnimator.duration = duration
        heightAnimator.duration = duration
        xAnimator.duration = duration
        yAnimator.duration = duration
        
        // Запускаем все анимации одновременно
        widthAnimator.start()
        heightAnimator.start()
        xAnimator.start()
        yAnimator.start()
        
        // Анимация появления содержимого
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setStartDelay(300)
            .setDuration(300)
            .start()
        
        // Обработчик закрытия
        overlay.setOnClickListener {
            collapseFaqCard(overlay, expandedCard, cardView, startX, startY, startWidth, startHeight) {
                onDismiss()
            }
        }
        
    }
    
    private fun collapseFaqCard(
        overlay: View,
        expandedCard: View,
        originalCard: View,
        startX: Float,
        startY: Float,
        startWidth: Float,
        startHeight: Float,
        onEnd: () -> Unit
    ) {
        val endWidth = expandedCard.width.toFloat()
        val endHeight = expandedCard.height.toFloat()
        val endX = expandedCard.x
        val endY = expandedCard.y
        
        // Анимация скрытия содержимого
        val content = (expandedCard as ViewGroup).getChildAt(0)
        content?.animate()?.apply {
            alpha(0f)
            duration = 200
            start()
        }
        
        // Анимация затемнения фона
        overlay.animate()
            .alpha(0f)
            .setDuration(300)
            .start()
        
        // Анимация сжатия карточки обратно
        val widthAnimator = ValueAnimator.ofFloat(endWidth, startWidth)
        val heightAnimator = ValueAnimator.ofFloat(endHeight, startHeight)
        val xAnimator = ValueAnimator.ofFloat(endX, startX)
        val yAnimator = ValueAnimator.ofFloat(endY, startY)
        
        val duration = 400L
        
        widthAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            expandedCard.layoutParams.width = value.toInt()
            expandedCard.requestLayout()
        }
        
        heightAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            expandedCard.layoutParams.height = value.toInt()
            expandedCard.requestLayout()
        }
        
        xAnimator.addUpdateListener { animation ->
            expandedCard.x = animation.animatedValue as Float
        }
        
        yAnimator.addUpdateListener { animation ->
            expandedCard.y = animation.animatedValue as Float
        }
        
        val interpolator = DecelerateInterpolator()
        widthAnimator.interpolator = interpolator
        heightAnimator.interpolator = interpolator
        xAnimator.interpolator = interpolator
        yAnimator.interpolator = interpolator
        
        widthAnimator.duration = duration
        heightAnimator.duration = duration
        xAnimator.duration = duration
        yAnimator.duration = duration
        
        val animatorSet = android.animation.AnimatorSet()
        animatorSet.playTogether(widthAnimator, heightAnimator, xAnimator, yAnimator)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val parent = overlay.parent as? ViewGroup
                parent?.removeView(overlay)
                parent?.removeView(expandedCard)
                onEnd()
            }
        })
        animatorSet.start()
    }
}

