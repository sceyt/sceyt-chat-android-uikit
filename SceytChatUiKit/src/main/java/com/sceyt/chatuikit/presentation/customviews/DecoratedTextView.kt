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

class DecoratedTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private lateinit var staticLayout: StaticLayout
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var text = ""
    private var textSize = 30
    private var leadingIconSize = 0
    private var trailingIconSize = 0
    private var leadingText: String = ""
    private var leadingTextStyle: Int = Typeface.ITALIC
    private var leadingIcon: Drawable? = null
    private var trailingIcon: Drawable? = null
    private var enableLeadingText = false
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
        get() = if (text.isBlank()) 0 else staticLayout.height

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.DecoratedTextView)
            bgColor = a.getColor(R.styleable.DecoratedTextView_sceytUiDecoratedTextBackgroundColor, 0)
            cornerRadius = a.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextBackgroundCornerRadius, cornerRadius)
            leadingIcon = a.getDrawable(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingIcon)?.mutate()
                    ?: leadingIcon
            trailingIcon = a.getDrawable(R.styleable.DecoratedTextView_sceytUiDecoratedTextTrailingIcon)?.mutate()
                    ?: trailingIcon
            iconsTintColor = a.getColor(R.styleable.DecoratedTextView_sceytUiDecoratedTextIconsTint, iconsTintColor)
            text = a.getString(R.styleable.DecoratedTextView_sceytUiDecoratedTextDateText)
                    ?: text
            textSize = a.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextDateTextSize, textSize)
            textColor = a.getColor(R.styleable.DecoratedTextView_sceytUiDecoratedTextDateTextColor, textColor)
            leadingIconPadding = a.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingIconPadding, leadingIconPadding)
            trailingIconPadding = a.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextTrailingIconPadding, trailingIconPadding)
            leadingIconSize = a.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingIconSize, leadingIconSize)
            trailingIconSize = a.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextTrailingIconSize, trailingIconSize)
            isHighlighted = a.getBoolean(R.styleable.DecoratedTextView_sceytUiDecoratedTextHighlighted, isHighlighted)
            ignoreRtl = a.getBoolean(R.styleable.DecoratedTextView_sceytUiDecoratedTextIgnoreRtl, ignoreRtl)
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
        val dateTitle = initText(text)
        staticLayout = getStaticLayout(dateTitle, textPaint)
    }

    private fun measureIcon(icon: Drawable, size: Int, textHeight: Int): Rect {
        val iconSize = initIconSizeDependText(icon, size, textHeight)

        val sizeDiff = getIconWidthHeightDiff(iconSize)
        val widthDiff = sizeDiff.first
        val heightDiff = sizeDiff.second

        val rect = Rect(widthDiff, heightDiff,
            iconSize.width + widthDiff, iconSize.height + heightDiff)

        return rect
    }

    private fun getIconWidthHeightDiff(iconSize: Size): Pair<Int, Int> {
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
        return if (enableLeadingText) {
            val str = SpannableStringBuilder("$leadingText  $text")
            str.setSpan(StyleSpan(leadingTextStyle), 0, leadingText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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

    fun setText(text: String, enableLeadingText: Boolean) {
        this.text = text
        this.enableLeadingText = enableLeadingText
        init()
        requestLayout()
        invalidate()
    }

    fun setIcons(leadingIcon: Drawable? = null,
                 trailingIcon: Drawable? = null,
                 ignoreHighlight: Boolean = false
    ) {
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

    fun setTextAndIcons(text: String,
                        leadingIcon: Drawable? = null,
                        trailingIcon: Drawable? = null,
                        enableLeadingText: Boolean = false,
                        textColor: Int = this.textColor,
                        leadingText: String = this.leadingText,
                        leadingTextStyle: Int = this.leadingTextStyle,
                        ignoreHighlight: Boolean = false
    ) {
        var leading = leadingIcon
        var trailing = trailingIcon
        if (isHighlighted && !ignoreHighlight) {
            leading = leadingIcon?.constantState?.newDrawable()?.apply { setTint(Color.WHITE) }
            trailing = trailingIcon?.constantState?.newDrawable()?.apply { setTint(Color.WHITE) }
        }

        this.leadingIcon = leading
        this.trailingIcon = trailing
        this.text = text
        this.enableLeadingText = enableLeadingText
        this.textColor = textColor
        this.leadingText = leadingText
        this.leadingTextStyle = leadingTextStyle
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

    fun setLeadingText(text: String) {
        leadingText = text
        invalidate()
    }

    fun setHighlighted(highlighted: Boolean) {
        if (isHighlighted == highlighted) return
        isHighlighted = highlighted
        init()
        invalidate()
    }

    fun enableLeadingText(enable: Boolean) {
        enableLeadingText = enable
        setText(text, enableLeadingText)
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
        private var leadingIconSize: Int = this@DecoratedTextView.leadingIconSize
        private var trailingIconSize: Int = this@DecoratedTextView.trailingIconSize
        private var leadingIcon: Drawable? = this@DecoratedTextView.leadingIcon
        private var trailingIcon: Drawable? = this@DecoratedTextView.trailingIcon
        private var text: String = ""
        private var textColor: Int = this@DecoratedTextView.textColor
        private var enableLeadingText: Boolean = this@DecoratedTextView.enableLeadingText
        private var leadingText: String = this@DecoratedTextView.leadingText
        private var leadingTextStyle: Int = this@DecoratedTextView.leadingTextStyle

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

        fun setText(text: String): BuildStyle {
            this.text = text
            return this
        }

        fun setTextColorId(@ColorRes colorId: Int): BuildStyle {
            textColor = context.getCompatColor(colorId)
            return this
        }

        fun setTextColor(@ColorInt color: Int): BuildStyle {
            textColor = color
            return this
        }

        fun setLeadingText(text: String): BuildStyle {
            leadingText = text
            return this
        }

        fun enableLeading(enable: Boolean): BuildStyle {
            enableLeadingText = enable
            return this
        }

        fun setLeadingTextStyle(style: Int): BuildStyle {
            leadingTextStyle = style
            return this
        }

        fun build() {
            this@DecoratedTextView.leadingIconSize = leadingIconSize
            this@DecoratedTextView.trailingIconSize = trailingIconSize
            this@DecoratedTextView.leadingIcon = leadingIcon?.mutate()
            this@DecoratedTextView.trailingIcon = trailingIcon?.mutate()
            this@DecoratedTextView.textColor = textColor
            this@DecoratedTextView.leadingText = leadingText
            this@DecoratedTextView.enableLeadingText = enableLeadingText
            this@DecoratedTextView.leadingTextStyle = leadingTextStyle
            this@DecoratedTextView.text = text


            init()
            requestLayout()
            invalidate()
        }
    }

    fun buildStyle() = BuildStyle()
}