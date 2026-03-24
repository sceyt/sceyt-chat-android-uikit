package com.sceyt.chat.demo.presentation

import android.content.Context
import com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.presentation.components.startchat.StartChatActivity

class CustomStartChatActivity : StartChatActivity() {
    override fun openChannelActivity(channel: SceytChannel) {
        CustomChannelActivity.launch(this, channel)
    }

    companion object {

        fun launch(context: Context) {
            context.launchActivity<CustomStartChatActivity>(
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right,
                sceyt_anim_slide_hold
            )
        }
    }
}