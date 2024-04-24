package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable

data class ConversationHeaderViewStyle(
        @ColorInt var titleColor: Int,
        @ColorInt var subTitleColor: Int,
        @ColorInt var underlineColor: Int,
        var backIcon: Drawable?,
        var enableUnderline: Boolean
) {

    companion object {
        var conversationHeaderViewStyleCustomizer = StyleCustomizer<ConversationHeaderViewStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?

    ) {

        fun build(): ConversationHeaderViewStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ConversationHeaderView, 0, 0)

            val backIcon = typedArray.getDrawable(R.styleable.ConversationHeaderView_sceytConvHeaderBackIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)

            val titleColor = typedArray.getColor(R.styleable.ConversationHeaderView_sceytConvHeaderTitleColor,
                context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

            val subTitleColor = typedArray.getColor(R.styleable.ConversationHeaderView_sceytConvHeaderSubTitleColor,
                context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

            val underlineColor = typedArray.getColor(R.styleable.ConversationHeaderView_sceytConvHeaderUnderlineColor,
                context.getCompatColor(SceytChatUIKit.theme.bordersColor))

            val enableUnderline = typedArray.getBoolean(R.styleable.ConversationHeaderView_sceytConvHeaderEnableUnderline, true)

            typedArray.recycle()

            return ConversationHeaderViewStyle(
                titleColor = titleColor,
                subTitleColor = subTitleColor,
                underlineColor = underlineColor,
                backIcon = backIcon,
                enableUnderline = enableUnderline
            ).let(conversationHeaderViewStyleCustomizer::apply)
        }
    }
}