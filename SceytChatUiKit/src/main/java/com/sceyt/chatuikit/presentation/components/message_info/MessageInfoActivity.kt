package com.sceyt.chatuikit.presentation.components.message_info

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.ActivityMessageInfoBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.applySystemBarsStyle
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

open class MessageInfoActivity : AppCompatActivity() {
    protected lateinit var binding: ActivityMessageInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarsStyle()
        binding = ActivityMessageInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsetsAndWindowColor(binding.root)

        loadMessageInfoFragment()
    }

    protected open fun loadMessageInfoFragment() {
        supportFragmentManager.commit {
            replace(
                binding.frameLayout.id,
                MessageInfoFragment.newInstance(
                    messageId = intent.getLongExtra(KEY_MESSAGE_ID, 0),
                    channelId = intent.getLongExtra(KEY_CHANNEL_ID, 0),
                    messageItemStyleId = intent.getStringExtra(KEY_ITEM_STYLE_ID)
                )
            )
        }
    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        StyleRegistry.unregister(intent.getStringExtra(KEY_ITEM_STYLE_ID))
    }

    companion object {
        private const val KEY_MESSAGE_ID = "key_message_id"
        private const val KEY_CHANNEL_ID = "key_channel_id"
        private const val KEY_ITEM_STYLE_ID = "key_item_style_id"

        fun createIntent(
            context: Context,
            message: SceytMessage,
            itemStyle: MessageItemStyle,
        ): Intent = createIntent(context, message.id, message.channelId, itemStyle)

        fun createIntent(
            context: Context,
            messageId: Long,
            channelId: Long,
            itemStyle: MessageItemStyle,
        ): Intent {
            // Register style
            StyleRegistry.register(itemStyle)

            return context.createIntent<MessageInfoActivity> {
                putExtra(KEY_MESSAGE_ID, messageId)
                putExtra(KEY_CHANNEL_ID, channelId)
                putExtra(KEY_ITEM_STYLE_ID, itemStyle.styleId)
            }
        }
    }
}