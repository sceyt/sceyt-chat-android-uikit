package com.sceyt.chatuikit.presentation.components.message_info

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.ActivityMessageInfoBinding
import com.sceyt.chatuikit.extensions.applyInsets
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

open class MessageInfoActivity : AppCompatActivity() {
    protected lateinit var binding: ActivityMessageInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()
        statusBarIconsColorWithBackground(
            statusBarColor = SceytChatUIKit.theme.colors.statusBarColor,
            navigationBarColor = SceytChatUIKit.theme.colors.primaryColor
        )

        loadMessageInfoFragment()
    }

    protected open fun loadMessageInfoFragment() {
        supportFragmentManager.commit {
            replace(binding.frameLayout.id, MessageInfoFragment.newInstance(
                messageId = intent.getLongExtra(KEY_MESSAGE_ID, 0),
                channelId = intent.getLongExtra(KEY_CHANNEL_ID, 0),
                messageItemStyle = messageItemStyle
            ))
        }
    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    companion object {
        private const val KEY_MESSAGE_ID = "key_message_id"
        private const val KEY_CHANNEL_ID = "key_channel_id"
        private lateinit var messageItemStyle: MessageItemStyle

        fun launch(
                context: Context,
                message: SceytMessage,
                itemStyle: MessageItemStyle
        ) {
            messageItemStyle = itemStyle
            context.launchActivity<MessageInfoActivity>(
                enterAnimResId = R.anim.sceyt_anim_slide_in_right,
                exitAnimResId = R.anim.sceyt_anim_slide_hold
            ) {
                putExtra(KEY_MESSAGE_ID, message.id)
                putExtra(KEY_CHANNEL_ID, message.channelId)
            }
        }

        fun launch(
                context: Context,
                messageId: Long,
                channelId: Long,
                itemStyle: MessageItemStyle
        ) {
            messageItemStyle = itemStyle
            context.launchActivity<MessageInfoActivity>(
                enterAnimResId = R.anim.sceyt_anim_slide_in_right,
                exitAnimResId = R.anim.sceyt_anim_slide_hold
            ) {
                putExtra(KEY_MESSAGE_ID, messageId)
                putExtra(KEY_CHANNEL_ID, channelId)
            }
        }
    }
}