package com.kleos.education.ui.onboarding

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class GradientShapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Создаем градиент от лавандового через розовый к желтому
        val gradient = LinearGradient(
            0f, 0f,
            width, height,
            intArrayOf(
                0xFFE8D5FF.toInt(), // Лавандовый (светло-фиолетовый)
                0xFFD4A5FF.toInt(),  // Светло-фиолетовый
                0xFFFFB6C1.toInt(),  // Розовый
                0xFFFFD700.toInt()   // Желтый/золотой
            ),
            floatArrayOf(0f, 0.33f, 0.66f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient

        // Рисуем форму, похожую на цифру 8 или амперсанд с плавными изгибами
        path.reset()
        
        // Начинаем с верхней части формы (левая сторона)
        path.moveTo(width * 0.25f, height * 0.08f)
        
        // Верхняя левая петля
        path.cubicTo(
            width * 0.15f, height * 0.02f,  // контрольная точка 1
            width * 0.05f, height * 0.12f,   // контрольная точка 2
            width * 0.12f, height * 0.25f    // конечная точка
        )
        
        // Переход к центру верхней части
        path.cubicTo(
            width * 0.18f, height * 0.35f,
            width * 0.28f, height * 0.38f,
            width * 0.38f, height * 0.32f
        )
        
        // Верхняя правая часть
        path.cubicTo(
            width * 0.48f, height * 0.26f,
            width * 0.55f, height * 0.18f,
            width * 0.52f, height * 0.08f
        )
        
        // Замыкаем верхнюю часть
        path.cubicTo(
            width * 0.49f, height * 0.02f,
            width * 0.4f, height * 0.05f,
            width * 0.32f, height * 0.12f
        )
        
        // Переход к нижней части (сужение в центре)
        path.cubicTo(
            width * 0.28f, height * 0.42f,
            width * 0.22f, height * 0.48f,
            width * 0.25f, height * 0.55f
        )
        
        // Нижняя левая петля
        path.cubicTo(
            width * 0.28f, height * 0.62f,
            width * 0.35f, height * 0.68f,
            width * 0.42f, height * 0.65f
        )
        
        // Нижняя правая часть
        path.cubicTo(
            width * 0.49f, height * 0.62f,
            width * 0.58f, height * 0.55f,
            width * 0.62f, height * 0.45f
        )
        
        // Замыкаем нижнюю часть обратно к началу
        path.cubicTo(
            width * 0.58f, height * 0.35f,
            width * 0.48f, height * 0.38f,
            width * 0.38f, height * 0.42f
        )
        
        path.close()

        canvas.drawPath(path, paint)
    }
}


