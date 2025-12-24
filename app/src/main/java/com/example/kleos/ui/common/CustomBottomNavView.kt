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
    private var isGuestMode: Boolean = false // Режим гостя - скрываем первую иконку

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
        // Иконка университета (левая)
        val universityIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_university_filter)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        }
        addView(universityIcon)

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

        iconViews.addAll(listOf(universityIcon, homeIcon, galleryIcon))

        // Устанавливаем обработчики кликов
        universityIcon.setOnClickListener { selectItem(0) }
        homeIcon.setOnClickListener { selectItem(1) }
        galleryIcon.setOnClickListener { selectItem(2) }
    }

    fun selectItem(position: Int) {
        // Для гостя позиции смещены: 0 = дом, 1 = галерея
        val actualPosition = if (isGuestMode) {
            when (position) {
                0 -> 1 // Дом
                1 -> 2 // Галерея
                else -> position
            }
        } else {
            position
        }
        
        if (actualPosition == selectedPosition) return
        selectedPosition = actualPosition
        updateIcons()
        // Передаем оригинальную позицию в listener для правильной навигации
        onItemSelectedListener?.invoke(position)
        invalidate()
    }

    fun setSelectedItem(position: Int) {
        // position - это логическая позиция (0, 1, 2 для обычных пользователей)
        // Для гостя: position 0 = дом (реальная позиция 1), position 1 = галерея (реальная позиция 2)
        val actualPosition = if (isGuestMode) {
            when (position) {
                0 -> 1 // Дом
                1 -> 2 // Галерея
                else -> position
            }
        } else {
            position
        }
        
        if (actualPosition != selectedPosition) {
            selectedPosition = actualPosition
            updateIcons()
            invalidate()
        }
    }

    private fun updateIcons() {
        // Обновляем иконки в зависимости от выбранной позиции
        // Для активной иконки используем белые версии, для неактивных - черные
        
        // Иконка университета (позиция 0) - всегда черная
        iconViews[0].setImageResource(R.drawable.ic_university_filter)
        // Для активной иконки делаем белой через colorFilter, для неактивной оставляем черной
        iconViews[0].colorFilter = if (selectedPosition == 0) {
            // Активная иконка - белая (инвертируем черный в белый)
            android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,  // R: инвертируем и добавляем белый
                    0f, -1f, 0f, 0f, 255f,  // G: инвертируем и добавляем белый
                    0f, 0f, -1f, 0f, 255f,  // B: инвертируем и добавляем белый
                    0f, 0f, 0f, 1f, 0f      // A: прозрачность без изменений
                ))
            )
        } else {
            // Неактивная иконка - черная (без фильтра, так как drawable уже черный)
            null
        }
        
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
    
    fun setGuestMode(isGuest: Boolean) {
        if (isGuestMode != isGuest) {
            isGuestMode = isGuest
            // Обновляем видимость первой иконки
            if (iconViews.isNotEmpty()) {
                iconViews[0].visibility = if (isGuest) android.view.View.GONE else android.view.View.VISIBLE
            }
            // Если гость и выбрана позиция 0 (Допуск), переключаемся на дом
            if (isGuest && selectedPosition == 0) {
                selectedPosition = 0 // Для гостя позиция 0 теперь дом
                updateIcons()
            }
            invalidate()
            requestLayout()
        }
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
            if (icon.visibility != android.view.View.GONE) {
                icon.measure(iconMeasureSpec, iconMeasureSpec)
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        // Размещаем только видимые иконки равномерно по горизонтали
        val visibleIcons = iconViews.filter { it.visibility != android.view.View.GONE }
        if (visibleIcons.isEmpty()) return
        
        val iconSize = visibleIcons[0].measuredWidth
        val totalIconsWidth = iconSize * visibleIcons.size
        val spacing = (width - totalIconsWidth) / (visibleIcons.size + 1)

        var currentX = spacing.toFloat()
        val centerY = height / 2f

        iconViews.forEach { icon ->
            if (icon.visibility != android.view.View.GONE) {
                val left = currentX.toInt()
                val top = (centerY - iconSize / 2f).toInt()
                val right = left + iconSize
                val bottom = top + iconSize
                icon.layout(left, top, right, bottom)
                currentX += iconSize + spacing
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // Рисуем белый закругленный фон
        val rect = RectF(0f, 0f, width, height)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        // Рисуем черный круг для активной иконки (64x64dp)
        val visibleIcons = iconViews.filter { it.visibility != android.view.View.GONE }
        if (visibleIcons.isNotEmpty() && selectedPosition < iconViews.size) {
            val iconSize = visibleIcons[0].measuredWidth.toFloat()
            val totalIconsWidth = iconSize * visibleIcons.size
            val spacing = (width - totalIconsWidth) / (visibleIcons.size + 1)
            
            // Вычисляем позицию активной иконки среди видимых
            val visiblePosition = if (isGuestMode) {
                // Для гостя: позиция 1 (дом) = 0 в видимых, позиция 2 (галерея) = 1 в видимых
                when (selectedPosition) {
                    1 -> 0 // Дом
                    2 -> 1 // Галерея
                    else -> 0
                }
            } else {
                selectedPosition
            }
            
            val activeIconX = spacing + iconSize / 2f + visiblePosition * (iconSize + spacing)
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

