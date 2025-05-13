package com.sceyt.chatuikit.presentation.components.channel_list.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.databinding.SceytFragmentChannelsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.setMargins
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelsViewModel
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelsViewModelFactory
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.bind
import com.sceyt.chatuikit.presentation.components.startchat.StartChatActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ChannelListFragment : Fragment() {
    private lateinit var binding: SceytFragmentChannelsBinding
    private val viewModel: ChannelsViewModel by viewModels(factoryProducer = {
        ChannelsViewModelFactory()
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelsBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        binding.applyStyle()

        viewModel.bind(binding.channelListView, viewLifecycleOwner)
        viewModel.bind(binding.searchView)

        binding.searchView.post {
            binding.channelListView.getPageStateView().setMargins(bottom = binding.searchView.height)
        }
    }

    private fun initViews() {
        setupConnectionStatus(ConnectionEventManager.connectionState)

        lifecycleScope.launch {
            ConnectionEventManager.onChangedConnectStatusFlow.distinctUntilChanged().collect {
                it.state?.let { it1 -> setupConnectionStatus(it1) }
            }
        }

        binding.fabNewChannel.setOnClickListener {
            StartChatActivity.launch(requireContext())
        }
    }

    private fun setupConnectionStatus(state: ConnectionState) {
        if (state == ConnectionState.Connected) {
            binding.title.text = getString(R.string.sceyt_chats)
            return
        }

        binding.title.text = SceytChatUIKit.formatters.connectionStateTitleFormatter.format(
            requireContext(),
            state
        )
    }

    private fun SceytFragmentChannelsBinding.applyStyle() {
        layoutToolbar.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.colors.primaryColor))
        searchAppBarLayout.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))
        title.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
        underline.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.colors.borderColor))
        fabNewChannel.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
        fabNewChannel.setTintColorRes(SceytChatUIKit.theme.colors.onPrimaryColor)
    }
}