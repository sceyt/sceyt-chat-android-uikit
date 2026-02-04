package com.sceyt.chat.demo.presentation

import android.content.Context
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.sceyt.chat.demo.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity

class CustomChannelActivity : ChannelActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.headerView.setToolbarMenu()
    }

    private fun MessagesListHeaderView.setToolbarMenu() {
        setToolbarMenu(R.menu.menu_conversation, Toolbar.OnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_audio_call -> makeCall(false)
                R.id.action_video_call -> makeCall(true)
            }
            return@OnMenuItemClickListener true
        })
    }

    private fun makeCall(isVideo: Boolean) {

    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun launch(context: Context, channel: SceytChannel) {
            context.launchActivity<CustomChannelActivity>(
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right,
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold
            ) {
                putExtra(CHANNEL, channel)
            }
        }
    }
}