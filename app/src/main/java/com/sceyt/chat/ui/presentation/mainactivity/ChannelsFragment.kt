package com.sceyt.chat.ui.presentation.mainactivity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.connectivity_change.NetworkMonitor
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.FragmentChannelsBinding
import com.sceyt.chat.ui.presentation.conversation.ConversationActivity
import com.sceyt.chat.ui.presentation.newchannel.NewChannelActivity
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.BaseChannelViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelPopupClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.bind
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch


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

        //binding.channelListView.setViewHolderFactory(CustomViewHolderFactory(requireContext()))

        mViewModel.bind(binding.channelListView, viewLifecycleOwner)
        mViewModel.bind(binding.searchView)

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
        setupConnectionStatus(ConnectionEventsObserver.connectionState)

        lifecycleScope.launch {
            ConnectionEventsObserver.onChangedConnectStatusFlow.distinctUntilChanged().collect {
                it.state?.let { it1 -> setupConnectionStatus(it1) }
            }
        }

        binding.fabNewChannel.setOnClickListener {
            NewChannelActivity.launch(requireContext())
        }
    }

    class CustomViewHolderFactory(context: Context) : ChannelViewHolderFactory(context) {
        override fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {

            //return super.createChannelViewHolder(parent)

            return ViewHolderWithSceytUiAndCustomLogic(SceytItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                clickListeners, getAttachDetachListener())
        }
    }

    /**This is custom view holder extended ChannelViewHolder.
     * Use this to customise your logic*/
    class ViewHolderWithSceytUiAndCustomLogic(
            binding: SceytItemChannelBinding,
            listener: ChannelClickListeners.ClickListeners,
            attachDetachListener: ((ChannelListItem?, Boolean) -> Unit)?) : ChannelViewHolder(binding, listener, attachDetachListener) {

        override fun setLastMessagedText(channel: SceytChannel, textView: TextView) {
            textView.text = "Bla Bla"
        }
    }

    /**This is custom view holder extended BaseChannelViewHolder.
     * Use this to customise your UI*/
    class CustomViewHolder(private val binding: SceytItemChannelBinding,
                           private val listener: ChannelClickListeners.ClickListeners) : BaseChannelViewHolder(binding.root) {

        override fun bind(item: ChannelListItem, diff: ChannelItemPayloadDiff) {
            super.bind(item, diff)

            binding.root.setOnClickListener {
                listener.onChannelClick(item as ChannelListItem.ChannelItem)
            }

            binding.avatar.setOnClickListener {
                listener.onAvatarClick(item as ChannelListItem.ChannelItem)
            }
        }
    }

    private fun setupConnectionStatus(state: ConnectionState) {
        val title = if (!NetworkMonitor.isOnline())
            getString(R.string.waiting_for_network_title)
        else when (state) {
            ConnectionState.Failed -> getString(R.string.connecting_title)
            ConnectionState.Disconnected -> getString(R.string.connecting_title)
            ConnectionState.Reconnecting,
            ConnectionState.Connecting -> getString(R.string.connecting_title)
            ConnectionState.Connected -> getString(R.string.channels)
        }
        binding.title.text = title
    }
}