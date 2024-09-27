package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.getFirstCharIsEmoji
import com.sceyt.chatuikit.extensions.processEmojiCompat
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.providers.VisualProvider

data object DefaultChannelDefaultAvatarProvider : VisualProvider<SceytChannel, AvatarView.DefaultAvatar> {
    override fun provide(context: Context, from: SceytChannel): AvatarView.DefaultAvatar {
        return when {
            from.isSelf() -> {
                AvatarView.DefaultAvatar.FromDrawableRes(R.drawable.sceyt_ic_notes)
            }

            from.isDirect() && from.isPeerDeleted() -> {
                AvatarView.DefaultAvatar.FromDrawableRes(R.drawable.sceyt_ic_deleted_user)
            }

            else -> {
                if (from.isGroup)
                    AvatarView.DefaultAvatar.Initial(getInitialText(from.channelSubject))
                else
                    AvatarView.DefaultAvatar.FromDrawableRes(R.drawable.sceyt_ic_default_avatar)
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