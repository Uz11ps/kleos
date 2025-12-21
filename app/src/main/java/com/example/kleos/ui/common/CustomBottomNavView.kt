package com.example.kleos.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.kleos.R

class CustomBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var selectedPosition: Int = 1 // По умолчанию выбран дом (средняя позиция)
    private var onItemSelectedListener: ((Int) -> Unit)? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        style = Paint.Style.FILL
    }

    private val activeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.black)
        style = Paint.Style.FILL
    }

    private val cornerRadius = 35f * resources.displayMetrics.density

    private val iconViews = mutableListOf<ImageView>()

    init {
        setWillNotDraw(false)
        setBackgroundColor(android.graphics.Color.TRANSPARENT) // Прозрачный фон ViewGroup
        // Убеждаемся, что навигатор отображается поверх всего
        elevation = 16f * resources.displayMetrics.density
        translationZ = 16f * resources.displayMetrics.density
        setupIcons()
        updateIcons() // Обновляем иконки после инициализации, чтобы активная иконка была белой
    }

    private fun setupIcons() {
        // Иконка кисти (левая)
        val brushIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_nav_brush)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        }
        addView(brushIcon)

        // Иконка дома (средняя)
        val homeIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_nav_home_inactive)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        }
        addView(homeIcon)

        // Иконка картинки (правая)
        val galleryIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_nav_gallery)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        }
        addView(galleryIcon)

        iconViews.addAll(listOf(brushIcon, homeIcon, galleryIcon))

        // Устанавливаем обработчики кликов
        brushIcon.setOnClickListener { selectItem(0) }
        homeIcon.setOnClickListener { selectItem(1) }
        galleryIcon.setOnClickListener { selectItem(2) }
    }

    fun selectItem(position: Int) {
        if (position == selectedPosition) return
        selectedPosition = position
        updateIcons()
        onItemSelectedListener?.invoke(position)
        invalidate()
    }

    fun setSelectedItem(position: Int) {
        if (position != selectedPosition) {
            selectedPosition = position
            updateIcons()
            invalidate()
        }
    }

    private fun updateIcons() {
        // Обновляем иконки в зависимости от выбранной позиции
        // Для активной иконки используем белые версии, для неактивных - черные
        
        // Иконка кисти (позиция 0)
        iconViews[0].setImageResource(
            if (selectedPosition == 0) R.drawable.ic_nav_brush_active else R.drawable.ic_nav_brush
        )
        iconViews[0].colorFilter = null
        
        // Иконка дома (позиция 1)
        iconViews[1].setImageResource(
            if (selectedPosition == 1) R.drawable.ic_nav_home_active else R.drawable.ic_nav_home_inactive
        )
        iconViews[1].colorFilter = null
        
        // Иконка картинки (позиция 2)
        iconViews[2].setImageResource(
            if (selectedPosition == 2) R.drawable.ic_nav_gallery_active else R.drawable.ic_nav_gallery
        )
        iconViews[2].colorFilter = null
    }

    fun setOnItemSelectedListener(listener: (Int) -> Unit) {
        onItemSelectedListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Фиксированные размеры: 228dp x 70dp
        val desiredWidth = (228 * resources.displayMetrics.density).toInt()
        val desiredHeight = (70 * resources.displayMetrics.density).toInt()

        // Устанавливаем фиксированные размеры
        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(desiredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY)
        )

        // Измеряем иконки
        val iconSize = (26 * resources.displayMetrics.density).toInt()
        val iconMeasureSpec = MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY)

        iconViews.forEach { icon ->
            icon.measure(iconMeasureSpec, iconMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        // Размещаем иконки равномерно по горизонтали
        val iconSize = iconViews[0].measuredWidth
        val totalIconsWidth = iconSize * iconViews.size
        val spacing = (width - totalIconsWidth) / (iconViews.size + 1)

        var currentX = spacing.toFloat()
        val centerY = height / 2f

        iconViews.forEach { icon ->
            val left = currentX.toInt()
            val top = (centerY - iconSize / 2f).toInt()
            val right = left + iconSize
            val bottom = top + iconSize
            icon.layout(left, top, right, bottom)
            currentX += iconSize + spacing
        }
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // Рисуем белый закругленный фон
        val rect = RectF(0f, 0f, width, height)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        // Рисуем черный круг для активной иконки (64x64dp)
        if (iconViews.isNotEmpty()) {
            val iconSize = iconViews[0].measuredWidth.toFloat()
            val totalIconsWidth = iconSize * iconViews.size
            val spacing = (width - totalIconsWidth) / (iconViews.size + 1)
            val activeIconX = spacing + iconSize / 2f + selectedPosition * (iconSize + spacing)
            val activeIconY = height / 2f
            val circleRadius = 32f * resources.displayMetrics.density // Радиус 32dp для круга 64x64dp

            canvas.drawCircle(activeIconX, activeIconY, circleRadius, activeCirclePaint)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Сначала рисуем фон и активный круг
        onDraw(canvas)
        // Затем рисуем дочерние элементы (иконки) поверх
        super.dispatchDraw(canvas)
    }
}

