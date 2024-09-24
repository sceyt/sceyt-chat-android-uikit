package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatTextView
import com.sceyt.chatuikit.extensions.getCompatColor

class ColorSpannableTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var spanColorId: Int = 0
    private var fromIndex: Int = 0
    private var toIndex: Int = 0
    private var flag: Int = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    private var spanString: SpannableString = SpannableString("")
    private var buildSpannable: BuildSpannable? = null


    inner class BuildSpannable {
        private var spanColorId: Int = this@ColorSpannableTextView.spanColorId
        private var fromIndex: Int = this@ColorSpannableTextView.fromIndex
        private var toIndex: Int = this@ColorSpannableTextView.toIndex
        private var flag: Int = this@ColorSpannableTextView.flag
        private var spanString: SpannableString = this@ColorSpannableTextView.spanString

        fun setForegroundColorId(@ColorRes colorId: Int): BuildSpannable {
            spanColorId = colorId
            return this
        }

        fun setIndexSpan(fromIndex: Int, toIndex: Int): BuildSpannable {
            this.fromIndex = fromIndex
            this.toIndex = toIndex
            return this
        }

        fun setString(text: String): BuildSpannable {
            spanString = SpannableString(text)
            return this
        }

        fun setSpannableString(text: CharSequence): BuildSpannable {
            spanString = SpannableString(text)
            return this
        }

        fun append(text: CharSequence): BuildSpannable {
            if (text.isEmpty()) return this
            spanString = SpannableString(TextUtils.concat(spanString, text))
            return this
        }

        fun setFlag(flag: Int): BuildSpannable {
            this.flag = flag
            return this
        }

        fun build(): SpannableString {
            if (fromIndex < toIndex)
                spanString.setSpan(ForegroundColorSpan(context.getCompatColor(spanColorId)), fromIndex, toIndex, flag)
            setSpannableText(spanString)
            buildSpannable = this
            return spanString
        }
    }

    private fun setSpannableText(text: SpannableString) {
        super.setText(text)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        buildSpannable = null
        super.setText(text, type)
    }

    fun buildSpannable() = BuildSpannable()

    fun invalidateColor() {
        buildSpannable?.build()
    }
}