package com.example.kleos.ui.onboarding

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.util.AttributeSet
import android.view.View

class QuarterDashedRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.WHITE
        strokeWidth = 4f * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width.coerceAtMost(height) / 2f) - paint.strokeWidth / 2f

        // Создаем пунктирный эффект
        val dashPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(8f * resources.displayMetrics.density, 0f)
        }
        val dashPathEffect = PathDashPathEffect(
            dashPath,
            12f * resources.displayMetrics.density, // интервал между пунктирами
            0f, // фаза
            PathDashPathEffect.Style.ROTATE
        )
        paint.pathEffect = dashPathEffect

        // Рисуем только четверть окружности (верхняя правая часть)
        val path = Path()
        val startAngle = 0f // Начинаем сверху
        val sweepAngle = 90f // Рисуем четверть окружности (90 градусов)
        
        path.addArc(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            startAngle,
            sweepAngle
        )

        canvas.drawPath(path, paint)
    }
}

