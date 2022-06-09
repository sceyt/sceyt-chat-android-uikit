package com.sceyt.chat.ui.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.channeleventobserverservice.MessageStatusChange
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.findIndexed
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.presentation.common.diff
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.conversation.ConversationActivity
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig.sortChannelsBy
import com.sceyt.chat.ui.utils.binding.BindingUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelsListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), ChannelClickListeners.ClickListeners {

    private var channelsRV: ChannelsRV
    private var pageStateView: PageStateView? = null
    private var clickListeners = ChannelClickListenersImpl(this)
    private var channelClickListeners: ChannelClickListeners.ClickListeners

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

            override fun onChannelLongClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelLongClick(item)
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
                    if (it.status < status.status) {
                        it.status = status.status
                        channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel.apply {
                            message = it
                        }))
                    }
                }
            }
        }
    }

    internal fun channelCleared(channelId: Long) {
        channelsRV.getChannels()?.findIndexed { channelId == it.channel.id }?.let { pair ->
            val channel = pair.second.channel
            val oldChannel = channel.clone()
            channel.message = null
            channel.unreadCount = 0
            channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
            sortChannelsBy(sortChannelsBy)
        }
    }

    internal fun updateStateView(state: BaseViewModel.PageState) {
        if (state.isEmpty && !channelsRV.isEmpty())
            return
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

    private fun sortChannelsBy(sortBy: SceytUIKitConfig.ChannelSortType) {
        channelsRV.sortBy(sortBy)
    }

    fun setChannelClickListener(listener: ChannelClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setCustomChannelClickListeners(listeners: ChannelClickListenersImpl) {
        clickListeners = listeners
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

    //Click listeners
    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        ConversationActivity.newInstance(context, item.channel)
    }

    override fun onChannelLongClick(item: ChannelListItem.ChannelItem) {

    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
        ConversationActivity.newInstance(context, item.channel)
    }
}