package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Size
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isRtl
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.styles.common.TextStyle
import kotlin.math.min

class DecoratedTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private lateinit var staticLayout: StaticLayout
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var text: CharSequence = ""
    private var leadingText: String = ""
    private var textStyle: TextStyle = TextStyle()
    private var leadingTextStyle: TextStyle = TextStyle()
    private var leadingIconSize = 0
    private var trailingIconSize = 0
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
    private var iconsTintColor: Int = 0

    @ColorInt
    private var bgColor: Int = 0

    private val isRtl: Boolean
        get() = context.isRtl()

    private val textHeight: Int
        get() = if (text.isBlank()) 0 else staticLayout.height

    init {
        attrs?.let {
            context.obtainStyledAttributes(attrs, R.styleable.DecoratedTextView).use { array ->
                bgColor = array.getColor(R.styleable.DecoratedTextView_sceytUiDecoratedTextBackgroundColor, 0)
                cornerRadius = array.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextBackgroundCornerRadius, cornerRadius)
                leadingIcon = array.getDrawable(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingIcon)?.mutate()
                        ?: leadingIcon
                trailingIcon = array.getDrawable(R.styleable.DecoratedTextView_sceytUiDecoratedTextTrailingIcon)?.mutate()
                        ?: trailingIcon
                iconsTintColor = array.getColor(R.styleable.DecoratedTextView_sceytUiDecoratedTextIconsTint, iconsTintColor)
                text = array.getString(R.styleable.DecoratedTextView_sceytUiDecoratedTextTitle)
                        ?: text
                leadingText = array.getString(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingText)
                        ?: leadingText
                textStyle = TextStyle.Builder(array)
                    .setSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextSize, 30)
                    .setColor(R.styleable.DecoratedTextView_sceytUiDecoratedTextColor, Color.BLACK)
                    .setFont(R.styleable.DecoratedTextView_sceytUiDecoratedTextFont)
                    .setStyle(R.styleable.DecoratedTextView_sceytUiDecoratedTextStyle)
                    .build()
                leadingTextStyle = TextStyle.Builder(array)
                    .setSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingTextSize, 30)
                    .setColor(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingTextColor, Color.BLACK)
                    .setFont(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingTextFont)
                    .setStyle(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingTextStyle)
                    .build()
                leadingIconPadding = array.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingIconPadding, leadingIconPadding)
                trailingIconPadding = array.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextTrailingIconPadding, trailingIconPadding)
                leadingIconSize = array.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextLeadingIconSize, leadingIconSize)
                trailingIconSize = array.getDimensionPixelSize(R.styleable.DecoratedTextView_sceytUiDecoratedTextTrailingIconSize, trailingIconSize)
                isHighlighted = array.getBoolean(R.styleable.DecoratedTextView_sceytUiDecoratedTextHighlighted, isHighlighted)
                ignoreRtl = array.getBoolean(R.styleable.DecoratedTextView_sceytUiDecoratedTextIgnoreRtl, ignoreRtl)

                init()
            }
        }
    }

    private fun init() {
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
    private fun getStaticLayout(text: CharSequence, textPaint: TextPaint): StaticLayout {
        val textWidth = Layout.getDesiredWidth(text, textPaint).roundUp()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
        } else StaticLayout(text, textPaint, textWidth,
            Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
    }

    private fun setHighlightedState(highlighted: Boolean) {
        if (highlighted) {
            setPadding(7.dpToPx(), 2.dpToPx(), 5.dpToPx(), 2.dpToPx())
            bgColor = context.getCompatColor(R.color.sceyt_color_overlay_background)
        } else {
            background = null
        }
    }

    private fun initText(text: CharSequence): SpannableStringBuilder {
        var tvStyle = textStyle
        var ldStyle = leadingTextStyle
        if (isHighlighted) {
            tvStyle = textStyle.copy(color = Color.WHITE)
            ldStyle = leadingTextStyle.copy(color = Color.WHITE)
        }
        return if (enableLeadingText) {
            val str = SpannableStringBuilder("$leadingText  $text")
            ldStyle.apply(context, str, 0, leadingText.length)
            tvStyle.apply(context, str, leadingText.length + 2, str.length)
            str
        } else SpannableStringBuilder(text).apply {
            tvStyle.apply(context, this)
        }
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

    fun setText(text: CharSequence, enableLeadingText: Boolean) {
        this.text = text
        this.enableLeadingText = enableLeadingText
        init()
        requestLayout()
        invalidate()
    }

    fun setIcons(
            leadingIcon: Drawable? = null,
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

    fun setTextAndIcons(
            text: CharSequence,
            leadingIcon: Drawable? = null,
            trailingIcon: Drawable? = null,
            enableLeadingText: Boolean = false,
            textColor: Int = this.textStyle.color,
            leadingText: String = this.leadingText,
            leadingTextStyle: TextStyle = this.leadingTextStyle,
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
        this.textStyle = textStyle.copy(color = textColor)
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
        setText(leadingText, enableLeadingText)
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

    inner class StyleBuilder {
        private var leadingIconSize: Int = this@DecoratedTextView.leadingIconSize
        private var trailingIconSize: Int = this@DecoratedTextView.trailingIconSize
        private var leadingIcon: Drawable? = this@DecoratedTextView.leadingIcon
        private var trailingIcon: Drawable? = this@DecoratedTextView.trailingIcon

        private var text: CharSequence = this@DecoratedTextView.text
        private var textStyle: TextStyle = this@DecoratedTextView.textStyle
        private var leadingText: String = this@DecoratedTextView.leadingText
        private var leadingTextStyle: TextStyle = this@DecoratedTextView.leadingTextStyle
        private var enableLeadingText: Boolean = this@DecoratedTextView.enableLeadingText

        fun setLeadingIcon(drawable: Drawable?): StyleBuilder {
            leadingIcon = drawable
            return this
        }

        fun setTrailingIcon(drawable: Drawable?): StyleBuilder {
            trailingIcon = drawable
            return this
        }

        fun setLeadingIconSize(size: Int): StyleBuilder {
            leadingIconSize = size
            return this
        }

        fun setTrailingIconSize(size: Int): StyleBuilder {
            trailingIconSize = size
            return this
        }

        fun setText(text: String): StyleBuilder {
            this.text = text
            return this
        }

        fun setLeadingText(text: String): StyleBuilder {
            this.leadingText = text
            return this
        }

        fun enableLeading(enable: Boolean): StyleBuilder {
            this.enableLeadingText = enable
            return this
        }

        fun setTextStyle(textStyle: TextStyle): StyleBuilder {
            this.textStyle = textStyle
            return this
        }

        fun setLeadingTextStyle(textStyle: TextStyle): StyleBuilder {
            this.leadingTextStyle = textStyle
            return this
        }

        fun build() {
            this@DecoratedTextView.leadingIconSize = leadingIconSize
            this@DecoratedTextView.trailingIconSize = trailingIconSize
            this@DecoratedTextView.leadingIcon = leadingIcon?.mutate()
            this@DecoratedTextView.trailingIcon = trailingIcon?.mutate()
            this@DecoratedTextView.text = text
            this@DecoratedTextView.textStyle = textStyle
            this@DecoratedTextView.leadingText = leadingText
            this@DecoratedTextView.leadingTextStyle = leadingTextStyle
            this@DecoratedTextView.enableLeadingText = enableLeadingText

            init()
            requestLayout()
            invalidate()
        }
    }

    fun styleBuilder() = StyleBuilder()
}