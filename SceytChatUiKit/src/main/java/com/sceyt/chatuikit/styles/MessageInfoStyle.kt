package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.components.message_info.MessageInfoFragment
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [MessageInfoFragment]
 * @property backgroundColor Background color of the fragment, default is [Colors.backgroundColor]
 * @property toolbarColor Color of the toolbar, default is [Colors.primaryColor]
 * @property titleColor Color of the title, default is [Colors.textPrimaryColor]
 * @property borderColor Color of the border, default is [Colors.borderColor]
 * @property title Title of the fragment, default is [R.string.sceyt_message_info]
 * @property backIcon Icon for the back button, default is [R.drawable.sceyt_ic_arrow_back]
 * */
data class MessageInfoStyle(
        @ColorInt var backgroundColor: Int,
        @ColorInt var toolbarColor: Int,
        @ColorInt var titleColor: Int,
        @ColorInt var borderColor: Int,
        var title: String,
        var backIcon: Drawable?,
) {

    companion object {
        var styleCustomizer = StyleCustomizer<MessageInfoStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessageInfoStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SceytMessageInfo)

            val backgroundColor = typedArray.getColor(R.styleable.SceytMessageInfo_sceytUiInfoBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))

            val toolbarColor = typedArray.getColor(R.styleable.SceytMessageInfo_sceytUiInfoToolbarColor,
                context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor))

            val title = typedArray.getString(R.styleable.SceytMessageInfo_sceytUiInfoTitle)
                    ?: context.getString(R.string.sceyt_message_info)

            val titleColor = typedArray.getColor(R.styleable.SceytMessageInfo_sceytUiInfoTitleColor,
                context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))

            val borderColor = typedArray.getColor(R.styleable.SceytMessageInfo_sceytUiInfoBorderColor,
                context.getCompatColor(SceytChatUIKit.theme.colors.borderColor))

            val backIcon = typedArray.getDrawable(R.styleable.SceytMessageInfo_sceytUiInfoBackIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)

            typedArray.recycle()

            return MessageInfoStyle(
                backgroundColor = backgroundColor,
                toolbarColor = toolbarColor,
                title = title,
                titleColor = titleColor,
                borderColor = borderColor,
                backIcon = backIcon
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}