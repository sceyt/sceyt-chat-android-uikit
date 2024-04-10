package com.sceyt.sceytchatuikit.presentation.uicomponents.forward

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytActivityForwardBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.parcelableArrayList
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

        statusBarIconsColorWithBackground()

        getDataFromIntent()
        binding.initViews()
        binding.setupStyle()
    }

    private fun getDataFromIntent() {
        forwardMessages = requireNotNull(intent?.parcelableArrayList(FORWARD_MESSAGES_KEY))
    }

    private fun SceytActivityForwardBinding.initViews() {
        determinateBtnState()

        toolbar.setNavigationIconClickListener {
            finish()
        }

        toolbar.setQueryChangeListener(::onSearchQueryChanged)

        btnForward.setOnClickListener {
            onForwardClick(true)
        }
    }

    private fun SceytActivityForwardBinding.setupStyle() {
        btnForward.backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
        toolbar.setIconsTint(SceytKitConfig.sceytColorAccent)
    }

    protected fun sendForwardMessage(markOwnMessageAsForwarded: Boolean) {
        viewModel.sendForwardMessage(channelIds = selectedChannels.toLongArray(), markOwnMessageAsForwarded, forwardMessages)
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
            context.launchActivity<SceytForwardActivity> {
                putParcelableArrayListExtra(FORWARD_MESSAGES_KEY, ArrayList(message.toList()))
            }
        }
    }
}