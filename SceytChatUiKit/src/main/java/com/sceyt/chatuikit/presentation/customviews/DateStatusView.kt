package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.util.Size
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isRtl
import kotlin.math.min

class DateStatusView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private lateinit var staticLayout: StaticLayout
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dateText = ""
    private var textSize = 30
    private var leadingIconSize = 0
    private var trailingIconSize = 0
    private var editedText: String = ""
    private var editedTextStyle: Int = Typeface.ITALIC
    private var leadingIcon: Drawable? = null
    private var trailingIcon: Drawable? = null
    private var isEdited = false
    private var ignoreRtl: Boolean = false
    private var cornerRadius: Int = 30
    private var isHighlighted = false
    private var backgroundRectF: RectF = RectF()

    private var leadingIconRect: Rect = Rect()
        get() = if (leadingIcon == null) Rect() else field

    private var trailingIconRect: Rect = Rect()
        get() = if (trailingIcon == null) Rect() else field

    private var leadingIconPadding: Int = 10
        get() = if (leadingIcon == null) 0 else field

    private var trailingIconPadding: Int = 10
        get() = if (trailingIcon == null) 0 else field

    private val highlightedStatePaddings by lazy {
        // left, top, right, bottom
        intArrayOf(7.dpToPx(), 2.dpToPx(), 5.dpToPx(), 2.dpToPx())
    }

    @ColorInt
    private var textColor = Color.BLACK

    @ColorInt
    private var iconsTintColor: Int = 0

    @ColorInt
    private var bgColor: Int = 0

    private val isRtl: Boolean
        get() = context.isRtl()

    private val textHeight: Int
        get() = if (dateText.isBlank()) 0 else staticLayout.height

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.DateStatusView)
            bgColor = a.getColor(R.styleable.DateStatusView_sceytUiDateStatusBackgroundColor, 0)
            cornerRadius = a.getDimensionPixelSize(R.styleable.DateStatusView_sceytUiDateStatusBackgroundCornerRadius, cornerRadius)
            leadingIcon = a.getDrawable(R.styleable.DateStatusView_sceytUiDateStatusLeadingIcon)?.mutate()
                    ?: leadingIcon
            trailingIcon = a.getDrawable(R.styleable.DateStatusView_sceytUiDateStatusTrailingIcon)?.mutate()
                    ?: trailingIcon
            iconsTintColor = a.getColor(R.styleable.DateStatusView_sceytUiDateStatusIconsTint, iconsTintColor)
            dateText = a.getString(R.styleable.DateStatusView_sceytUiDateStatusDateText)
                    ?: dateText
            textSize = a.getDimensionPixelSize(R.styleable.DateStatusView_sceytUiDateStatusDateTextSize, textSize)
            textColor = a.getColor(R.styleable.DateStatusView_sceytUiDateStatusDateTextColor, textColor)
            leadingIconPadding = a.getDimensionPixelSize(R.styleable.DateStatusView_sceytUiDateStatusLeadingIconPadding, leadingIconPadding)
            trailingIconPadding = a.getDimensionPixelSize(R.styleable.DateStatusView_sceytUiDateStatusTrailingIconPadding, trailingIconPadding)
            leadingIconSize = a.getDimensionPixelSize(R.styleable.DateStatusView_sceytUiDateStatusLeadingIconSize, leadingIconSize)
            trailingIconSize = a.getDimensionPixelSize(R.styleable.DateStatusView_sceytUiDateStatusTrailingIconSize, trailingIconSize)
            isHighlighted = a.getBoolean(R.styleable.DateStatusView_sceytUiDateStatusHighlighted, isHighlighted)
            ignoreRtl = a.getBoolean(R.styleable.DateStatusView_sceytUiDateStatusIgnoreRtl, ignoreRtl)
            a.recycle()
        }
        init()
    }

    private fun init() {
        textPaint.color = textColor
        textPaint.textSize = textSize.toFloat()
        backgroundPaint.color = bgColor
        backgroundPaint.style = Paint.Style.FILL

        setHighlightedState(isHighlighted)

        initStaticLayout()

        leadingIcon?.let {
            leadingIconRect = measureIcon(it, leadingIconSize, textHeight)
        }

        trailingIcon?.let {
            trailingIconRect = measureIcon(it, trailingIconSize, textHeight)
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (bgColor != 0)
            drawBackground(canvas)

        if (isRtl && !ignoreRtl) {
            // Apply paddings
            canvas.translate(realPaddingEnd.toFloat(), realPaddingTop.toFloat())
            // Draw trailing icon
            trailingIcon?.let { drawable ->
                drawTrailingIcon(canvas, drawable)
                // Move canvas to the end of the icon
                canvas.translate(trailingIconRect.width() + trailingIconPadding.toFloat(), 0f)
            }

            // Draw text
            drawText(canvas)
            // Move canvas to the end of the text
            canvas.translate(staticLayout.width.toFloat() + leadingIconPadding.toFloat(), 0f)

            // Draw leading icon
            leadingIcon?.let { drawable ->
                drawLeadingIcon(canvas, drawable)
            }
        } else {
            // Apply paddings
            canvas.translate(realPaddingStart.toFloat(), realPaddingTop.toFloat())
            // Draw leading icon
            leadingIcon?.let { drawable ->
                drawLeadingIcon(canvas, drawable)
                // Move canvas to the end of the icon
                canvas.translate(leadingIconRect.width() + leadingIconPadding.toFloat(), 0f)
            }

            // Draw text
            drawText(canvas)
            // Move canvas to the end of the text
            canvas.translate(staticLayout.width.toFloat() + trailingIconPadding.toFloat(), 0f)

            // Draw trailing icon
            trailingIcon?.let { drawable ->
                drawTrailingIcon(canvas, drawable)
            }
        }
    }

    private fun drawBackground(canvas: Canvas) {
        backgroundRectF.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(
            backgroundRectF,
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            backgroundPaint
        )
    }

    private fun drawLeadingIcon(canvas: Canvas, icon: Drawable) {
        canvas.save()
        val dyIcon = (heightWithoutPaddingVertical - leadingIconRect.height()) / 2f
        canvas.translate(0f, dyIcon)
        icon.bounds = leadingIconRect
        if (iconsTintColor != 0)
            icon.setTint(iconsTintColor)

        icon.draw(canvas)
        canvas.restore()
    }

    private fun drawText(canvas: Canvas) {
        val dyText = (heightWithoutPaddingVertical - staticLayout.height) / 2f
        canvas.save()
        canvas.translate(0f, dyText)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawTrailingIcon(canvas: Canvas, icon: Drawable) {
        canvas.save()
        val dyIcon = (heightWithoutPaddingVertical - trailingIconRect.height()) / 2f
        canvas.translate(0f, dyIcon)
        icon.bounds = trailingIconRect
        if (iconsTintColor != 0)
            icon.setTint(iconsTintColor)

        icon.draw(canvas)
        canvas.restore()
    }

    private fun initStaticLayout() {
        val dateTitle = initText(dateText)
        staticLayout = getStaticLayout(dateTitle, textPaint)
    }

    private fun measureIcon(icon: Drawable, size: Int, textHeight: Int): Rect {
        val iconSize = initIconSizeDependText(icon, size, textHeight)

        val sizeDiff = getStatusIconWidthHeightDiff(iconSize)
        val widthDiff = sizeDiff.first
        val heightDiff = sizeDiff.second

        val rect = Rect(widthDiff, heightDiff,
            iconSize.width + widthDiff, iconSize.height + heightDiff)

        return rect
    }

    private fun getStatusIconWidthHeightDiff(iconSize: Size): Pair<Int, Int> {
        val (widthIcon, heightIcon) = Pair(iconSize.width, iconSize.height)
        val widthDiff = if (widthIcon < heightIcon)
            (heightIcon - widthIcon) / 2 else 0
        val heightDiff = if (heightIcon in textHeight..<widthIcon)
            (widthIcon - heightIcon) / 2 else 0
        return Pair(widthDiff, heightDiff)
    }

    private fun initIconSizeDependText(icon: Drawable, size: Int, textHeight: Int = 0): Size {
        var iconSize: Int = size
        if (size == 0) {
            iconSize = if (textHeight != 0)
                textHeight
            else min(icon.intrinsicWidth, icon.intrinsicHeight)
        }
        return if (icon.intrinsicWidth > icon.intrinsicHeight) {
            val widthIcon = min(iconSize, Integer.max(iconSize, icon.intrinsicWidth))
            val heightIcon = widthIcon * icon.intrinsicHeight / icon.intrinsicWidth
            Size(widthIcon, heightIcon)
        } else {
            val heightIcon = min(iconSize, Integer.max(iconSize, icon.intrinsicHeight))
            val widthIcon = heightIcon * icon.intrinsicWidth / icon.intrinsicHeight
            Size(widthIcon, heightIcon)
        }
    }

    @Suppress("DEPRECATION")
    private fun getStaticLayout(stringBuilder: SpannableStringBuilder, textPaint: TextPaint): StaticLayout {
        val textWidth = textPaint.measureText(stringBuilder.toString()).toInt()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder
                .obtain(stringBuilder, 0, stringBuilder.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else StaticLayout(stringBuilder, textPaint, textWidth,
            Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
    }

    private fun setHighlightedState(highlighted: Boolean) {
        if (highlighted) {
            setPadding(7.dpToPx(), 2.dpToPx(), 5.dpToPx(), 2.dpToPx())
            textPaint.color = Color.WHITE
            bgColor = context.getCompatColor(R.color.sceyt_color_overlay_background)
        } else {
            textPaint.color = textColor
            background = null
        }
    }

    private fun initText(text: String): SpannableStringBuilder {
        return if (isEdited) {
            val str = SpannableStringBuilder("$editedText  $text")
            str.setSpan(StyleSpan(editedTextStyle), 0, editedText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            str
        } else SpannableStringBuilder(text)
    }

    private val realPaddingStart
        get() = if (isHighlighted && paddingStart == 0)
            highlightedStatePaddings[0] else paddingStart

    private val realPaddingEnd
        get() = if (isHighlighted && paddingEnd == 0)
            highlightedStatePaddings[2] else paddingEnd

    private val realPaddingTop
        get() = if (isHighlighted && paddingTop == 0)
            highlightedStatePaddings[1] else paddingTop

    private val realPaddingBottom
        get() = if (isHighlighted && paddingBottom == 0)
            highlightedStatePaddings[3] else paddingBottom

    private val heightWithoutPaddingVertical
        get() = height - realPaddingBottom - realPaddingTop

    fun setIconsSize(size: Int) {
        leadingIconSize = size
        trailingIconSize = size
        init()
        requestLayout()
        invalidate()
    }

    fun setDateText(text: String, edited: Boolean) {
        dateText = text
        isEdited = edited
        init()
        requestLayout()
        invalidate()
    }

    fun setIcons(leadingIcon: Drawable? = null,
                 trailingIcon: Drawable? = null,
                 ignoreHighlight: Boolean = false) {
        var leading = leadingIcon
        var trailing = trailingIcon
        if (isHighlighted && !ignoreHighlight) {
            leading = leadingIcon?.constantState?.newDrawable()?.apply { setTint(Color.WHITE) }
            trailing = trailingIcon?.constantState?.newDrawable()?.apply { setTint(Color.WHITE) }
        }
        this.leadingIcon = leading
        this.trailingIcon = trailing
        init()
        requestLayout()
        invalidate()
    }

    fun setDateAndStatusIcon(text: String,
                             leadingIcon: Drawable? = null,
                             trailingIcon: Drawable? = null,
                             edited: Boolean = false,
                             textColor: Int = this.textColor,
                             editedText: String = this.editedText,
                             editedTextStyle: Int = this.editedTextStyle,
                             ignoreHighlight: Boolean = false) {
        var leading = leadingIcon
        var trailing = trailingIcon
        if (isHighlighted && !ignoreHighlight) {
            leading = leadingIcon?.constantState?.newDrawable()?.apply { setTint(Color.WHITE) }
            trailing = trailingIcon?.constantState?.newDrawable()?.apply { setTint(Color.WHITE) }
        }

        this.leadingIcon = leading
        this.trailingIcon = trailing
        dateText = text
        isEdited = edited
        this.textColor = textColor
        this.editedText = editedText
        this.editedTextStyle = editedTextStyle
        init()
        requestLayout()
        invalidate()
    }

    fun setTextColorRes(@ColorRes color: Int) {
        textPaint.color = context.getCompatColor(color)
        invalidate()
    }

    fun setTextColor(@ColorInt color: Int) {
        textPaint.color = color
        invalidate()
    }

    fun setEditedText(editedText: String) {
        this.editedText = editedText
        invalidate()
    }

    fun setHighlighted(highlighted: Boolean) {
        if (isHighlighted == highlighted) return
        isHighlighted = highlighted
        init()
        invalidate()
    }

    fun setEdited(edited: Boolean) {
        isEdited = edited
        setDateText(dateText, isEdited)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val iconsPadding = leadingIconPadding + trailingIconPadding
        val width = staticLayout.width +
                leadingIconRect.width() +
                trailingIconRect.width() +
                iconsPadding +
                realPaddingStart + realPaddingEnd

        val leadingIconHeight = leadingIconRect.height()
        val trailingIconHeight = trailingIconRect.height()
        val maxIconHeight = Integer.max(leadingIconHeight, trailingIconHeight)
        val height = staticLayout.height.coerceAtLeast(maxIconHeight) + realPaddingTop + realPaddingBottom
        setMeasuredDimension(width, height)
    }

    inner class BuildStyle {
        private var leadingIconSize: Int = this@DateStatusView.leadingIconSize
        private var trailingIconSize: Int = this@DateStatusView.trailingIconSize
        private var leadingIcon: Drawable? = this@DateStatusView.leadingIcon
        private var trailingIcon: Drawable? = this@DateStatusView.trailingIcon
        private var dateText: String = ""
        private var dateTextColor: Int = this@DateStatusView.textColor
        private var isEdited: Boolean = this@DateStatusView.isEdited
        private var editedText: String = this@DateStatusView.editedText
        private var editedTextStyle: Int = this@DateStatusView.editedTextStyle

        fun setLeadingIcon(drawable: Drawable?): BuildStyle {
            leadingIcon = drawable
            return this
        }

        fun setTrailingIcon(drawable: Drawable?): BuildStyle {
            trailingIcon = drawable
            return this
        }

        fun setLeadingIconSize(size: Int): BuildStyle {
            leadingIconSize = size
            return this
        }

        fun setTrailingIconSize(size: Int): BuildStyle {
            trailingIconSize = size
            return this
        }

        fun setDateText(text: String): BuildStyle {
            dateText = text
            return this
        }

        fun setDateColorId(@ColorRes colorId: Int): BuildStyle {
            dateTextColor = context.getCompatColor(colorId)
            return this
        }

        fun setDateColor(@ColorInt color: Int): BuildStyle {
            dateTextColor = color
            return this
        }

        fun setEditedTitle(editedText: String): BuildStyle {
            this.editedText = editedText
            return this
        }

        fun edited(boolean: Boolean): BuildStyle {
            this.isEdited = boolean
            return this
        }

        fun setEditedTextStyle(style: Int): BuildStyle {
            editedTextStyle = style
            return this
        }

        fun build() {
            this@DateStatusView.leadingIconSize = leadingIconSize
            this@DateStatusView.trailingIconSize = trailingIconSize
            this@DateStatusView.leadingIcon = leadingIcon?.mutate()
            this@DateStatusView.trailingIcon = trailingIcon?.mutate()
            this@DateStatusView.textColor = dateTextColor
            this@DateStatusView.editedText = editedText
            this@DateStatusView.isEdited = isEdited
            this@DateStatusView.editedTextStyle = editedTextStyle
            this@DateStatusView.dateText = dateText


            init()
            requestLayout()
            invalidate()
        }
    }

    fun buildStyle() = BuildStyle()
}