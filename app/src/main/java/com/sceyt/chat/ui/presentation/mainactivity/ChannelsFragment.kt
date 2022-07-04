package com.sceyt.chat.ui.presentation.mainactivity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sceyt.chat.ui.databinding.FragmentChannelsBinding
import com.sceyt.chat.ui.databinding.SceytItemChannelBinding
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseChannelViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.bindView
import com.sceyt.chat.ui.presentation.uicomponents.newchannel.NewChannelActivity


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

        //mBinding.channelListView.setViewHolderFactory(CustomViewHolderFactory(requireContext()))

        mViewModel.bindView(binding.channelListView, viewLifecycleOwner)
        mViewModel.bindView(binding.searchView)

        binding.fabNewChannel.setOnClickListener {
            NewChannelActivity.launch(requireContext())
        }

        /* (requireActivity().application as? SceytUiKitApp)?.sceytConnectionStatus?.observe(viewLifecycleOwner) {
             if (it == Types.ConnectState.StateConnected) {
                 mViewModel.loadChannels(0)
             }
         }*/

        binding.channelListView.setCustomChannelClickListeners(object : ChannelClickListenersImpl(binding.channelListView) {
            override fun onChannelClick(item: ChannelListItem.ChannelItem) {
                super.onChannelClick(item)
                println("onChannelClick")
            }
        })

        binding.channelListView.setChannelClickListener(ChannelClickListeners.AvatarClickListener {
            Toast.makeText(context, "avatar", Toast.LENGTH_LONG).show()
        })
    }

    class CustomViewHolderFactory(context: Context) : ChannelViewHolderFactory(context) {
        override fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {

            return CustomViewHolder(SceytItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false), clickListeners)
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
}