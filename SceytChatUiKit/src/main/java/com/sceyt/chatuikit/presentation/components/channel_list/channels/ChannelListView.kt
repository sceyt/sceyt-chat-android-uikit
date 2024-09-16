package com.sceyt.chatuikit.presentation.components.channel_list.channels

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.copy
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytChannelListViewBinding
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.customviews.PageStateView
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_list.channels.dialogs.ChannelActionConfirmationWithDialog
import com.sceyt.chatuikit.presentation.components.channel_list.channels.dialogs.ChannelActionsDialog
import com.sceyt.chatuikit.presentation.components.channel_list.channels.data.ChannelEvent
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelPopupClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelPopupClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel_list.channels.popups.ChannelActionsPopup
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.config.ChannelSortType
import com.sceyt.chatuikit.presentation.components.channel_list.channels.components.ChannelsRV
import com.sceyt.chatuikit.styles.ChannelListViewStyle

class ChannelListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), ChannelClickListeners.ClickListeners,
        ChannelPopupClickListeners.PopupClickListeners {

    private val binding: SceytChannelListViewBinding
    private var channelsRV: ChannelsRV
    private var defaultClickListeners: ChannelClickListenersImpl
    private var clickListeners = ChannelClickListenersImpl(this)
    private var popupClickListeners = ChannelPopupClickListenersImpl(this)
    private var channelCommandEventListener: ((ChannelEvent) -> Unit)? = null
    private val debounceHelper by lazy { DebounceHelper(300, this) }
    private val style: ChannelListViewStyle

    init {
        binding = SceytChannelListViewBinding.inflate(LayoutInflater.from(context), this)
        style = ChannelListViewStyle.Builder(context, attrs).build()
        if (background == null)
            setBackgroundColor(style.backgroundColor)

        channelsRV = binding.channelsRV.also { it.setStyle(style) }
        channelsRV.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        channelsRV.clipToPadding = clipToPadding
        setPadding(0, 0, 0, 0)

        binding.pageStateView.apply {
            setLoadingStateView(style.loadingState)
            setEmptyStateView(style.emptyState)
            setEmptySearchStateView(style.emptySearchState)
        }

        defaultClickListeners = object : ChannelClickListenersImpl() {
            override fun onChannelClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelClick(item.copy(channel = item.channel))
            }

            override fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem) {
                clickListeners.onChannelLongClick(view, item.copy(channel = item.channel))
            }

            override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
                clickListeners.onAvatarClick(item.copy(channel = item.channel))
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

        binding.pageStateView.updateState(PageState.Nothing)
    }

    internal fun channelUpdated(channel: SceytChannel?): ChannelDiff? {
        channelsRV.getChannelIndexed(channel?.id ?: return null)?.let { (index, channelItem) ->
            val oldChannel = channelItem.channel
            channelItem.channel = channel
            val diff = oldChannel.diff(channel)
            channelsRV.adapter?.notifyItemChanged(index, diff)
            return diff
        }
        return null
    }

    internal fun channelUpdatedWithDiff(channel: SceytChannel, diff: ChannelDiff) {
        channelsRV.getChannelIndexed(channel.id)?.let { (index, channelItem) ->
            channelItem.channel = channel
            channelsRV.adapter?.notifyItemChanged(index, diff)
        }
    }

    fun replaceChannel(pendingChannelId: Long, channel: SceytChannel) {
        channelsRV.getChannelIndexed(pendingChannelId)?.let { (_, channelItem) ->
            channelItem.channel = channel
        }
    }

    internal fun onTyping(data: ChannelTypingEventData) {
        channelsRV.getChannelIndexed(data.channel.id)?.let { (index, channelItem) ->
            val oldChannel = channelItem.channel
            channelItem.channel.typingData = data
            val diff = oldChannel.diff(channelItem.channel)
            channelsRV.adapter?.notifyItemChanged(index, diff)
        }
    }

    internal fun deleteChannel(channelId: Long?, searchQuery: String) {
        channelsRV.deleteChannel(channelId ?: return)
        if (channelsRV.getData().isNullOrEmpty())
            binding.pageStateView.updateState(PageState.StateEmpty(searchQuery))
    }

    internal fun userBlocked(data: List<User>?) {
        data?.forEach { user ->
            channelsRV.getChannels()?.find {
                it.channel.isDirect() && it.channel.getPeer()?.id == user.id
            }?.let { channelItem ->
                val channel = channelItem.channel
                channelItem.channel = channel.copy(
                    members = channel.members?.map { member ->
                        if (member.id == user.id) member.copy(user = user.copy())
                        else member
                    }
                )
            }
        }
    }

    internal fun updateStateView(state: PageState) {
        binding.pageStateView.updateState(state, channelsRV.isEmpty(), enableErrorSnackBar = false)
    }

    private fun showChannelActionsPopup(view: View, item: ChannelListItem.ChannelItem) {
        if (style.showChannelActionAsPopup) {
            val popup = ChannelActionsPopup(ContextThemeWrapper(context, style.popupStyle), view, channel = item.channel)
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
        } else
            ChannelActionsDialog.newInstance(context, item.channel).also {
                it.setChooseTypeCb { action ->
                    when (action) {
                        ChannelActionsDialog.ActionsEnum.Pin -> popupClickListeners.onPinClick(item.channel)
                        ChannelActionsDialog.ActionsEnum.UnPin -> popupClickListeners.onUnPinClick(item.channel)
                        ChannelActionsDialog.ActionsEnum.MarkAsRead -> popupClickListeners.onMarkAsReadClick(item.channel)
                        ChannelActionsDialog.ActionsEnum.MarkAsUnRead -> popupClickListeners.onMarkAsUnReadClick(item.channel)
                        ChannelActionsDialog.ActionsEnum.Mute -> popupClickListeners.onMuteClick(item.channel)
                        ChannelActionsDialog.ActionsEnum.UnMute -> popupClickListeners.onUnMuteClick(item.channel)
                        ChannelActionsDialog.ActionsEnum.Leave -> popupClickListeners.onLeaveChannelClick(item.channel)
                        ChannelActionsDialog.ActionsEnum.Delete -> popupClickListeners.onDeleteChannelClick(item.channel)
                    }
                }
            }.show()
    }

    /**
     * @param listener Showing that scroll is finished, and return the current offset and last
     * showing channel item.
     */
    internal fun setReachToEndListener(listener: (offset: Int, lastChannel: SceytChannel?) -> Unit) {
        channelsRV.setReachToEndListeners(listener)
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
     * @return The inner [PageStateView] .
     */
    fun getPageStateView() = binding.pageStateView

    // Channel Click callbacks
    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        ChannelActivity.newInstance(context, item.channel)
    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
        ChannelActivity.newInstance(context, item.channel)
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