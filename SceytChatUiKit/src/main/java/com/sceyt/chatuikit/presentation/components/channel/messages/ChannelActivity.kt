package com.sceyt.chatuikit.presentation.components.channel.messages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytActivityChannelBinding
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModelFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.bind

open class ChannelActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityChannelBinding
    private val viewModel: MessageListViewModel by viewModels(factoryProducer = { factory })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(SceytActivityChannelBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(
            statusBarColor = SceytChatUIKit.theme.colors.statusBarColor,
            navigationBarColor = SceytChatUIKit.theme.colors.primaryColor)

        viewModel.bind(binding.messagesListView, lifecycleOwner = this)
        viewModel.bind(binding.messageInputView, null, lifecycleOwner = this)
        viewModel.bind(binding.headerView, null, lifecycleOwner = this)
    }

    private val factory: MessageListViewModelFactory by lazy(LazyThreadSafetyMode.NONE) {
        MessageListViewModelFactory(requireNotNull(intent.parcelable(CHANNEL)))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val channel = intent?.parcelable<SceytChannel>(CHANNEL) ?: return
        if (channel.id == viewModel.channel.id) return
        launchActivity<ChannelActivity> {
            putExtra(CHANNEL, channel)
        }
        super.finish()
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun launch(context: Context, channel: SceytChannel) {
            context.launchActivity<ChannelActivity>(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold) {
                putExtra(CHANNEL, channel)
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
}