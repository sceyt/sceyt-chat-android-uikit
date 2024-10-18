package com.sceyt.chatuikit.styles.common

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import androidx.core.content.res.ResourcesCompat
import com.sceyt.chatuikit.styles.Style
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_RESOURCE
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_STYLE
import com.sceyt.chatuikit.styles.StyleConstants.styleOrDefault

data class TextStyle(
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val color: Int = UNSET_COLOR,
        @Px val size: Int = UNSET_SIZE,
        @FontRes val font: Int = UNSET_RESOURCE,
        @Style val style: Int = UNSET_STYLE
) {

    fun apply(textView: TextView) {
        if (size != UNSET_SIZE) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size.toFloat())
        }
        if (backgroundColor != UNSET_COLOR) {
            if (textView.background == null)
                textView.setBackgroundColor(color)
            else
                textView.background.setTint(backgroundColor)
        }
        if (color != UNSET_COLOR) {
            textView.setTextColor(color)
        }
        val typeface = if (font != UNSET_RESOURCE)
            ResourcesCompat.getFont(textView.context, font) else Typeface.DEFAULT

        textView.setTypeface(typeface, style.styleOrDefault(Typeface.NORMAL))
    }

    fun apply(
            context: Context,
            spannable: Spannable,
            start: Int = 0,
            end: Int = spannable.length
    ) {
        if (end - start <= 0) return
        if (color != UNSET_COLOR) {
            spannable.setSpan(ForegroundColorSpan(color), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (font != UNSET_RESOURCE)
            ResourcesCompat.getFont(context, font)?.let { typeface ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    spannable.setSpan(TypefaceSpan(typeface), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

        if (style != UNSET_STYLE) {
            spannable.setSpan(StyleSpan(style), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (size != UNSET_SIZE) {
            spannable.setSpan(AbsoluteSizeSpan(size), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun apply(context: Context, textPaint: TextPaint) {
        if (color != UNSET_COLOR) {
            textPaint.color = color
        }

        if (font != UNSET_RESOURCE) {
            val style = when (style) {
                Typeface.BOLD -> Typeface.BOLD
                Typeface.ITALIC -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            val typeface = ResourcesCompat.getFont(context, font) ?: Typeface.DEFAULT
            textPaint.typeface = Typeface.create(typeface, style)
        }

        if (style != UNSET_STYLE) {
            textPaint.isFakeBoldText = style == Typeface.BOLD
        }

        if (size != UNSET_SIZE) {
            textPaint.textSize = size.toFloat()
        }
    }

    internal class Builder(private val typedArray: TypedArray) {
        @ColorInt
        private var color = UNSET_COLOR

        @ColorInt
        private var backgroundColor = UNSET_COLOR

        @Px
        private var size = UNSET_SIZE

        @FontRes
        private var font = UNSET_RESOURCE

        private var style = UNSET_STYLE

        fun setStyle(@StyleableRes index: Int, defValue: Int = style) = apply {
            style = typedArray.getInt(index, defValue)
        }

        fun setColor(@StyleableRes index: Int, @ColorInt defValue: Int = color) = apply {
            color = typedArray.getColor(index, defValue)
        }

        fun setBackgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            backgroundColor = typedArray.getColor(index, defValue)
        }

        fun setSize(@StyleableRes index: Int, defValue: Int = size) = apply {
            size = typedArray.getDimensionPixelSize(index, defValue)
        }

        fun setFont(@StyleableRes index: Int, @FontRes defValue: Int = font) = apply {
            font = typedArray.getResourceId(index, defValue)
        }

        fun build() = TextStyle(
            backgroundColor = backgroundColor,
            color = color,
            size = size,
            style = style,
            font = font
        )
    }
}