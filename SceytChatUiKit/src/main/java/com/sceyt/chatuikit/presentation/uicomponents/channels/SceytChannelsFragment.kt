package com.sceyt.chatuikit.presentation.uicomponents.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.connectivity_change.NetworkMonitor
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.chatuikit.databinding.SceytFragmentChannelsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.setMargins
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.chatuikit.presentation.uicomponents.channels.viewmodels.bind
import com.sceyt.chatuikit.presentation.uicomponents.startchat.SceytStartChatActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SceytChannelsFragment : Fragment() {
    private lateinit var binding: SceytFragmentChannelsBinding
    private val mViewModel: ChannelsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelsBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        binding.applyStyle()

        mViewModel.bind(binding.channelListView, viewLifecycleOwner)
        mViewModel.bind(binding.searchView)

        binding.searchView.post {
            binding.channelListView.getPageStateView().setMargins(bottom = binding.searchView.height)
        }
    }

    private fun initViews() {
        setupConnectionStatus(ConnectionEventsObserver.connectionState)

        lifecycleScope.launch {
            ConnectionEventsObserver.onChangedConnectStatusFlow.distinctUntilChanged().collect {
                it.state?.let { it1 -> setupConnectionStatus(it1) }
            }
        }

        binding.fabNewChannel.setOnClickListener {
            SceytStartChatActivity.launch(requireContext())
        }
    }

    private fun setupConnectionStatus(state: ConnectionState) {
        val title = if (!NetworkMonitor.isOnline())
            getString(R.string.sceyt_waiting_for_network_title)
        else when (state) {
            ConnectionState.Failed -> getString(R.string.sceyt_connecting_title)
            ConnectionState.Disconnected -> getString(R.string.sceyt_connecting_title)
            ConnectionState.Reconnecting,
            ConnectionState.Connecting -> getString(R.string.sceyt_connecting_title)

            ConnectionState.Connected -> getString(R.string.sceyt_chats)
        }
        binding.title.text = title
    }

    private fun SceytFragmentChannelsBinding.applyStyle() {
        layoutToolbar.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.primaryColor))
        searchAppBarLayout.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColor))
        title.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        underline.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.borderColor))
        fabNewChannel.setBackgroundTintColorRes(SceytChatUIKit.theme.accentColor)
    }
}