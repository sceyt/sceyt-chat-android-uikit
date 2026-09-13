package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.presentation.components.channel_list.channels.data.ChannelEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * Executes the [ChannelEvent]s produced by the channel actions dialog, for screens that show
 * channel items but do not own a [ChannelsViewModel] — currently the global search Chats tab.
 *
 * [ChannelsViewModel] handles the same events, but its initializer eagerly loads the whole
 * channel list and subscribes to the channel caches, which those screens do not need.
 */
class ChannelActionsViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), SceytKoinComponent {
    private val channelInteractor: ChannelInteractor by inject()
    private val systemMessageSender: SystemMessageSender by inject()

    fun onChannelCommandEvent(event: ChannelEvent) {
        when (event) {
            is ChannelEvent.MarkAsRead -> execute("mark channel as read") {
                channelInteractor.markChannelAsRead(event.channel.id)
            }

            is ChannelEvent.MarkAsUnRead -> execute("mark channel as unread") {
                channelInteractor.markChannelAsUnRead(event.channel.id)
            }

            is ChannelEvent.ClearHistory -> execute("clear channel history") {
                channelInteractor.clearHistory(event.channel.id, event.channel.isPublic())
            }

            is ChannelEvent.LeaveChannel -> leaveChannel(event.channel)

            is ChannelEvent.DeleteChannel -> execute("delete channel") {
                channelInteractor.deleteChannel(event.channel.id)
            }

            is ChannelEvent.Mute -> execute("mute channel") {
                channelInteractor.muteChannel(event.channel.id, event.muteUntil)
            }

            is ChannelEvent.UnMute -> execute("unmute channel") {
                channelInteractor.unMuteChannel(event.channel.id)
            }

            is ChannelEvent.Pin -> execute("pin channel") {
                channelInteractor.pinChannel(event.channel.id)
            }

            is ChannelEvent.UnPin -> execute("unpin channel") {
                channelInteractor.unpinChannel(event.channel.id)
            }
        }
    }

    private fun leaveChannel(channel: SceytChannel) {
        viewModelScope.launch(ioDispatcher) {
            if (channel.isGroup) {
                runCatching {
                    systemMessageSender.sendMemberLeft(channel.id)
                }.onFailure {
                    SceytLog.e(TAG, "Failed to send member-left system message: ${it.message}")
                }
            }

            channelInteractor.leaveChannel(channel.id).logIfError("leave channel")
        }
    }

    private fun execute(action: String, block: suspend () -> SceytResponse<*>) {
        viewModelScope.launch(ioDispatcher) {
            block().logIfError(action)
        }
    }

    private fun SceytResponse<*>.logIfError(action: String) {
        if (this is SceytResponse.Error)
            SceytLog.e(TAG, "Failed to $action: ${exception?.message}")
    }

    private companion object {
        const val TAG = "ChannelActionsViewModel"
    }
}
