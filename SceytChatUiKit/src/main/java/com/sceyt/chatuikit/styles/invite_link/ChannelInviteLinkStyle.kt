package com.sceyt.chatuikit.styles.invite_link

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.SwitchStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildInviteLinkTextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildInviteLinkTitleTextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildLinkPreviewBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildOptionsTextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildShowPreviewMessagesSubtitleTextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildShowPreviewMessagesSwitchStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildToolbarStyle

data class ChannelInviteLinkStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val dividerColor: Int,
        val toolbarTitle: String,
        val inviteLinkTitle: String,
        val showPreviewMessagesTitle: String,
        val showPreviewMessagesDescription: String,
        val shareTitle: String,
        val resetLinkTitle: String,
        val openQrTitle: String,
        val copyLinkIcon: Drawable?,
        val shareIcon: Drawable?,
        val resetLinkIcon: Drawable?,
        val openQrIcon: Drawable?,
        val toolbarStyle: ToolbarStyle,
        val inviteLinkTitleTextStyle: TextStyle,
        val inviteLinkTextStyle: TextStyle,
        val showPreviewMessagesSwitchStyle: SwitchStyle,
        val showPreviewMessagesSubtitleTextStyle: TextStyle,
        val optionsTextStyle: TextStyle,
        val linkPreviewBackgroundStyle: BackgroundStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInviteLinkStyle> { _, style -> style }
    }

    class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): ChannelInviteLinkStyle {
            context.obtainStyledAttributes(attrs, R.styleable.InviteLink).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
                val dividerColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
                val toolbarTitle = context.getString(R.string.sceyt_invite_link)
                val inviteLinkTitle = context.getString(R.string.sceyt_invite_link)
                val showPreviewMessagesTitle = context.getString(R.string.sceyt_show_previous_messages)
                val showPreviewMessagesDescription = context.getString(R.string.sceyt_show_previous_messages_desc)
                val shareTitle = context.getString(R.string.sceyt_share)
                val resetLinkTitle = context.getString(R.string.sceyt_reset_link)
                val openQrTitle = context.getString(R.string.sceyt_open_qr_code)

                val copyLinkIcon = context.getCompatDrawable(R.drawable.sceyt_ic_copy).applyTint(
                    tintColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                )

                val shareIcon = context.getCompatDrawable(R.drawable.sceyt_ic_share)?.applyTint(
                    tintColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                )

                val resetLinkIcon = context.getCompatDrawable(R.drawable.sceyt_ic_refresh)?.applyTint(
                    tintColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                )

                val openQrIcon = context.getCompatDrawable(R.drawable.sceyt_ic_qr)?.applyTint(
                    tintColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                )

                return ChannelInviteLinkStyle(
                    backgroundColor = backgroundColor,
                    dividerColor = dividerColor,
                    toolbarTitle = toolbarTitle,
                    inviteLinkTitle = inviteLinkTitle,
                    showPreviewMessagesTitle = showPreviewMessagesTitle,
                    showPreviewMessagesDescription = showPreviewMessagesDescription,
                    shareTitle = shareTitle,
                    resetLinkTitle = resetLinkTitle,
                    openQrTitle = openQrTitle,
                    copyLinkIcon = copyLinkIcon,
                    shareIcon = shareIcon,
                    resetLinkIcon = resetLinkIcon,
                    openQrIcon = openQrIcon,
                    toolbarStyle = buildToolbarStyle(array),
                    inviteLinkTitleTextStyle = buildInviteLinkTitleTextStyle(array),
                    inviteLinkTextStyle = buildInviteLinkTextStyle(array),
                    showPreviewMessagesSwitchStyle = buildShowPreviewMessagesSwitchStyle(array),
                    showPreviewMessagesSubtitleTextStyle = buildShowPreviewMessagesSubtitleTextStyle(array),
                    optionsTextStyle = buildOptionsTextStyle(array),
                    linkPreviewBackgroundStyle = buildLinkPreviewBackgroundStyle(array)
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}