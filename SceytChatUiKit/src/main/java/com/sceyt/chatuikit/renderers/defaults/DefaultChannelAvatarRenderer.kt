package com.sceyt.chatuikit.renderers.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.getFirstCharIsEmoji
import com.sceyt.chatuikit.extensions.processEmojiCompat
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.common.AvatarStyle

open class DefaultChannelAvatarRenderer : AvatarRenderer<SceytChannel> {

    override fun render(context: Context, from: SceytChannel, style: AvatarStyle, avatarView: AvatarView) {
        val appearanceBuilder = avatarView
            .appearanceBuilder()
            .setStyle(style)

        when {
            from.isGroup -> {
                if (from.isSelf || from.isPeerDeleted()) {
                    appearanceBuilder.setImageUrl(null)
                } else appearanceBuilder.setImageUrl(from.iconUrl)

                appearanceBuilder.setDefaultAvatar(DefaultAvatar.Initials(from.channelSubject))
            }

            from.isSelf -> {
                val notsDrawable = context.getCompatDrawable(R.drawable.sceyt_ic_notes_with_bachgriund_layers).applyTintBackgroundLayer(
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor), R.id.backgroundLayer
                )
                appearanceBuilder.setDefaultAvatar(notsDrawable)
                appearanceBuilder.setImageUrl(null)
            }

            from.isDirect() -> {
                val peer = from.getPeer()?.user ?: SceytUser("")
                val defaultAvatar = SceytChatUIKit.providers.userDefaultAvatarProvider.provide(context, peer)

                appearanceBuilder
                    .setDefaultAvatar(defaultAvatar)
                    .setImageUrl(peer.avatarURL)
            }

            else -> {
                appearanceBuilder.setDefaultAvatar(DefaultAvatar.Initials(from.channelSubject))
            }
        }
        appearanceBuilder
            .build()
            .applyToAvatar()
    }

    protected open fun getInitialText(title: String): CharSequence {
        if (title.isBlank()) return ""
        val strings = title.trim().split(" ").filter { it.isNotBlank() }
        if (strings.isEmpty()) return ""
        val data = strings[0].getFirstCharIsEmoji()
        val firstChar = data.first
        val isEmoji = data.second
        if (isEmoji)
            return firstChar.processEmojiCompat() ?: title.take(1)

        val text = if (strings.size > 1) {
            val secondChar = strings[1].getFirstCharIsEmoji().first
            "${firstChar}${secondChar}".uppercase()
        } else firstChar.toString().uppercase()

        return text.processEmojiCompat() ?: title.take(1)
    }

}