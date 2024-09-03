package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.uicomponents.messageinfo.MessageInfoFragment
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [MessageInfoFragment]
 * @property backgroundColor Background color of the fragment, default is [SceytChatUIKitTheme.backgroundColor]
 * @property toolbarColor Color of the toolbar, default is [SceytChatUIKitTheme.primaryColor]
 * @property titleColor Color of the title, default is [SceytChatUIKitTheme.textPrimaryColor]
 * @property borderColor Color of the border, default is [SceytChatUIKitTheme.borderColor]
 * @property title Title of the fragment, default is [R.string.sceyt_message_info]
 * @property backIcon Icon for the back button, default is [R.drawable.sceyt_ic_arrow_back]
 * */
data class MessageInfoStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val toolbarColor: Int,
        @ColorInt val titleColor: Int,
        @ColorInt val borderColor: Int,
        val title: String,
        val backIcon: Drawable?,
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
                context.getCompatColor(SceytChatUIKit.theme.backgroundColor))

            val toolbarColor = typedArray.getColor(R.styleable.SceytMessageInfo_sceytUiInfoToolbarColor,
                context.getCompatColor(SceytChatUIKit.theme.primaryColor))

            val title = typedArray.getString(R.styleable.SceytMessageInfo_sceytUiInfoTitle)
                    ?: context.getString(R.string.sceyt_message_info)

            val titleColor = typedArray.getColor(R.styleable.SceytMessageInfo_sceytUiInfoTitleColor,
                context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

            val borderColor = typedArray.getColor(R.styleable.SceytMessageInfo_sceytUiInfoBorderColor,
                context.getCompatColor(SceytChatUIKit.theme.borderColor))

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