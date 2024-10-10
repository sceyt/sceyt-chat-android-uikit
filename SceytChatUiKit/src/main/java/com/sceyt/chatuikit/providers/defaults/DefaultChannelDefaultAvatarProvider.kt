package com.sceyt.chatuikit.providers.defaults

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
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.providers.VisualProvider

open class DefaultChannelDefaultAvatarProvider : VisualProvider<SceytChannel, DefaultAvatar> {
    override fun provide(context: Context, from: SceytChannel): DefaultAvatar {
        return when {
            from.isGroup -> {
                DefaultAvatar.Initial(getInitialText(from.channelSubject))
            }

            from.isSelf -> {
                DefaultAvatar.FromDrawable(
                    context.getCompatDrawable(R.drawable.sceyt_ic_notes_with_bachgriund_layers)
                        .applyTintBackgroundLayer(
                            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor), R.id.backgroundLayer
                        ))
            }

            from.isDirect() -> {
                val peer = from.getPeer()?.user ?: SceytUser("")
                SceytChatUIKit.providers.userDefaultAvatarProvider.provide(context, peer)
            }

            else -> {
                DefaultAvatar.Initial(getInitialText(from.channelSubject))
            }
        }
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