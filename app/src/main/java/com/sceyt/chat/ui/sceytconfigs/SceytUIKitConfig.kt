package com.sceyt.chat.ui.sceytconfigs

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

object SceytUIKitConfig {
    val SceytUITheme = ThemeConfig()

    const val CHANNELS_LOAD_SIZE = 20
    const val MESSAGES_LOAD_SIZE = 20

    val isDarkMode get() =  SceytUITheme.isDarkMode

    class ThemeConfig : BaseObservable() {
        @Bindable
        var isDarkMode = false
            set(value) {
                field = value
                notifyPropertyChanged(BR.isDarkMode)
            }
    }
}