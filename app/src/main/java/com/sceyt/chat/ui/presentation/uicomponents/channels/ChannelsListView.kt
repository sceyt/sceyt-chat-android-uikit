package com.sceyt.chat.ui.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.channeleventobserverservice.MessageStatusChange
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.findIndexed
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.presentation.common.diff
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.channels.events.ChannelEvent
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelPopupClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelPopupClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.channels.popups.PopupMenuChannel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.ConversationActivity
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig.sortChannelsBy
import com.sceyt.chat.ui.utils.binding.BindingUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelsListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), ChannelClickListeners.ClickListeners,
        ChannelPopupClickListeners.PopupClickListeners {

    private var channelsRV: ChannelsRV
    private var pageStateView: PageStateView? = null
    private var clickListeners = ChannelClickListenersImpl(this)
    private var popupClickListeners = ChannelPopupClickListenersImpl(this)
    private var channelClickListeners: ChannelClickListeners.ClickListeners
    private var channelEventListener: ((ChannelEvent) -> Unit)? = null

    init {
        setBackgroundColor(context.getCompatColor(R.color.sceyt_color_bg))
        BindingUtil.themedBackgroundColor(this, R.color.sceyt_color_bg)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ChannelsListView)
            ChannelStyle.updateWithAttributes(a)
            a.recycle()
        }

        channelsRV = ChannelsRV(context)
        channelsRV.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        channelsRV.clipToPadding = clipToPadding
        setPadding(0, 0, 0, 0)

        addView(channelsRV, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        addView(PageStateView(context).also {
            pageStateView = it
            it.setLoadingStateView(ChannelStyle.loadingState)
            it.setEmptyStateView(ChannelStyle.emptyState)
            it.setEmptySearchStateView(ChannelStyle.emptySearchState)
        })

        channelsRV.setChannelListener(object : ChannelClickListeners.ClickListeners {
            override fun onChannelClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelClick(item)
            }

            override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelLongClick(view, item)
            }

            override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onAvatarClick(item)
            }
        }.also {
            channelClickListeners = it
        })
    }

    internal fun setChannelsList(channels: List<ChannelListItem>) {
        channelsRV.setData(channels)
    }

    internal fun addNewChannels(channels: List<ChannelListItem>) {
        channelsRV.addNewChannels(channels)
    }

    internal fun updateLastMessage(message: SceytMessage, unreadCount: Long? = null): Boolean {
        channelsRV.getChannels()?.findIndexed { message.channelId == it.channel.id }?.let { pair ->
            val channel = pair.second.channel
            if (message.channelId == channel.id) {
                val oldChannel = channel.clone()
                channel.message = message
                unreadCount?.let { count ->
                    channel.unreadCount = count
                }
                channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
                sortChannelsBy(sortChannelsBy)
                return true
            }
        }
        return false
    }

    internal fun updateLastMessageStatus(status: MessageStatusChange) {
        context.asAppCompatActivity()?.lifecycleScope?.launch(Dispatchers.Default) {
            channelsRV.getChannels()?.findIndexed { status.channel?.id == it.channel.id }?.let { pair ->
                val channel = pair.second.channel
                val oldChannel = channel.clone()
                channel.message?.let {
                    if (it.deliveryStatus < status.status) {
                        it.deliveryStatus = status.status
                        channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel.apply {
                            message = it
                        }))
                    }
                }
            }
        }
    }

    internal fun channelCleared(channelId: Long?) {
        channelsRV.getChannels()?.findIndexed { channelId == it.channel.id }?.let { pair ->
            val channel = pair.second.channel
            val oldChannel = channel.clone()
            channel.message = null
            channel.unreadCount = 0
            channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
            sortChannelsBy(sortChannelsBy)
        }
    }

    internal fun updateStateView(state: PageState) {
        pageStateView?.updateState(state, channelsRV.isEmpty())
    }

    internal fun setReachToEndListener(listener: (offset: Int) -> Unit) {
        channelsRV.setRichToEndListeners(listener)
    }

    internal fun deleteChannel(channelId: Long?) {
        channelsRV.deleteChannel(channelId ?: return)
    }

    internal fun updateMuteState(muted: Boolean, channelId: Long?) {
        channelsRV.updateMuteState(muted, channelId ?: return)
    }

    internal fun channelUpdated(channel: SceytChannel?) {
        channelsRV.updateChannel(channel ?: return)
    }

    internal fun setChannelEvenListener(listener: (ChannelEvent) -> Unit) {
        channelEventListener = listener
    }

    private fun sortChannelsBy(sortBy: SceytUIKitConfig.ChannelSortType) {
        channelsRV.sortBy(sortBy)
    }

    private fun showChannelActionsPopup(view: View, item: ChannelListItem.ChannelItem) {
        val popup = PopupMenuChannel(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view, isGroup = item.channel.isGroup)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sceyt_mark_as_read -> {}
                R.id.sceyt_mark_as_unread -> {}
                R.id.sceyt_clear_history -> popupClickListeners.onClearHistoryClick(item.channel)
                R.id.sceyt_leave_channel -> popupClickListeners.onLeaveChannelClick(item.channel)
                R.id.sceyt_block_channel -> popupClickListeners.onBlockChannelClick(item.channel)
                R.id.sceyt_block_user -> {}
            }
            false
        }
        popup.show()
    }

    fun setChannelClickListener(listener: ChannelClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setCustomChannelClickListeners(listeners: ChannelClickListenersImpl) {
        clickListeners = listeners
    }

    fun setCustomChannelPopupClickListener(listener: ChannelPopupClickListenersImpl) {
        popupClickListeners = listener
    }

    fun setViewHolderFactory(factory: ChannelViewHolderFactory) {
        channelsRV.setViewHolderFactory(factory.also {
            it.setChannelListener(channelClickListeners)
        })
    }

    fun getChannels() = channelsRV.getChannels()

    fun getChannelsRv() = channelsRV

    val getChannelsSizeFromUpdate
        get() = getChannels()?.size ?: SceytUIKitConfig.CHANNELS_LOAD_SIZE


    //Channel Click callbacks
    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        ConversationActivity.newInstance(context, item.channel)
    }

    override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
        showChannelActionsPopup(view, item)
    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
        ConversationActivity.newInstance(context, item.channel)
    }


    //Channel Popup callbacks
    override fun onLeaveChannelClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.LeaveChannel(channel))
    }

    override fun onClearHistoryClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.ClearHistory(channel))
    }

    override fun onBlockChannelClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.BlockChannel(channel))
    }
}