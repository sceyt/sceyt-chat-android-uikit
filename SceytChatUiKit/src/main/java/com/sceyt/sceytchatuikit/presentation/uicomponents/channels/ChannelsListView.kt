package com.sceyt.sceytchatuikit.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.hasDiff
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.presentation.common.diff
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.root.PageStateView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.events.ChannelEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelPopupClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelPopupClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.popups.PopupMenuChannel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.genMemberBy
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.sceytconfigs.ChannelStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.utils.BindingUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChannelsListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), ChannelClickListeners.ClickListeners,
        ChannelPopupClickListeners.PopupClickListeners {

    private var channelsRV: ChannelsRV
    private var pageStateView: PageStateView? = null
    private var defaultClickListeners: ChannelClickListenersImpl
    private var clickListeners = ChannelClickListenersImpl(this)
    private var popupClickListeners = ChannelPopupClickListenersImpl(this)
    private var channelEventListener: ((ChannelEvent) -> Unit)? = null
    private val debounceHelper by lazy { DebounceHelper(300) }

    init {

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ChannelsListView)
            ChannelStyle.updateWithAttributes(a)
            a.recycle()
        }

        setBackgroundColor(context.getCompatColorByTheme(ChannelStyle.backgroundColor))
        BindingUtil.themedBackgroundColor(this, ChannelStyle.backgroundColor)

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

        defaultClickListeners = object : ChannelClickListenersImpl() {
            override fun onChannelClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelClick(item)
            }

            override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelLongClick(view, item)
            }

            override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onAvatarClick(item)
            }
        }
        channelsRV.setChannelListener(defaultClickListeners)
    }

    internal fun setChannelsList(channels: List<ChannelListItem>) {
        channelsRV.setData(channels)
    }

    internal fun addNewChannels(channels: List<ChannelListItem>) {
        channelsRV.addNewChannels(channels)
    }

    internal fun addNewChannelAndSort(channelItem: ChannelListItem.ChannelItem) {
        channelsRV.getData()?.let {
            if (it.contains(channelItem)) return
            val newData = ArrayList(it).also { items -> items.add(channelItem) }
            channelsRV.sortByAndSetNewData(SceytKitConfig.sortChannelsBy, newData)
        } ?: channelsRV.setData(arrayListOf(channelItem))

        pageStateView?.updateState(PageState.Nothing)
    }

    internal fun channelUpdated(channel: SceytChannel?): ChannelItemPayloadDiff? {
        channelsRV.getChannelIndexed(channel?.id ?: return null)?.let { pair ->
            val channelItem = pair.second
            val oldChannel = channelItem.channel.clone()
            channelItem.channel = channel
            val diff = oldChannel.diff(channel)
            channelsRV.adapter?.notifyItemChanged(pair.first, diff)
            return diff
        }
        return null
    }

    internal fun deleteChannel(channelId: Long?) {
        channelsRV.deleteChannel(channelId ?: return)
        if (channelsRV.getData().isNullOrEmpty())
            pageStateView?.updateState(PageState.StateEmpty())
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
        pageStateView?.updateState(state, channelsRV.isEmpty(), enableErrorSnackBar = false)
    }

    internal suspend fun updateUsersPresenceIfNeeded(users: List<User>) {
        try {
            users.forEach {
                channelsRV.getDirectChannelByUserIdIndexed(it.id)?.let { pair ->
                    val channel = (pair.second.channel as SceytDirectChannel)
                    val hasDiff = channel.peer?.user?.presence?.hasDiff(it.presence)
                    if (hasDiff == true) {
                        val oldChannel = channel.clone()
                        channel.peer?.user = it
                        withContext(Dispatchers.Main) {
                            channelsRV.adapter?.notifyItemChanged(pair.first, oldChannel.diff(channel))
                        }
                    }
                }
            }
        } catch (ex: ConcurrentModificationException) {
            Log.e(TAG, ex.message.toString())
        }
    }

    private fun showChannelActionsPopup(view: View, item: ChannelListItem.ChannelItem) {
        val popup = PopupMenuChannel(ContextThemeWrapper(context, ChannelStyle.popupStyle), view, channel = item.channel)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sceyt_mark_as_read -> popupClickListeners.onMarkAsReadClick(item.channel)
                R.id.sceyt_mark_as_unread -> popupClickListeners.onMarkAsUnReadClick(item.channel)
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

    /**
     * @param listener Showing that scroll is finished, and return the current offset and last
     * showing channel item.
     */
    internal fun setReachToEndListener(listener: (offset: Int, lastChannel: SceytChannel?) -> Unit) {
        channelsRV.setRichToEndListeners(listener)
    }

    /**
     * @param listener Showing that channel is currently showing in the screen or not
     */
    internal fun setChannelAttachDetachListener(listener: (ChannelListItem?, attached: Boolean) -> Unit) {
        channelsRV.setAttachDetachListeners(listener)
    }

    /**
     * @param listener From listening events connected with channel.
     */
    internal fun setChannelEvenListener(listener: (ChannelEvent) -> Unit) {
        channelEventListener = listener
    }

    internal fun getData() = channelsRV.getData()

    internal fun hideLoadingMore() {
        channelsRV.hideLoadingMore()
    }

    fun sortChannelsBy(sortBy: SceytKitConfig.ChannelSortType) {
        debounceHelper.submit { channelsRV.sortBy(sortBy) }
    }

    /**
     * @param listener Channel click listeners, to listen click events.
     */
    fun setChannelClickListener(listener: ChannelClickListeners) {
        clickListeners.setListener(listener)
    }

    /**
     * @param listener The custom channel click listeners.
     */
    fun setCustomChannelClickListeners(listener: ChannelClickListenersImpl) {
        clickListeners = listener
    }

    /**
     * User this method to set your custom popup click listeners.
     * @param listener is the custom listener.
     */
    fun setCustomChannelPopupClickListener(listener: ChannelPopupClickListenersImpl) {
        popupClickListeners = listener
    }

    /**
     * User this method to set your custom view holder factory,
     * which is extended from [ChannelViewHolderFactory].
     * @param factory custom view holder factory, extended from [ChannelViewHolderFactory].
     */
    fun setViewHolderFactory(factory: ChannelViewHolderFactory) {
        channelsRV.setViewHolderFactory(factory.also {
            it.setChannelListener(defaultClickListeners)
        })
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        channelsRV.getViewHolderFactory().setUserNameBuilder(builder)
    }

    /**
     * Returns the inner [RecyclerView] that is used to display a list of channel list items.
     * @return The inner [RecyclerView] with channels.
     */
    fun getChannelsRv(): RecyclerView = channelsRV

    // Channel Click callbacks
    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        // Need open your conversation page
    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {

    }

    override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
        showChannelActionsPopup(view, item)
    }


    // Channel Popup callbacks
    override fun onMarkAsReadClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.MarkAsRead(channel))
    }

    override fun onMarkAsUnReadClick(channel: SceytChannel) {
        channelEventListener?.invoke(ChannelEvent.MarkAsUnRead(channel))
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