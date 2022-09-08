package com.sceyt.sceytchatuikit.presentation.uicomponents.channels

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.shared.utils.BindingUtil
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.extensions.asAppCompatActivity
import com.sceyt.sceytchatuikit.extensions.findIndexed
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.root.PageStateView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.events.ChannelEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelPopupClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelPopupClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.popups.PopupMenuChannel
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.presentation.common.diff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.genMemberBy
import com.sceyt.sceytchatuikit.sceytconfigs.ChannelStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    internal fun updateChannelsWithServerData(data: List<ChannelListItem>, offset: Int, lifecycleOwner: LifecycleOwner) {
        val channels = ArrayList(channelsRV.getData() as? ArrayList ?: arrayListOf())
        if (data.isEmpty() && offset == 0) {
            channelsRV.setData(data)
            return
        }
        if (channels.isEmpty()) {
            channels.addAll(data)
            channelsRV.setData(channels)
        } else {
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                var dataHasLoadingItem = false
                // Update UI channels if exist, or add new channels
                data.forEach { dataItem ->
                    if (!dataHasLoadingItem)
                        dataHasLoadingItem = dataItem is ChannelListItem.LoadingMoreItem

                    channels.findIndexed {
                        it is ChannelListItem.ChannelItem &&
                                dataItem is ChannelListItem.ChannelItem &&
                                it.channel.id == dataItem.channel.id
                                || it is ChannelListItem.LoadingMoreItem
                    }?.let {
                        channels[it.first] = dataItem
                    } ?: run {
                        channels.add(dataItem)
                    }
                }
                if (!dataHasLoadingItem)
                    channels.remove(ChannelListItem.LoadingMoreItem)

                withContext(Dispatchers.Main) {
                    channelsRV.sortByAndSetNewData(SceytUIKitConfig.sortChannelsBy, channels)
                }
            }
        }
    }

    internal fun addNewChannels(channels: List<ChannelListItem>) {
        channelsRV.addNewChannels(channels)
    }

    internal fun updateLastMessage(message: SceytMessage, edited: Boolean, unreadCount: Long? = null): Boolean {
        channelsRV.getChannelIndexed(message.channelId)?.let { pair ->
            val channel = pair.second.channel
            if (message.channelId == channel.id) {
                val oldChannel = channel.clone()
                if (!edited || channel.message?.id == message.id)
                    channel.message = message

                unreadCount?.let { count ->
                    channel.unreadCount = count
                }
                channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
                sortChannelsBy(SceytUIKitConfig.sortChannelsBy)
                return true
            }
        }
        return false
    }

    internal fun updateLastMessageStatus(status: MessageStatusChangeData) {
        context.asAppCompatActivity().lifecycleScope.launch(Dispatchers.Default) {
            val channelId = status.channelId ?: return@launch
            channelsRV.getChannelIndexed(channelId)?.let { pair ->
                val channel = pair.second.channel
                channel.message?.let {
                    if (status.messageIds.contains(it.id)) {
                        val oldChannel = channel.clone()
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
    }

    internal fun updateOutgoingLastMessageStatus(status: MessageStatusChangeData) {
        context.asAppCompatActivity().lifecycleScope.launch(Dispatchers.Default) {
            val channelId = status.channelId ?: return@launch
            channelsRV.getChannelIndexed(channelId)?.let { pair ->
                val channel = pair.second.channel
                channel.message?.let {
                    if (status.messageIds.contains(it.tid)) {
                        val oldChannel = channel.clone()
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
    }

    internal fun channelCleared(channelId: Long?) {
        channelsRV.getChannelIndexed(channelId ?: return)?.let { pair ->
            val channel = pair.second.channel
            val oldChannel = channel.clone()
            channel.message = null
            channel.unreadCount = 0
            channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
            sortChannelsBy(SceytUIKitConfig.sortChannelsBy)
        }
    }

    internal fun updateMuteState(muted: Boolean, channelId: Long?) {
        channelsRV.getChannelIndexed(channelId ?: return)?.let { pair ->
            val channel = pair.second.channel
            val oldChannel = channel.clone()
            channel.muted = muted
            channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
        }
    }

    internal fun channelUpdated(channel: SceytChannel?) {
        channelsRV.getChannelIndexed(channel?.id ?: return)?.let { pair ->
            val channelItem = pair.second
            val oldChannel = channelItem.channel.clone()
            channelItem.channel = channel
            channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
        }
    }

    internal fun deleteChannel(channelId: Long?) {
        channelsRV.deleteChannel(channelId ?: return)
    }

    internal fun markedUsRead(data: MessageListMarker?) {
        val channelId = data?.channelId ?: return
        channelsRV.getChannelIndexed(channelId)?.let { pair ->
            val channel = pair.second.channel
            val oldChannel = channel.clone()
            channel.unreadCount = 0
            channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
        }
    }

    internal fun markedChannelAsRead(channelId: Long?) {
        channelsRV.getChannelIndexed(channelId ?: return)?.let { pair ->
            val channel = pair.second.channel
            val oldChannel = channel.clone()
            channel.unreadCount = 0
            channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
        }
    }

    internal fun userBlocked(data: List<User>?) {
        data?.forEach { user ->
            channelsRV.getChannels()?.find {
                it.channel is SceytDirectChannel && (it.channel as SceytDirectChannel).peer?.id == user.id
            }?.let {
                (it.channel as SceytDirectChannel).peer = genMemberBy(user).toSceytMember()
            }
        }
    }

    internal fun updateStateView(state: PageState) {
        pageStateView?.updateState(state, channelsRV.isEmpty())
    }

    private fun sortChannelsBy(sortBy: SceytUIKitConfig.ChannelSortType) {
        channelsRV.sortBy(sortBy)
    }

    private fun showChannelActionsPopup(view: View, item: ChannelListItem.ChannelItem) {
        val popup = PopupMenuChannel(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view, channel = item.channel)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sceyt_mark_as_read -> popupClickListeners.onMarkAsReadClick(item.channel)
                R.id.sceyt_clear_history -> popupClickListeners.onClearHistoryClick(item.channel)
                R.id.sceyt_leave_channel -> popupClickListeners.onLeaveChannelClick(item.channel)
                R.id.sceyt_block_channel -> popupClickListeners.onBlockChannelClick(item.channel)
                R.id.sceyt_block_user -> popupClickListeners.onBlockUserClick(item.channel)
                R.id.sceyt_un_block_user -> popupClickListeners.onUnBlockUserClick(item.channel)
            }
            false
        }
        popup.show()
    }

    internal fun setReachToEndListener(listener: (offset: Int, lastChannel: SceytChannel?) -> Unit) {
        channelsRV.setRichToEndListeners(listener)
    }

    internal fun setChannelEvenListener(listener: (ChannelEvent) -> Unit) {
        channelEventListener = listener
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

    fun getChannelsRv() = channelsRV


    //Channel Click callbacks
    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        item.channel.unreadCount = 0
        Handler(Looper.getMainLooper()).postDelayed({
            channelUpdated(item.channel)
        }, 300)
    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
        onChannelClick(item)
    }

    override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
        showChannelActionsPopup(view, item)
    }


    //Channel Popup callbacks
    override fun onMarkAsReadClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.MarkAsRead(channel))
    }

    override fun onLeaveChannelClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.LeaveChannel(channel))
    }

    override fun onClearHistoryClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.ClearHistory(channel))
    }

    override fun onBlockChannelClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.BlockChannel(channel))
    }

    override fun onBlockUserClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.BlockUser(channel))
    }

    override fun onUnBlockUserClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.UnBlockUser(channel))
    }
}