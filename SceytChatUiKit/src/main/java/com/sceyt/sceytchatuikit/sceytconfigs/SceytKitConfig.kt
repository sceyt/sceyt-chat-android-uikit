package com.sceyt.sceytchatuikit.sceytconfigs

import androidx.annotation.ColorRes
import androidx.annotation.IntRange
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.BR
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.repositories.MessageTransformer

object SceytKitConfig {
    val SceytUITheme = ThemeConfig()

    @IntRange(1, 50)
    var CHANNELS_LOAD_SIZE = 20

    @IntRange(1, 50)
    var CHANNELS_MEMBERS_LOAD_SIZE = 30

    @IntRange(1, 50)
    var USERS_LOAD_SIZE = 30

    @IntRange(1, 50)
    var MESSAGES_LOAD_SIZE = 30

    @IntRange(1, 50)
    var ATTACHMENTS_LOAD_SIZE = 20

    @IntRange(1, 50)
    var REACTIONS_LOAD_SIZE = 30

    @IntRange(from = 1, to = 6)
    var MAX_SELF_REACTIONS_SIZE = 6

    @IntRange(from = 1, to = 50)
    var MAX_MULTISELECT_MESSAGES_COUNT = 30

    var enableDarkMode = true
    val isDarkMode get() = enableDarkMode && SceytUITheme.isDarkMode
    var sortChannelsBy: ChannelSortType = ChannelSortType.ByLastMsg
    var presenceStatusText = ""

    @ColorRes
    var sceytColorAccent = R.color.sceyt_color_accent

    var userNameBuilder: ((User) -> String)? = null

    var defaultReactions = arrayListOf("\uD83D\uDE0E", "\uD83D\uDE02", "\uD83D\uDC4C", "\uD83D\uDE0D", "\uD83D\uDC4D", "\uD83D\uDE0F")

    var avatarColors = arrayOf("#FF3E74", "#4F6AFF", "#FBB019", "#00CC99", "#9F35E7", "#63AFFF")

    var messageTransformer: MessageTransformer? = null

    var backgroundUploadNotificationClickData: BackgroundUploadNotificationClickData? = null

    class ThemeConfig : BaseObservable() {
        @Bindable
        var isDarkMode = false
            set(value) {
                field = value
                notifyPropertyChanged(BR.isDarkMode)
            }
    }

    enum class ChannelSortType {
        ByLastMsg,
        ByChannelCreatedAt
    }
}