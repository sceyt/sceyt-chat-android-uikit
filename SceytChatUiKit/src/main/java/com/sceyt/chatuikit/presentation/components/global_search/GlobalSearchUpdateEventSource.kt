package com.sceyt.chatuikit.presentation.components.global_search

import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn

sealed class GlobalSearchUpdateEvent {
    data class ChannelUpdated(val channel: SceytChannel) : GlobalSearchUpdateEvent()
    data class ChannelsDeleted(val ids: List<Long>) : GlobalSearchUpdateEvent()
    data class MessageUpdated(val message: SceytMessage) : GlobalSearchUpdateEvent()

    /**
     * Returns true if this event should trigger a refresh key update.
     * This is needed when item-level changes are not reflected in state equality,
     * ensuring StateFlow emits a new value and UI recomposes.
     * */
    val shouldUpdateRefreshKey: Boolean
        get() = when (this) {
            is ChannelUpdated -> true
            is ChannelsDeleted -> false
            is MessageUpdated -> message.state == MessageState.Edited
        }
}

class GlobalSearchUpdateEventSource(scope: CoroutineScope) {

    val updatesFlow: SharedFlow<GlobalSearchUpdateEvent> = merge(
        ChannelsCache.channelUpdatedFlow.map {
            GlobalSearchUpdateEvent.ChannelUpdated(it.channel)
        },
        ChannelsCache.channelsDeletedFlow.map {
            GlobalSearchUpdateEvent.ChannelsDeleted(it)
        },
        MessageEventManager.onMessageEditedOrDeletedFlow.map {
            GlobalSearchUpdateEvent.MessageUpdated(it)
        }
    ).shareIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        replay = 0
    )
}

/**
 * Applies a [GlobalSearchUpdateEvent] to a list of items for the Chats and Channels tabs.
 * Handles channel metadata updates, channel deletions, message edits, and message deletions.
 */
fun List<GlobalSearchListItem>.applyChannelMessageUpdateEvent(
    event: GlobalSearchUpdateEvent,
): List<GlobalSearchListItem> = when (event) {
    is GlobalSearchUpdateEvent.ChannelUpdated -> map { item ->
        if (item is GlobalSearchListItem.ChannelItem && item.channel.id == event.channel.id)
            item.copy(channel = event.channel)
        else item
    }

    is GlobalSearchUpdateEvent.ChannelsDeleted -> filter { item ->
        item !is GlobalSearchListItem.ChannelItem || item.channel.id !in event.ids
    }.removeEmptySectionHeaders()

    is GlobalSearchUpdateEvent.MessageUpdated -> {
        val isDeleted = event.message.state == MessageState.Deleted
                || event.message.state == MessageState.DeletedHard
        if (isDeleted) {
            filter {
                it !is GlobalSearchListItem.MessageItem || it.result.message.id != event.message.id
            }.removeEmptySectionHeaders()
        } else {
            map { item ->
                if (item is GlobalSearchListItem.MessageItem && item.result.message.id == event.message.id)
                    item.copy(result = item.result.copy(message = event.message))
                else item
            }
        }
    }
}

/**
 * Applies a [GlobalSearchUpdateEvent] to a list of items for the attachment tabs
 * (Media, Files, Voice, Links). Only handles deletions — channel renames and message
 * edits do not affect attachment items visually.
 */
fun List<GlobalSearchListItem>.applyAttachmentUpdateEvent(
    event: GlobalSearchUpdateEvent,
): List<GlobalSearchListItem> = when (event) {
    is GlobalSearchUpdateEvent.ChannelUpdated -> this

    is GlobalSearchUpdateEvent.ChannelsDeleted -> filter { item ->
        item !is GlobalSearchListItem.AttachmentItem || item.result.channel.id !in event.ids
    }.removeEmptyDateSeparators()

    is GlobalSearchUpdateEvent.MessageUpdated -> {
        val isDeleted =
            event.message.state == MessageState.Deleted || event.message.state == MessageState.DeletedHard

        if (isDeleted)
            filter {
                it !is GlobalSearchListItem.AttachmentItem || it.result.message.id != event.message.id
            }.removeEmptyDateSeparators()
        else {
            map { item ->
                if (item is GlobalSearchListItem.AttachmentItem && item.result.message.id == event.message.id)
                    item.copy(result = item.result.copy(message = event.message))
                else item
            }
        }
    }
}

private fun List<GlobalSearchListItem>.removeEmptySectionHeaders(): List<GlobalSearchListItem> =
    filterIndexed { i, item ->
        item !is GlobalSearchListItem.SectionHeader || getOrNull(i + 1)?.let {
            it !is GlobalSearchListItem.SectionHeader
        } == true
    }

private fun List<GlobalSearchListItem>.removeEmptyDateSeparators(): List<GlobalSearchListItem> =
    filterIndexed { i, item ->
        item !is GlobalSearchListItem.DateSeparator || getOrNull(i + 1) is GlobalSearchListItem.AttachmentItem
    }
