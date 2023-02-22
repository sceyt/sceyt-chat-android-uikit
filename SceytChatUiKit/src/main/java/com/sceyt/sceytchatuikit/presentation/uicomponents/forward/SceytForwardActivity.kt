package com.sceyt.sceytchatuikit.presentation.uicomponents.forward

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytActivityForwardBinding
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.common.SceytLoader
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.forward.viewmodel.ForwardViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.sharebaleactivity.SceytShareableActivity
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class SceytForwardActivity : SceytShareableActivity() {
    private lateinit var binding: SceytActivityForwardBinding
    protected val viewModel: ForwardViewModel by viewModels()
    private lateinit var forwardMessages: List<SceytMessage>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(SceytActivityForwardBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode)

        getDataFromIntent()
        binding.initViews()
    }

    private fun getDataFromIntent() {
        forwardMessages = requireNotNull(intent?.getParcelableArrayListExtra(FORWARD_MESSAGES_KEY))
    }

    private fun SceytActivityForwardBinding.initViews() {
        determinateBtnState()

        toolbar.setNavigationIconClickListener {
            onBackPressed()
        }

        toolbar.setQueryChangeListener(::onSearchQueryChanged)

        btnForward.setOnClickListener {
            onForwardClick()
        }
    }

    protected fun sendForwardMessage() {
        viewModel.sendForwardMessage(channelIds = selectedChannels.toLongArray(), forwardMessages)
            .onEach {
                when (it) {
                    ForwardViewModel.State.Loading -> SceytLoader.showLoading(this@SceytForwardActivity)
                    ForwardViewModel.State.Finish -> {
                        SceytLoader.hideLoading()
                        finishSharingAction()
                    }
                }
            }.launchIn(lifecycleScope)
    }

    protected fun determinateBtnState() {
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

    override fun getRV() = binding.rvChannels

    override fun onChannelClick(channelItem: ChannelListItem.ChannelItem): Boolean {
        return super.onChannelClick(channelItem).also {
            determinateBtnState()
        }
    }

    protected open fun onForwardClick() {
        sendForwardMessage()
    }

    override fun finishSharingAction() {
        super.finish()
    }

    companion object {
        const val FORWARD_MESSAGES_KEY = "FORWARD_MESSAGE_KEY"

        fun launch(context: Context, vararg message: SceytMessage) {
            context.launchActivity<SceytForwardActivity> {
                putParcelableArrayListExtra(FORWARD_MESSAGES_KEY, ArrayList(message.toList()))
            }
        }
    }
}