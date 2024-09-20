package com.sceyt.chatuikit.providers

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.getFirstCharIsEmoji
import com.sceyt.chatuikit.extensions.processEmojiCompat
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.customviews.AvatarView

interface ChannelDefaultAvatarProvider : Provider<SceytChannel, AvatarView.DefaultAvatar>

open class DefaultChannelDefaultAvatarProvider : ChannelDefaultAvatarProvider {
    override fun provide(from: SceytChannel): AvatarView.DefaultAvatar {
        return when {
            from.isSelf() -> {
                AvatarView.DefaultAvatar.FromDrawableRes(SceytChatUIKit.theme.notesAvatar)
            }

            from.isDirect() && from.isPeerDeleted() -> {
                AvatarView.DefaultAvatar.FromDrawableRes(SceytChatUIKit.theme.deletedUserAvatar)
            }

            else -> {
                if (from.isGroup)
                    AvatarView.DefaultAvatar.Initial(getInitialText(from.channelSubject))
                else
                    AvatarView.DefaultAvatar.FromDrawableRes(SceytChatUIKit.theme.userDefaultAvatar)
            }
        }
    }

    private fun getInitialText(title: String): CharSequence {
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