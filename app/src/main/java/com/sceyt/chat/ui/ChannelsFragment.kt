package com.sceyt.chat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sceyt.chat.Types
import com.sceyt.chat.ui.databinding.FragmentChannelsBinding
import com.sceyt.chat.ui.extensions.shortToast
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.bindSearchView
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.bindView


class ChannelsFragment : Fragment() {
    private lateinit var mBinding: FragmentChannelsBinding
    private val mViewModel: ChannelsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelsBinding.inflate(inflater, container, false)
            .also { mBinding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.bindView(mBinding.channelListView, viewLifecycleOwner)
        mViewModel.bindSearchView(mBinding.searchView)

        mBinding.channelListView.setChannelListener(ChannelListeners.ChannelClickListener {
            requireActivity().shortToast(it.channel.lastMessage?.body ?: "")
        })

       /* mBinding.channelListView.setChannelListener(object :ChannelListListeners.Listeners {
            override fun onChannelClick(item: ChannelListItem.ChannelItem) {
                requireActivity().shortToast(item.channel.lastMessage?.body ?: "")
            }

            override fun onChannelLongClick() {
                requireActivity().shortToast("Long")
            }

            override fun onAvatarClick() {
                requireActivity().shortToast("Avatar")
            }
        })*/

        (requireActivity().application as SceytUiKitApp).sceytConnectionStatus.observe(viewLifecycleOwner) {
            if (it == Types.ConnectState.StateConnected) {
                mViewModel.loadChannels(0)
            }
        }
    }
}