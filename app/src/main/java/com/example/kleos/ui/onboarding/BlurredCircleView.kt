package com.example.kleos.ui.onboarding

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.view.View

class BlurredCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Цвет #7E5074 без прозрачности (полная непрозрачность)
        color = 0xFF7E5074.toInt()
    }

    init {
        // Применяем линейный блюр 280px для Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            post {
                applyBlur()
            }
        }
    }
    
    private fun applyBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && width > 0 && height > 0) {
            val blurRadius = 280f * resources.displayMetrics.density
            setRenderEffect(android.graphics.RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius,
                android.graphics.Shader.TileMode.CLAMP
            ))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width.coerceAtMost(height)) / 2f

        // Рисуем простой круг с цветом #7E5074
        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Применяем размытие после изменения размера
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            post {
                applyBlur()
            }
        }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Применяем размытие при присоединении к окну
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            post {
                applyBlur()
            }
        }
    }
}

