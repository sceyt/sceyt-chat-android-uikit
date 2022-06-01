package com.sceyt.chat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sceyt.chat.Types
import com.sceyt.chat.ui.databinding.FragmentChannelsBinding
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.bindView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.ConversationActivity


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
        mViewModel.bindView(mBinding.searchView)

        mBinding.channelListView.setChannelListener(ChannelClickListeners.ChannelClickClickListener {
            ConversationActivity.newInstance(requireContext(), it.channel)
        })


        (requireActivity().application as SceytUiKitApp).sceytConnectionStatus.observe(viewLifecycleOwner) {
            if (it == Types.ConnectState.StateConnected) {
                mViewModel.loadChannels(0)
            }
        }
    }
}