package com.sceyt.chat.ui.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.channeleventobserverservice.MessageStatusChange
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.conversation.ConversationActivity
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import com.sceyt.chat.ui.utils.binding.BindingUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelsListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), ChannelClickListeners.ClickListeners {

    private var channelsRV: ChannelsRV
    private var pageStateView: PageStateView? = null
    private var clickListeners = ChannelClickListenersImpl(this)

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
        })
    }

    internal fun setChannelsList(channels: List<ChannelListItem>) {
        channelsRV.setData(channels)
    }

    internal fun addNewChannels(channels: List<ChannelListItem>) {
        channelsRV.addNewChannels(channels)
    }

    internal fun updateLastMessage(message: SceytMessage, unreadCount: Long? = null): Boolean {
        channelsRV.getChannels()?.find { message.channelId == it.channel.id }?.let {
            it.channel.message = message
            unreadCount?.let { count ->
                it.channel.unreadCount = count
            }
            sortChannelsBy(SceytUIKitConfig.sortChannelsBy)
            return true
        }
        return false
    }

    internal fun channelCleared(channelId: Long) {
        channelsRV.getChannels()?.find { channelId == it.channel.id }?.let {
            it.channel.message = null
            it.channel.unreadCount = 0
            sortChannelsBy(SceytUIKitConfig.sortChannelsBy)
        }
    }

    internal fun updateLastMessageStatus(status: MessageStatusChange) {
        context.asAppCompatActivity()?.lifecycleScope?.launch(Dispatchers.Default) {
            channelsRV.getChannels()?.find { status.channel?.id == it.channel.id }?.let { channelItem ->
                channelItem.channel.message?.let {
                    if (it.status < status.status) {
                        it.status = status.status
                        channelItem.channel.message = it
                    }
                }
            }
        }
    }

    internal fun updateState(state: BaseViewModel.PageState) {
        pageStateView?.updateState(state, channelsRV.isEmpty())
    }

    internal fun setReachToEndListener(listener: (offset: Int) -> Unit) {
        channelsRV.setRichToEndListeners(listener)
    }

    internal fun deleteChannel(channelId: Long?) {
        channelsRV.deleteChannel(channelId ?: return)
    }

    private fun sortChannelsBy(sortBy: SceytUIKitConfig.ChannelSortType) {
        channelsRV.sortBy(sortBy)
    }

    val getChannels get() = channelsRV.getChannels()

    val getChannelsSizeFromUpdate
        get() = getChannels?.size ?: SceytUIKitConfig.CHANNELS_LOAD_SIZE

    fun setChannelListener(listener: ChannelClickListeners) {
        clickListeners.setListener(listener)
    }

    //Click listeners
    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        ConversationActivity.newInstance(context, item.channel)
    }

    override fun onChannelLongClick(item: ChannelListItem.ChannelItem) {

    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {

    }
}