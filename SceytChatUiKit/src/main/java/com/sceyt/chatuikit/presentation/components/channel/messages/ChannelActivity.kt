package com.sceyt.chatuikit.presentation.components.channel.messages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.toIntentPayload
import com.sceyt.chatuikit.databinding.SceytActivityChannelBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.applySystemBarsStyle
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.navigate
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModelFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.LoadKeyType
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.bind

open class ChannelActivity : AppCompatActivity() {
    protected lateinit var binding: SceytActivityChannelBinding
    protected val viewModel: MessageListViewModel by viewModels(factoryProducer = { factory })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarsStyle()
        setContentView(
            SceytActivityChannelBinding.inflate(layoutInflater)
                .also { binding = it }
                .root)

        applyInsetsAndWindowColor(binding.root)

        viewModel.bind(binding.messagesListView, lifecycleOwner = this)
        viewModel.bind(binding.messageInputView, null, lifecycleOwner = this)
        viewModel.bind(binding.headerView, null, lifecycleOwner = this)
    }

    private val factory: MessageListViewModelFactory by lazy(LazyThreadSafetyMode.NONE) {
        MessageListViewModelFactory(
            channel = requireNotNull(intent.parcelable(CHANNEL)),
            targetMessageId = intent.getTargetMessageId()
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val channel = intent.parcelable<SceytChannel>(CHANNEL) ?: return
        val targetMessageId = intent.getTargetMessageId()
        if (channel.id == viewModel.channel.id) {
            targetMessageId?.let {
                viewModel.loadNearMessages(
                    messageId = it,
                    loadKey = LoadKeyData(
                        key = LoadKeyType.ScrollToMessageBy.longValue,
                        value = it
                    ),
                    ignoreServer = false
                )
            }
            return
        }
        SceytChatUIKit.navigator.navigate(
            this,
            Destination.Channel(channel, targetMessageId)
        )
        super.finish()
    }

    companion object {
        const val CHANNEL = "CHANNEL"
        const val TARGET_MESSAGE_ID = "TARGET_MESSAGE_ID"

        fun createIntent(
            context: Context,
            channel: SceytChannel,
            targetMessageId: Long? = null
        ): Intent {
            return context.createIntent<ChannelActivity> {
                putExtra(CHANNEL, channel.toIntentPayload())
                targetMessageId?.let { putExtra(TARGET_MESSAGE_ID, it) }
            }
        }
    }

    override fun finish() {
        if (isTaskRoot) {
            SceytChatUIKit.navigator.navigate(
                context = this,
                destination = Destination.AppRoot(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            )
            super.finish()
            return
        }
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    private fun Intent.getTargetMessageId(): Long? {
        return if (hasExtra(TARGET_MESSAGE_ID)) getLongExtra(TARGET_MESSAGE_ID, 0L) else null
    }
}
