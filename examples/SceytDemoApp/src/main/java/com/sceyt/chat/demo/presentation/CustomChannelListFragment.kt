package com.sceyt.chat.demo.presentation

import android.os.Bundle
import android.view.View
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListFragment
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl

class CustomChannelListFragment : ChannelListFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.channelListView.setCustomChannelClickListeners(object :
            ChannelClickListenersImpl() {
            override fun onChannelClick(
                view: View,
                item: ChannelListItem.ChannelItem
            ) {
                CustomChannelActivity.launch(requireContext(), item.channel)
            }

            override fun onAvatarClick(
                view: View,
                item: ChannelListItem.ChannelItem
            ) {
                CustomChannelActivity.launch(requireContext(), item.channel)
            }
        })
    }

    override fun openStartChatActivity() {
        CustomStartChatActivity.launch(requireContext())
    }
}