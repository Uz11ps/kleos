package com.kleos.education.ui.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

object AnimationUtils {
    
    /**
     * Плавное появление элемента с увеличением масштаба
     */
    fun fadeInScale(view: View, duration: Long = 400) {
        view.alpha = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Появление с отскоком
     */
    fun bounceIn(view: View, duration: Long = 600) {
        view.alpha = 0f
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(BounceInterpolator())
            .start()
    }
    
    /**
     * Скольжение снизу с затуханием
     */
    fun slideUpFade(view: View, duration: Long = 500, delay: Long = 0) {
        view.alpha = 0f
        view.translationY = 100f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Скольжение справа
     */
    fun slideInFromRight(view: View, duration: Long = 400) {
        view.alpha = 0f
        view.translationX = view.width.toFloat()
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Скольжение слева
     */
    fun slideInFromLeft(view: View, duration: Long = 400) {
        view.alpha = 0f
        view.translationX = -view.width.toFloat()
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Вращение с появлением
     */
    fun rotateIn(view: View, duration: Long = 500) {
        view.alpha = 0f
        view.rotation = -180f
        view.animate()
            .alpha(1f)
            .rotation(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Эффект нажатия кнопки
     */
    fun pressButton(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    /**
     * Эффект отпускания кнопки
     */
    fun releaseButton(view: View) {
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(150)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }
    
    /**
     * Пульсация элемента
     */
    fun pulse(view: View, duration: Long = 1000) {
        val animator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        animator.duration = duration
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        
        val animatorY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)
        animatorY.duration = duration
        animatorY.repeatCount = ValueAnimator.INFINITE
        animatorY.repeatMode = ValueAnimator.REVERSE
        animatorY.interpolator = AccelerateDecelerateInterpolator()
        animatorY.start()
    }
    
    /**
     * Покачивание элемента
     */
    fun shake(view: View, duration: Long = 500) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = duration
        animator.start()
    }
    
    /**
     * Анимация появления карточки
     */
    fun cardEnter(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        view.translationY = 50f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Последовательное появление элементов списка
     */
    fun staggerRecyclerViewItems(recyclerView: RecyclerView, delayBetweenItems: Long = 100) {
        val adapter = recyclerView.adapter ?: return
        val layoutManager = recyclerView.layoutManager ?: return
        
        for (i in 0 until adapter.itemCount) {
            val view = layoutManager.findViewByPosition(i) ?: continue
            cardEnter(view, i * delayBetweenItems)
        }
    }
    
    /**
     * Плавное скрытие с уменьшением
     */
    fun fadeOutScale(view: View, duration: Long = 300, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Эффект "волны" для группы элементов
     */
    fun waveAnimation(views: List<View>, startDelay: Long = 0, delayBetween: Long = 50) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.scaleX = 0.5f
            view.scaleY = 0.5f
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(startDelay + index * delayBetween)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }
    }
    
    /**
     * Плавное изменение высоты
     */
    fun expandView(view: View, targetHeight: Int, duration: Long = 300) {
        val startHeight = view.height
        val animator = ValueAnimator.ofInt(startHeight, targetHeight)
        animator.duration = duration
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams = view.layoutParams
            layoutParams.height = value
            view.layoutParams = layoutParams
        }
        animator.start()
    }
    
    /**
     * Сворачивание вида
     */
    fun collapseView(view: View, duration: Long = 300, onEnd: (() -> Unit)? = null) {
        val startHeight = view.height
        val animator = ValueAnimator.ofInt(startHeight, 0)
        animator.duration = duration
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val layoutParams = view.layoutParams
            layoutParams.height = value
            view.layoutParams = layoutParams
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
        })
        animator.start()
    }
    
    /**
     * Параллакс эффект при скролле
     */
    fun parallaxScroll(view: View, scrollY: Int, factor: Float = 0.5f) {
        view.translationY = scrollY * factor
    }
}


