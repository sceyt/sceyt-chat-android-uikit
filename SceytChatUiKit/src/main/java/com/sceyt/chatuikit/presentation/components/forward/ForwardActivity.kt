package com.sceyt.chatuikit.presentation.components.forward

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytActivityForwardBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.forward.viewmodel.ForwardViewModel
import com.sceyt.chatuikit.presentation.components.shareable.ShareableActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class ForwardActivity : ShareableActivity() {
    protected lateinit var binding: SceytActivityForwardBinding
    protected val viewModel: ForwardViewModel by viewModels()
    protected lateinit var forwardMessages: List<SceytMessage>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(SceytActivityForwardBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground()

        getDataFromIntent()
        binding.initViews()
        binding.applyStyle()
    }

    protected open fun getDataFromIntent() {
        forwardMessages = requireNotNull(intent?.parcelableArrayList(FORWARD_MESSAGES_KEY))
    }

    protected open fun SceytActivityForwardBinding.initViews() {
        determinateBtnState()

        toolbar.setNavigationIconClickListener {
            finish()
        }

        toolbar.setQueryChangeListener(::onSearchQueryChanged)

        btnForward.setOnClickListener {
            onForwardClick(true)
        }
    }

    protected open fun SceytActivityForwardBinding.applyStyle() {
        root.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.backgroundColor))
        btnForward.backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytChatUIKit.theme.accentColor))
        toolbar.setIconsTint(SceytChatUIKit.theme.accentColor)
    }

    protected open fun sendForwardMessage(markOwnMessageAsForwarded: Boolean) {
        viewModel.sendForwardMessage(channelIds = selectedChannels.toLongArray(), markOwnMessageAsForwarded, forwardMessages)
            .onEach {
                when (it) {
                    ForwardViewModel.State.Loading -> SceytLoader.showLoading(this@ForwardActivity)
                    ForwardViewModel.State.Finish -> {
                        SceytLoader.hideLoading()
                        finishSharingAction()
                    }
                }
            }.launchIn(lifecycleScope)
    }

    protected open fun determinateBtnState() {
        with(binding.btnForward) {
            if (enableNext()) {
                alpha = 1f
                isEnabled = true
            } else {
                alpha = .5f
                isEnabled = false
            }
        }
    }

    override fun getRV(): RecyclerView? {
        return if (::binding.isInitialized)
            binding.rvChannels
        else null
    }

    override fun onChannelClick(channelItem: ChannelListItem.ChannelItem): Boolean {
        return super.onChannelClick(channelItem).also {
            determinateBtnState()
        }
    }

    @Suppress("SameParameterValue")
    protected open fun onForwardClick(markOwnMessageAsForwarded: Boolean) {
        sendForwardMessage(markOwnMessageAsForwarded)
    }

    override fun finishSharingAction() {
        super.finish()
    }

    companion object {
        const val FORWARD_MESSAGES_KEY = "FORWARD_MESSAGE_KEY"

        fun launch(context: Context, vararg message: SceytMessage) {
            context.launchActivity<ForwardActivity> {
                putParcelableArrayListExtra(FORWARD_MESSAGES_KEY, ArrayList(message.toList()))
            }
        }
    }
}