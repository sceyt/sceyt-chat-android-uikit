package com.sceyt.sceytchatuikit.sceytconfigs

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

object SceytKitConfig {
    val SceytUITheme = ThemeConfig()

    const val CHANNELS_LOAD_SIZE = 20
    const val CHANNELS_MEMBERS_LOAD_SIZE = 20
    const val USERS_LOAD_SIZE = 30
    const val MESSAGES_LOAD_SIZE = 30

    var enableDarkMode = true
    val isDarkMode get() = enableDarkMode && SceytUITheme.isDarkMode
    var sortChannelsBy: ChannelSortType = ChannelSortType.ByLastMsg
    var presenceStatusText = ""

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