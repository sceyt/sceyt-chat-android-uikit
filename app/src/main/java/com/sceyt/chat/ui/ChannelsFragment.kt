package com.sceyt.chat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sceyt.chat.Types
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.databinding.FragmentChannelsBinding
import com.sceyt.chat.ui.extensions.launchActivity
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.bindSearchView
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.bindChannelsView
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

        mViewModel.bindChannelsView(mBinding.channelListView, viewLifecycleOwner)
        mViewModel.bindSearchView(mBinding.searchView)

        mBinding.channelListView.setChannelListener(ChannelClickListeners.ChannelClickClickListener {
            requireActivity().launchActivity<ConversationActivity> {
                putExtra("channelId", it.channel.id)
                putExtra("isGroup", it.channel.channelType != ChannelTypeEnum.Direct)
            }
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