package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.media.RatingCompat.Style
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.isRtl
import kotlin.math.absoluteValue
import kotlin.math.min


class SceytDateStatusView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var textBoundsRect: Rect
    private lateinit var iconBoundsRect: Rect
    private lateinit var stringBuilder: SpannableStringBuilder
    private var dateText = ""
    private var textSize = 30
    private var statusIconSize = 0
    private var statusIconMargin = 0
    private var heightIcon = 0
    private var widthIcon = 0
    private var textColor = context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor)
    private var editedText: String = ""
    private var editedTextStyle: Int = Typeface.ITALIC
    private var statusDrawable: Drawable? = null
    private var firstStatusIcon = true
    private var mMargin = 0
    private var mIconSize = 0
    private var mMinHeightSize = 0
    private var isHighlighted = false
    private var isEdited = false
    private var ignoreRtl: Boolean = false
    private lateinit var paddings: IntArray
    private val showFirstStatus get() = firstStatusIcon or (!ignoreRtl && context.isRtl())

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SceytDateStatusView)
            statusDrawable = a.getDrawable(R.styleable.SceytDateStatusView_sceytDateStatusViewStatusIcon)?.mutate()
            dateText = a.getString(R.styleable.SceytDateStatusView_sceytDateStatusViewDateText)
                    ?: dateText
            textSize = a.getDimensionPixelSize(R.styleable.SceytDateStatusView_sceytDateStatusViewDateTextSize, textSize)
            textColor = a.getColor(R.styleable.SceytDateStatusView_sceytDateStatusViewDateTextColor, textColor)
            statusIconMargin = a.getDimensionPixelSize(R.styleable.SceytDateStatusView_sceytDateStatusViewStatusIconMargin, statusIconMargin)
            statusIconSize = a.getDimensionPixelSize(R.styleable.SceytDateStatusView_sceytDateStatusViewStatusIconSize, 0)
            firstStatusIcon = a.getBoolean(R.styleable.SceytDateStatusView_sceytDateStatusViewFirstStatus, firstStatusIcon)
            ignoreRtl = a.getBoolean(R.styleable.SceytDateStatusView_sceytDateStatusViewIgnoreRtl, ignoreRtl)
            isHighlighted = a.getBoolean(R.styleable.SceytDateStatusView_sceytDateStatusViewHighlighted, isHighlighted)
            getPaddingsFromAttr(a)
            a.recycle()
        }
        init()
    }

    private fun getPaddingsFromAttr(typedArray: TypedArray) {
        /** For highlighted state.
         *  After removing state, need to set initial paddings.*/
        paddings = IntArray(4)
        val padding = typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_padding, 0)
        if (padding != 0) {
            paddings.forEachIndexed { i, _ ->
                paddings[i] = padding
            }
        } else {
            // padding start
            paddings[0] = typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_paddingStart,
                typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_paddingHorizontal, 0))
            // padding top
            paddings[1] = typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_paddingTop,
                typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_paddingVertical, 0))
            // padding end
            paddings[2] = typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_paddingEnd,
                typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_paddingHorizontal, 0))
            // padding bottom
            paddings[3] = typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_paddingBottom,
                typedArray.getDimensionPixelSize(R.styleable.SceytDateStatusView_android_paddingVertical, 0))
        }
    }

    private fun init() {
        textPaint.color = textColor
        textPaint.textSize = textSize.toFloat()
        mMargin = statusIconMargin
        mIconSize = statusIconSize
        mMinHeightSize = statusIconSize
        textBoundsRect = Rect()
        iconBoundsRect = Rect()

        setHighlightedState(isHighlighted)

        checkSizesAndMargins()
        if (showFirstStatus)
            measureViewsFirstStatus()
        else measureViewsFirstText()
    }

    private fun measureViewsFirstText() {
        val dateTitle = initText(dateText)
        textPaint.getTextBounds(stringBuilder.toString(), 0, dateTitle.length, textBoundsRect)

        if (statusDrawable != null) {
            initStatsIconSize(statusDrawable!!)

            val left = textBoundsRect.right + mMargin - textBoundsRect.left
            val top = getTopFormIcon()
            val sizeDiff = getStatusIconWidthHeightDiff()
            val widthDiff = sizeDiff.first
            val heightDiff = sizeDiff.second

            iconBoundsRect.set(left + widthDiff, top + heightDiff,
                left + widthIcon + widthDiff, top + heightIcon + heightDiff)
        }
    }

    private fun measureViewsFirstStatus() {
        val dateTitle = initText(dateText)
        textPaint.getTextBounds(dateTitle, 0, dateTitle.length, textBoundsRect)

        statusDrawable?.let {
            initStatsIconSize(it)

            val left = 0
            val sizeDiff = getStatusIconWidthHeightDiff()
            val widthDiff = sizeDiff.first
            val heightDiff = sizeDiff.second

            val top = getTopFormIcon()
            iconBoundsRect.set(left + widthDiff, top + heightDiff,
                left + widthIcon + widthDiff, top + heightIcon + heightDiff)
        }
    }

    private fun getStatusIconWidthHeightDiff(): Pair<Int, Int> {
        val widthDiff = if (widthIcon < heightIcon) (heightIcon - widthIcon) / 2 else 0
        val heightDiff = if (heightIcon < widthIcon && Integer.max(heightIcon, mIconSize) >= textBoundsRect.height()) (widthIcon - heightIcon) / 2 else 0
        return Pair(widthDiff, heightDiff)
    }

    private fun checkSizesAndMargins() {
        if (statusDrawable == null)
            mIconSize = 0

        if (statusDrawable == null || dateText.isBlank())
            mMargin = 0
    }

    private fun initStatsIconSize(icon: Drawable) {
        if (mIconSize == 0) {
            mIconSize = if (textBoundsRect.height() != 0)
                textBoundsRect.height()
            else min(icon.intrinsicWidth, icon.intrinsicHeight)
        }

        if (icon.intrinsicWidth > icon.intrinsicHeight) {
            widthIcon = min(mIconSize, Integer.max(mIconSize, icon.intrinsicWidth))
            heightIcon = widthIcon * icon.intrinsicHeight / icon.intrinsicWidth
        } else {
            heightIcon = min(mIconSize, Integer.max(mIconSize, icon.intrinsicHeight))
            widthIcon = heightIcon * icon.intrinsicWidth / icon.intrinsicHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showFirstStatus) {
            //Draw status icon
            canvas.translate(paddingStart.toFloat(), paddingTop.toFloat())
            statusDrawable?.let {
                it.bounds = iconBoundsRect
                it.draw(canvas)
            }
            //Draw text
            initText(dateText)
            val staticLayout = getStaticLayout()
            canvas.translate((Integer.max(iconBoundsRect.right, mIconSize) + mMargin).toFloat(),
                ((height + paddingTop - paddingBottom) - staticLayout.height) / 2f - paddingTop.toFloat())
            staticLayout.draw(canvas)
        } else {
            //Draw text
            initText(dateText)
            val staticLayout = getStaticLayout()
            canvas.save()
            canvas.translate(paddingStart.toFloat(), ((height + paddingTop - paddingBottom) - staticLayout.height) / 2f)
            staticLayout.draw(canvas)
            canvas.restore()
            canvas.translate(paddingStart.toFloat(), paddingTop.toFloat())
            //Draw status icon
            statusDrawable?.let {
                it.bounds = iconBoundsRect
                it.draw(canvas)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getStaticLayout(): StaticLayout {
        val textWidth = textPaint.measureText(stringBuilder.toString()).toInt()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(stringBuilder, 0, stringBuilder.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false).build()
        } else StaticLayout(stringBuilder, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
    }

    /** For center vertical text date when icon is bigger */
    private fun getTopFormText(): Int {
        val realHeight = Integer.max(heightIcon, mMinHeightSize)
        return if (textBoundsRect.height() < realHeight)
            (realHeight - (textBoundsRect.top.absoluteValue)) / 2
        else 0
    }

    /** For center vertical status icon when text height is bigger*/
    private fun getTopFormIcon(): Int {
        return if (Integer.max(heightIcon, mIconSize) < textBoundsRect.height()) {
            (textBoundsRect.height() - heightIcon) / 2
        } else 0
    }

    private fun setHighlightedState(highlighted: Boolean) {
        if (highlighted) {
            textPaint.color = Color.WHITE
            background = context.getCompatDrawable(R.drawable.sceyt_date_transparent_background)
        } else {
            textPaint.color = textColor
            background = null
            setPadding(paddings[0], paddings[1], paddings[2], paddings[3])
        }
    }

    private fun initText(text: String): String {
        return if (isEdited) {
            val str = SpannableStringBuilder("$editedText  $text")
            str.setSpan(StyleSpan(editedTextStyle), 0, editedText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            stringBuilder = str
            str.toString()
        } else {
            stringBuilder = SpannableStringBuilder(text)
            text
        }
    }

    fun setStatusIconSize(size: Int) {
        statusIconSize = size
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

    fun setStatusIcon(drawable: Drawable?, ignoreHighlight: Boolean = false) {
        var statusIcon = drawable
        if (isHighlighted && !ignoreHighlight)
            statusIcon = drawable?.constantState?.newDrawable()?.apply { setTint(Color.WHITE) }

        statusDrawable = statusIcon
        init()
        requestLayout()
        invalidate()
    }

    fun setDateAndStatusIcon(text: String,
                             drawable: Drawable?,
                             edited: Boolean,
                             textColor: Int = this.textColor,
                             editedText: String = this.editedText,
                             editedTextStyle: Int = this.editedTextStyle,
                             ignoreHighlight: Boolean = false) {
        var statusIcon = drawable
        if (isHighlighted && !ignoreHighlight)
            statusIcon = drawable?.constantState?.newDrawable()?.apply { setTint(Color.WHITE) }

        statusDrawable = statusIcon
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

    inner class BuildStyle {
        private var statusIconSize: Int = this@SceytDateStatusView.statusIconSize
        private var statusIcon: Drawable? = null
        private var dateText: String = ""
        private var dateTextColor: Int = this@SceytDateStatusView.textColor
        private var isEdited: Boolean = this@SceytDateStatusView.isEdited
        private var editedText: String = this@SceytDateStatusView.editedText
        private var editedTextStyle: Int = this@SceytDateStatusView.editedTextStyle
        private var ignoreHighlight: Boolean = false

        fun setStatusIconSize(size: Int): BuildStyle {
            statusIconSize = size
            return this
        }

        fun setStatusIcon(drawable: Drawable?): BuildStyle {
            statusIcon = drawable
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

        fun setEditedTextStyle(@Style style: Int): BuildStyle {
            editedTextStyle = style
            return this
        }

        fun build() {
            this@SceytDateStatusView.statusIconSize = statusIconSize
            this@SceytDateStatusView.statusDrawable = statusIcon?.mutate()
            this@SceytDateStatusView.textColor = dateTextColor
            this@SceytDateStatusView.editedText = editedText
            this@SceytDateStatusView.isEdited = isEdited
            this@SceytDateStatusView.editedTextStyle = editedTextStyle
            this@SceytDateStatusView.dateText = dateText


            init()
            requestLayout()
            invalidate()
        }
    }

    fun buildStyle() = BuildStyle()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = textPaint.measureText(stringBuilder.toString()).toInt() + mMargin + Integer.max(iconBoundsRect.width(), mIconSize) + paddingStart + paddingEnd
        val height = textBoundsRect.height().coerceAtLeast(Integer.max(iconBoundsRect.height(), mMinHeightSize)) + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }

    fun setEdited(edited: Boolean) {
        isEdited = edited
        setDateText(dateText, isEdited)
    }
}