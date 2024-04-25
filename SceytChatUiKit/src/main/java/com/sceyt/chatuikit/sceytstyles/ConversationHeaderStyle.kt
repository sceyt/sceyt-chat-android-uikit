package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme


/**
 * Style for [ConversationHeaderView] component.
 * @property backgroundColor background color of the header, default is [SceytChatUIKitTheme.primaryColor]
 * @property titleColor color of the title, default is [SceytChatUIKitTheme.textPrimaryColor]
 * @property subTitleColor color of the subtitle, default is [SceytChatUIKitTheme.textSecondaryColor]
 * @property underlineColor color of the underline, default is [SceytChatUIKitTheme.bordersColor]
 * @property backIcon icon for back button, default is [R.drawable.sceyt_ic_arrow_back]
 * @property enableUnderline enable underline, default is true
 * @property menuStyle style for the toolbar menu, default is [R.style.SceytPopupMenuStyle]
 * @property menuTitleAppearance title appearance for the toolbar menu, default is [R.style.SceytMenuTitleAppearance]
 * */
data class ConversationHeaderStyle(
        @ColorInt var backgroundColor: Int,
        @ColorInt var titleColor: Int,
        @ColorInt var subTitleColor: Int,
        @ColorInt var underlineColor: Int,
        var backIcon: Drawable?,
        var enableUnderline: Boolean,
        val menuStyle: Int,
        val menuTitleAppearance: Int,
) {

    companion object {
        var conversationHeaderStyleCustomizer = StyleCustomizer<ConversationHeaderStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?

    ) {

        fun build(): ConversationHeaderStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ConversationHeaderView, 0, 0)

            val backgroundColor = typedArray.getColor(R.styleable.ConversationHeaderView_sceytConvHeaderBackground,
                context.getCompatColor(SceytChatUIKit.theme.primaryColor))

            val backIcon = typedArray.getDrawable(R.styleable.ConversationHeaderView_sceytConvHeaderBackIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)

            val titleColor = typedArray.getColor(R.styleable.ConversationHeaderView_sceytConvHeaderTitleColor,
                context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

            val subTitleColor = typedArray.getColor(R.styleable.ConversationHeaderView_sceytConvHeaderSubTitleColor,
                context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

            val underlineColor = typedArray.getColor(R.styleable.ConversationHeaderView_sceytConvHeaderUnderlineColor,
                context.getCompatColor(SceytChatUIKit.theme.bordersColor))

            val enableUnderline = typedArray.getBoolean(R.styleable.ConversationHeaderView_sceytConvHeaderEnableUnderline, true)

            val menuStyle = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderToolbarMenuStyle,
                R.style.SceytPopupMenuStyle)

            val menuTitleAppearance = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderToolbarMenuTitleAppearance,
                R.style.SceytMenuTitleAppearance)

            typedArray.recycle()

            return ConversationHeaderStyle(
                backgroundColor = backgroundColor,
                titleColor = titleColor,
                subTitleColor = subTitleColor,
                underlineColor = underlineColor,
                backIcon = backIcon,
                enableUnderline = enableUnderline,
                menuStyle = menuStyle,
                menuTitleAppearance = menuTitleAppearance
            ).let(conversationHeaderStyleCustomizer::apply)
        }
    }
}