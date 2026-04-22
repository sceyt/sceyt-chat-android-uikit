package com.sceyt.chat.demo

import android.content.Context
import com.sceyt.chat.demo.presentation.CustomChannelActivity
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.navigation.DefaultSceytChatUIKitNavigator

class DemoAppNavigator : DefaultSceytChatUIKitNavigator() {
    override fun openChannel(context: Context, channel: SceytChannel, targetMessageId: Long?) {
        CustomChannelActivity.launch(context, channel, targetMessageId)
    }
}