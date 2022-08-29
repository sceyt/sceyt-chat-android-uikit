package com.sceyt.chat.ui.presentation.mainactivity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sceyt.chat.Types
import com.sceyt.chat.connectivity_change.NetworkMonitor
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.SceytUiKitApp
import com.sceyt.chat.ui.databinding.FragmentChannelsBinding
import com.sceyt.chat.ui.presentation.conversation.ConversationActivity
import com.sceyt.chat.ui.presentation.newchannel.NewChannelActivity
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.BaseChannelViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.events.ChannelEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelPopupClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.bindView


class ChannelsFragment : Fragment() {
    private lateinit var binding: FragmentChannelsBinding
    private val mViewModel: ChannelsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelsBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()

        binding.channelListView.setViewHolderFactory(CustomViewHolderFactory(requireContext()))

        mViewModel.bindView(binding.channelListView, viewLifecycleOwner)
        mViewModel.bindView(binding.searchView)

        /* (requireActivity().application as? SceytUiKitApp)?.sceytConnectionStatus?.observe(viewLifecycleOwner) {
             if (it == Types.ConnectState.StateConnected) {
                 mViewModel.loadChannels(0)
             }
         }*/

        binding.channelListView.setCustomChannelClickListeners(object : ChannelClickListenersImpl(binding.channelListView) {
            override fun onChannelClick(item: ChannelListItem.ChannelItem) {
                super.onChannelClick(item)
                ConversationActivity.newInstance(requireContext(), item.channel)
            }
        })

        binding.channelListView.setCustomChannelPopupClickListener(object : ChannelPopupClickListenersImpl(binding.channelListView) {
            override fun onMarkAsReadClick(channel: SceytChannel) {
                super.onMarkAsReadClick(channel)
                println("mark as read ")
            }
        })

        binding.channelListView.setChannelClickListener(ChannelClickListeners.AvatarClickListener {
            Toast.makeText(context, "avatar", Toast.LENGTH_LONG).show()
        })
    }

    private fun initViews() {
        (requireActivity().application as? SceytUiKitApp)?.sceytConnectionStatus?.observe(viewLifecycleOwner) {
            setupConnectionStatus(it)
        }

        binding.fabNewChannel.setOnClickListener {
            NewChannelActivity.launch(requireContext())
        }
    }

    class CustomViewHolderFactory(context: Context) : ChannelViewHolderFactory(context) {
        override fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {
            return super.createChannelViewHolder(parent)
            // return CustomViewHolder(SceytItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false), clickListeners)
        }
    }

    class CustomViewHolder(private val binding: SceytItemChannelBinding,
                           val listener: ChannelClickListeners.ClickListeners) : BaseChannelViewHolder(binding.root) {
        override fun bind(item: ChannelListItem, diff: ChannelItemPayloadDiff) {

            binding.root.setOnClickListener {
                listener.onChannelClick(item as ChannelListItem.ChannelItem)
            }

            binding.avatar.setOnClickListener {
                listener.onAvatarClick(item as ChannelListItem.ChannelItem)
            }
        }
    }

    private fun setupConnectionStatus(status: Types.ConnectState) {
        val title = if (!NetworkMonitor.isOnline())
            getString(R.string.waiting_for_network_title)
        else when (status) {
            Types.ConnectState.StateFailed -> getString(R.string.waiting_for_network_title)
            Types.ConnectState.StateDisconnect -> getString(R.string.waiting_for_network_title)
            Types.ConnectState.StateReconnecting,
            Types.ConnectState.StateConnecting -> getString(R.string.connecting_title)
            Types.ConnectState.StateConnected -> getString(R.string.channels)
        }
        binding.title.text = title
    }
}