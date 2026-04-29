package com.sceyt.chat.demo

import android.content.Context
import android.content.Intent
import com.sceyt.chat.demo.presentation.CustomChannelActivity
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.SceytChatUIKitNavigator

class DemoAppNavigator : SceytChatUIKitNavigator {
    override fun resolve(destination: Destination): Destination {
        return when (destination) {
            is Destination.Channel -> customChannelDestination(destination)
            else -> super.resolve(destination)
        }
    }

    private fun customChannelDestination(destination: Destination.Channel) =
        object : Destination.Channel(destination.channel, destination.targetMessageId) {
            override fun createIntent(context: Context): Intent {
                return CustomChannelActivity.createIntent(context, channel, targetMessageId)
            }
        }
}