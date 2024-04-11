package com.sceyt.chatuikit.sceytconfigs

import androidx.annotation.ColorRes
import androidx.annotation.IntRange
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.transformers.MessageTransformer

object SceytKitConfig {
    @JvmField
    val SceytUITheme = ThemeConfig()

    @JvmField
    @IntRange(1, 50)
    var CHANNELS_LOAD_SIZE = 20

    @JvmField
    @IntRange(1, 50)
    var CHANNELS_MEMBERS_LOAD_SIZE = 30

    @JvmField
    @IntRange(1, 50)
    var USERS_LOAD_SIZE = 30

    @JvmField
    @IntRange(1, 50)
    var MESSAGES_LOAD_SIZE = 50

    @JvmField
    @IntRange(1, 50)
    var ATTACHMENTS_LOAD_SIZE = 20

    @JvmField
    @IntRange(1, 50)
    var REACTIONS_LOAD_SIZE = 30

    @JvmField
    @IntRange(from = 1, to = 6)
    var MAX_SELF_REACTIONS_SIZE = 6

    @JvmField
    @IntRange(from = 1, to = 50)
    var MAX_MULTISELECT_MESSAGES_COUNT = 30

    @JvmField
    var enableDarkMode = true

    @JvmField
    var sortChannelsBy: ChannelSortType = ChannelSortType.ByLastMsg

    @JvmField
    var presenceStatusText = ""

    @JvmField
    @ColorRes
    var sceytColorAccent = R.color.colorAccent

    @JvmField
    var userNameBuilder: ((User) -> String)? = null

    @JvmField
    var defaultReactions = arrayListOf("\uD83D\uDE0E", "\uD83D\uDE02", "\uD83D\uDC4C", "\uD83D\uDE0D", "\uD83D\uDC4D", "\uD83D\uDE0F")

    @JvmField
    var avatarColors = arrayOf("#4F6AFF")

    @JvmField
    var messageTransformer: MessageTransformer? = null

    @JvmField
    var backgroundUploadNotificationClickData: BackgroundUploadNotificationClickData? = null

    data class ThemeConfig(
            var isDarkMode: Boolean = false
    )

    enum class ChannelSortType {
        ByLastMsg,
        ByChannelCreatedAt
    }
}