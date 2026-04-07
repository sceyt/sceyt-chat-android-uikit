package com.sceyt.chatuikit.presentation.components.channel.messages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.toIntentPayload
import com.sceyt.chatuikit.databinding.SceytActivityChannelBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModelFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.LoadKeyType
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.bind

open class ChannelActivity : AppCompatActivity() {
    protected lateinit var binding: SceytActivityChannelBinding
    protected val viewModel: MessageListViewModel by viewModels(factoryProducer = { factory })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(
            SceytActivityChannelBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        applyInsetsAndWindowColor(binding.root)
        statusBarIconsColorWithBackground()

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
        launchActivity<ChannelActivity> {
            putExtra(CHANNEL, channel.toIntentPayload())
            targetMessageId?.let { putExtra(TARGET_MESSAGE_ID, it) }
        }
        super.finish()
    }

    companion object {
        const val CHANNEL = "CHANNEL"
        const val TARGET_MESSAGE_ID = "TARGET_MESSAGE_ID"

        fun launch(context: Context, channel: SceytChannel, targetMessageId: Long? = null) {
            context.launchActivity<ChannelActivity>(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold) {
                putExtra(CHANNEL, channel.toIntentPayload())
                targetMessageId?.let { putExtra(TARGET_MESSAGE_ID, it) }
            }
        }
    }

    override fun finish() {
        if (isTaskRoot) {
            val launcher = packageManager.getLaunchIntentForPackage(packageName)
            launcher?.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            startActivity(launcher)
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
