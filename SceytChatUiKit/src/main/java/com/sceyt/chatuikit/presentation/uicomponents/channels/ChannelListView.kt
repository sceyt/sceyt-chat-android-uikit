package com.sceyt.chatuikit.presentation.uicomponents.channels

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.customviews.SceytPageStateView
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.chatuikit.presentation.uicomponents.channels.dialogs.ChannelActionConfirmationWithDialog
import com.sceyt.chatuikit.presentation.uicomponents.channels.dialogs.ChatActionsDialog
import com.sceyt.chatuikit.presentation.uicomponents.channels.events.ChannelEvent
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelPopupClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelPopupClickListenersImpl
import com.sceyt.chatuikit.presentation.uicomponents.channels.popups.PopupMenuChannel
import com.sceyt.chatuikit.presentation.uicomponents.conversation.SceytConversationActivity
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.chatuikit.sceytconfigs.ChannelSortType
import com.sceyt.chatuikit.sceytstyles.ChannelListViewStyle

class ChannelListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), ChannelClickListeners.ClickListeners,
        ChannelPopupClickListeners.PopupClickListeners {

    private var channelsRV: ChannelsRV
    private var pageStateView: SceytPageStateView? = null
    private var defaultClickListeners: ChannelClickListenersImpl
    private var clickListeners = ChannelClickListenersImpl(this)
    private var popupClickListeners = ChannelPopupClickListenersImpl(this)
    private var channelCommandEventListener: ((ChannelEvent) -> Unit)? = null
    private val debounceHelper by lazy { DebounceHelper(300, this) }
    private val style: ChannelListViewStyle

    init {
        style = ChannelListViewStyle.Builder(context, attrs).build()
        if (background == null)
            setBackgroundColor(style.backgroundColor)

        channelsRV = ChannelsRV(context).also { it.setStyle(style) }
        channelsRV.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        channelsRV.clipToPadding = clipToPadding
        setPadding(0, 0, 0, 0)

        addView(channelsRV, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        if (!isInEditMode)
            addView(SceytPageStateView(context).also {
                pageStateView = it
                it.setLoadingStateView(style.loadingState)
                it.setEmptyStateView(style.emptyState)
                it.setEmptySearchStateView(style.emptySearchState)
            })

        defaultClickListeners = object : ChannelClickListenersImpl() {
            override fun onChannelClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelClick(item.copy(channel = item.channel.clone()))
            }

            override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelLongClick(view, item.copy(channel = item.channel.clone()))
            }

            override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onAvatarClick(item.copy(channel = item.channel.clone()))
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
            channelsRV.sortByAndSetNewData(SceytChatUIKit.config.sortChannelsBy, newData)
        } ?: channelsRV.setData(arrayListOf(channelItem))

        pageStateView?.updateState(PageState.Nothing)
    }

    internal fun channelUpdated(channel: SceytChannel?): ChannelDiff? {
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

    internal fun channelUpdatedWithDiff(channel: SceytChannel, diff: ChannelDiff) {
        channelsRV.getChannelIndexed(channel.id)?.let { pair ->
            val channelItem = pair.second
            channelItem.channel = channel
            channelsRV.adapter?.notifyItemChanged(pair.first, diff)
        }
    }

    fun replaceChannel(first: Long, second: SceytChannel) {
        channelsRV.getChannelIndexed(first)?.let { pair ->
            val channelItem = pair.second
            channelItem.channel = second.clone()
        }
    }

    internal fun onTyping(data: ChannelTypingEventData) {
        channelsRV.getChannelIndexed(data.channel.id)?.let { pair ->
            val channelItem = pair.second
            val oldChannel = channelItem.channel.clone()
            channelItem.channel.typingData = data
            val diff = oldChannel.diff(channelItem.channel)
            channelsRV.adapter?.notifyItemChanged(pair.first, diff)
        }
    }

    internal fun deleteChannel(channelId: Long?, searchQuery: String) {
        channelsRV.deleteChannel(channelId ?: return)
        if (channelsRV.getData().isNullOrEmpty())
            pageStateView?.updateState(PageState.StateEmpty(searchQuery))
    }

    internal fun userBlocked(data: List<User>?) {
        data?.forEach { user ->
            channelsRV.getChannels()?.find {
                it.channel.isDirect() && it.channel.getPeer()?.id == user.id
            }?.let {
                it.channel.getPeer()?.user = user
            }
        }
    }

    internal fun updateStateView(state: PageState) {
        pageStateView?.updateState(state, channelsRV.isEmpty(), enableErrorSnackBar = false)
    }

    private fun showChannelActionsPopup(view: View, item: ChannelListItem.ChannelItem) {
        if (style.showChannelActionAsPopup) {
            val popup = PopupMenuChannel(ContextThemeWrapper(context, style.popupStyle), view, channel = item.channel)
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
        } else
            ChatActionsDialog.newInstance(context, item.channel).also {
                it.setChooseTypeCb { action ->
                    when (action) {
                        ChatActionsDialog.ActionsEnum.Pin -> popupClickListeners.onPinClick(item.channel)
                        ChatActionsDialog.ActionsEnum.UnPin -> popupClickListeners.onUnPinClick(item.channel)
                        ChatActionsDialog.ActionsEnum.MarkAsRead -> popupClickListeners.onMarkAsReadClick(item.channel)
                        ChatActionsDialog.ActionsEnum.MarkAsUnRead -> popupClickListeners.onMarkAsUnReadClick(item.channel)
                        ChatActionsDialog.ActionsEnum.Mute -> popupClickListeners.onMuteClick(item.channel)
                        ChatActionsDialog.ActionsEnum.UnMute -> popupClickListeners.onUnMuteClick(item.channel)
                        ChatActionsDialog.ActionsEnum.Leave -> popupClickListeners.onLeaveChannelClick(item.channel)
                        ChatActionsDialog.ActionsEnum.Delete -> popupClickListeners.onDeleteChannelClick(item.channel)
                    }
                }
            }.show()
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
    internal fun setChannelCommandEvenListener(listener: (ChannelEvent) -> Unit) {
        channelCommandEventListener = listener
    }

    internal fun getData() = channelsRV.getData()

    internal fun hideLoadingMore() {
        channelsRV.hideLoadingMore()
    }

    fun sortChannelsBy(sortBy: ChannelSortType) {
        debounceHelper.submitForceIfNotRunning { channelsRV.sortBy(sortBy) }
    }

    /**
     * Cancel last sort channels job.
     * */
    fun cancelLastSort(): Boolean {
        return debounceHelper.cancelLastDebounce()
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
        channelsRV.setViewHolderFactory(factory.apply {
            setChannelListener(defaultClickListeners)
            setStyle(style)
        })
    }

    fun setUserNameFormatter(builder: (User) -> String) {
        channelsRV.getViewHolderFactory().setUserNameFormatter(builder)
    }

    /**
     * Returns the inner [RecyclerView] that is used to display a list of channel list items.
     * @return The inner [RecyclerView] with channels.
     */
    fun getChannelsRv() = channelsRV

    /**
     * @return The inner [SceytPageStateView] .
     */
    fun getPageStateView() = pageStateView

    // Channel Click callbacks
    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        SceytConversationActivity.newInstance(context, item.channel)
    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
        SceytConversationActivity.newInstance(context, item.channel)
    }

    override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
        if (item.channel.checkIsMemberInChannel())
            showChannelActionsPopup(view, item)
    }

    // Channel Popup callbacks
    override fun onMarkAsReadClick(channel: SceytChannel) {
        channelCommandEventListener?.invoke(ChannelEvent.MarkAsRead(channel))
    }

    override fun onMarkAsUnReadClick(channel: SceytChannel) {
        channelCommandEventListener?.invoke(ChannelEvent.MarkAsUnRead(channel))
    }

    override fun onPinClick(channel: SceytChannel) {
        channelCommandEventListener?.invoke(ChannelEvent.Pin(channel))
    }

    override fun onUnPinClick(channel: SceytChannel) {
        channelCommandEventListener?.invoke(ChannelEvent.UnPin(channel))
    }

    override fun onMuteClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmMuteUntilAction(context) {
            channelCommandEventListener?.invoke(ChannelEvent.Mute(channel))
        }
    }

    override fun onUnMuteClick(channel: SceytChannel) {
        channelCommandEventListener?.invoke(ChannelEvent.UnMute(channel))
    }

    override fun onLeaveChannelClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmLeaveAction(context, channel) {
            channelCommandEventListener?.invoke(ChannelEvent.LeaveChannel(channel))
        }
    }

    override fun onDeleteChannelClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmDeleteChatAction(context, channel) {
            channelCommandEventListener?.invoke(ChannelEvent.DeleteChannel(channel))
        }
    }

    override fun onClearHistoryClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmClearHistoryAction(context, channel) {
            channelCommandEventListener?.invoke(ChannelEvent.ClearHistory(channel))
        }
    }

    override fun onBlockChannelClick(channel: SceytChannel) {
        channelCommandEventListener?.invoke(ChannelEvent.BlockChannel(channel))
    }

    override fun onBlockUserClick(channel: SceytChannel) {
        channelCommandEventListener?.invoke(ChannelEvent.BlockUser(channel))
    }

    override fun onUnBlockUserClick(channel: SceytChannel) {
        channelCommandEventListener?.invoke(ChannelEvent.UnBlockUser(channel))
    }
}