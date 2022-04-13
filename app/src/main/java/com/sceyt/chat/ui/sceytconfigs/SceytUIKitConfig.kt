package com.sceyt.chat.ui.sceytconfigs

import android.app.Application

object SceytUIKitConfig {
    const val CHANNELS_LOAD_SIZE = 20

    private lateinit var app: Application

    fun initApp(application: Application) {
        app = application
    }

    private lateinit var mChannelStyle: ChannelStyle

    fun getChannelsListStyle(): ChannelStyle {
        return if (::mChannelStyle.isInitialized)
            mChannelStyle
        else ChannelStyle(app).also { mChannelStyle = it }
    }
}