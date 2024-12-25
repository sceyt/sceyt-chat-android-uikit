package com.sceyt.chatuikit.presentation.components.channel_list.channels

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytChannelListViewBinding
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_list.channels.components.ChannelsRV
import com.sceyt.chatuikit.presentation.components.channel_list.channels.data.ChannelEvent
import com.sceyt.chatuikit.presentation.components.channel_list.channels.dialogs.ChannelActionConfirmationWithDialog
import com.sceyt.chatuikit.presentation.components.channel_list.channels.dialogs.ChannelActionsDialog
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelPopupClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelPopupClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.setListener
import com.sceyt.chatuikit.presentation.components.channel_list.channels.popups.ChannelActionsPopup
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.ChannelListViewStyle

class ChannelListView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ChannelClickListeners.ClickListeners,
        ChannelPopupClickListeners.PopupClickListeners {

    private val binding: SceytChannelListViewBinding
    private var channelsRV: ChannelsRV
    private var defaultClickListeners: ChannelClickListenersImpl
    private var clickListeners: ChannelClickListeners.ClickListeners = ChannelClickListenersImpl(this)
    private var popupClickListeners: ChannelPopupClickListeners.PopupClickListeners = ChannelPopupClickListenersImpl(this)
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
        super.setPadding(0, 0, 0, 0)

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
        if (channels.isNotEmpty())
            updateStateView(state = PageState.Nothing)
    }

    internal fun addNewChannels(channels: List<ChannelListItem>) {
        channelsRV.addNewChannels(channels)
    }

    internal fun addNewChannelAndSort(order: ChannelListOrder, channelItem: ChannelListItem.ChannelItem) {
        channelsRV.getData()?.let {
            if (it.contains(channelItem)) return
            val newData = it.plus(channelItem)
            channelsRV.sortByAndSetNewData(order, newData)
        } ?: channelsRV.setData(listOf(channelItem))

        binding.pageStateView.updateState(PageState.Nothing)
    }

    internal fun channelUpdated(
            channel: SceytChannel,
            commitCallback: (() -> Unit)? = null,
    ) {
        channelsRV.updateChannel(
            predicate = { (it as? ChannelListItem.ChannelItem)?.channel?.id == channel.id },
            newItem = ChannelListItem.ChannelItem(channel),
            commitCallback = commitCallback
        )
    }

    internal fun channelUpdatedWithDiff(channel: SceytChannel, diff: ChannelDiff) {
        channelsRV.updateChannel(
            predicate = { (it as? ChannelListItem.ChannelItem)?.channel?.id == channel.id },
            newItem = ChannelListItem.ChannelItem(channel),
            payloads = diff
        )
    }

    fun replaceChannel(pendingChannelId: Long, channel: SceytChannel) {
        channelsRV.updateChannel(
            predicate = { (it as? ChannelListItem.ChannelItem)?.channel?.id == pendingChannelId },
            newItem = ChannelListItem.ChannelItem(channel)
        )
    }

    internal fun onTyping(data: ChannelTypingEventData) {
        channelsRV.updateChannel(
            predicate = { (it as? ChannelListItem.ChannelItem)?.channel?.id == data.channel.id },
            newItem = ChannelListItem.ChannelItem(data.channel.copy(typingData = data)),
            payloads = ChannelDiff.DEFAULT_FALSE.copy(typingStateChanged = true)
        )
    }

    internal fun deleteChannel(channelId: Long?, searchQuery: String) {
        channelsRV.deleteChannel(channelId ?: return, commitCallback = {
            if (channelsRV.getData().isNullOrEmpty())
                binding.pageStateView.updateState(PageState.StateEmpty(searchQuery))
        })
    }

    internal fun updateStateView(state: PageState) {
        binding.pageStateView.updateState(state, channelsRV.isEmpty(), enableErrorSnackBar = false)
    }

    private fun showChannelActionsPopup(view: View, item: ChannelListItem.ChannelItem) {
        if (style.showChannelActionAsPopup) {
            val popup = ChannelActionsPopup(ContextThemeWrapper(context, style.popupStyle), view, channel = item.channel)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.sceyt_pin_channel -> popupClickListeners.onPinClick(item.channel)
                    R.id.sceyt_unpin_channel -> popupClickListeners.onUnPinClick(item.channel)
                    R.id.sceyt_mark_as_read -> popupClickListeners.onMarkAsReadClick(item.channel)
                    R.id.sceyt_mark_as_unread -> popupClickListeners.onMarkAsUnReadClick(item.channel)
                    R.id.sceyt_mute_channel -> popupClickListeners.onMuteClick(item.channel)
                    R.id.sceyt_un_mute_channel -> popupClickListeners.onUnMuteClick(item.channel)
                    R.id.sceyt_delete_channel -> popupClickListeners.onDeleteChannelClick(item.channel)
                    R.id.sceyt_leave_channel -> popupClickListeners.onLeaveChannelClick(item.channel)
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

    fun sortChannelsBy(sortBy: ChannelListOrder) {
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
    fun setChannelClickListener(listener: ChannelClickListeners.ClickListeners) {
        clickListeners.setListener(listener)
    }

    /**
     * @param listener The custom channel click listeners.
     */
    fun setCustomChannelClickListeners(listener: ChannelClickListeners.ClickListeners) {
        clickListeners = (listener as? ChannelClickListenersImpl)?.withDefaultListeners(this)
                ?: listener
    }

    /**
     * User this method to set your custom popup click listeners.
     * @param listener is the custom listener.
     */
    fun setCustomChannelPopupClickListener(listener: ChannelPopupClickListeners.PopupClickListeners) {
        popupClickListeners = (listener as? ChannelPopupClickListenersImpl)?.withDefaultListeners(this)
                ?: listener
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

    /**
     * Returns the inner [RecyclerView] that is used to display a list of channel list items.
     * @return The inner [RecyclerView] with channels.
     */
    fun getChannelsRv() = channelsRV

    /**
     * @return The inner [PageStateView] .
     */
    fun getPageStateView() = binding.pageStateView

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        channelsRV.setPadding(left, top, right, bottom)
    }

    override fun setClipToPadding(clipToPadding: Boolean) {
        super.setClipToPadding(clipToPadding)
        try {
            channelsRV.clipToPadding = clipToPadding
        } catch (_: Exception) {
        }
    }

    // Channel Click callbacks
    override fun onChannelClick(item: ChannelListItem.ChannelItem) {
        ChannelActivity.launch(context, item.channel)
    }

    override fun onAvatarClick(item: ChannelListItem.ChannelItem) {
        clickListeners.onChannelClick(item)
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
        ChannelActionConfirmationWithDialog.confirmMuteUntilAction(context) { until ->
            channelCommandEventListener?.invoke(ChannelEvent.Mute(channel, until))
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
}