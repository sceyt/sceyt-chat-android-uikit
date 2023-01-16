package com.sceyt.sceytchatuikit.sceytconfigs

import androidx.annotation.ColorRes
import androidx.annotation.IntDef
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.BR

object SceytKitConfig {
    val SceytUITheme = ThemeConfig()

    var CHANNELS_LOAD_SIZE = 20
    var CHANNELS_MEMBERS_LOAD_SIZE = 20
    var USERS_LOAD_SIZE = 30
    var MESSAGES_LOAD_SIZE = 30
    var ATTACHMENTS_LOAD_SIZE = 30

    var enableDarkMode = true
    val isDarkMode get() = enableDarkMode && SceytUITheme.isDarkMode
    var sortChannelsBy: ChannelSortType = ChannelSortType.ByLastMsg
    var presenceStatusText = ""

    @ColorRes
    var sceytColorAccent = R.color.sceyt_color_accent

    var userNameBuilder: ((User) -> String)? = null

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