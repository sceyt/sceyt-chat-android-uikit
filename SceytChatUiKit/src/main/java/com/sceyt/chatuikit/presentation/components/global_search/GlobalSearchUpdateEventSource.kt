package com.sceyt.chatuikit.presentation.components.global_search

import androidx.lifecycle.asFlow
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn

sealed class GlobalSearchUpdateEvent {
    data class ChannelUpdated(val channel: SceytChannel) : GlobalSearchUpdateEvent()
    data class ChannelsDeleted(val ids: List<Long>) : GlobalSearchUpdateEvent()
    data class MessagesUpdated(val messages: List<SceytMessage>) : GlobalSearchUpdateEvent()
    data class TransferUpdated(val transferData: TransferData) : GlobalSearchUpdateEvent()

    /**
     * Returns true if this event should trigger a refresh key update.
     * This is needed when item-level changes are not reflected in state equality,
     * ensuring StateFlow emits a new value and UI recomposes.
     * */
    val shouldUpdateRefreshKey: Boolean
        get() = when (this) {
            is ChannelUpdated -> true
            is ChannelsDeleted -> false
            is MessagesUpdated -> messages.none {
                it.state == MessageState.Deleted || it.state == MessageState.DeletedHard
            }

            is TransferUpdated -> false
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

        MessagesCache.messageUpdatedFlow.map { (channelId, messages) ->
            GlobalSearchUpdateEvent.MessagesUpdated(messages)
        },

        FileTransferHelper.onTransferUpdatedLiveData.asFlow()
            .filter { it.state == TransferState.Downloaded }
            .map {
                GlobalSearchUpdateEvent.TransferUpdated(it)
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

    is GlobalSearchUpdateEvent.MessagesUpdated -> {
        val (deleted, updated) = event.messages.partition {
            it.state == MessageState.Deleted || it.state == MessageState.DeletedHard
        }
        val deletedIds = deleted.mapTo(mutableSetOf()) { it.id }
        val updatedMap = updated.associateBy { it.id }
        mapNotNull { item ->
            if (item is GlobalSearchListItem.MessageItem) {
                if (item.result.message.id in deletedIds) null
                else updatedMap[item.result.message.id]
                    ?.let { item.copy(result = item.result.copy(message = it)) }
                    ?: item
            } else item
        }.removeEmptySectionHeaders()
    }

    is GlobalSearchUpdateEvent.TransferUpdated -> this
}

/**
 * Applies a [GlobalSearchUpdateEvent] to a list of items for the attachment tabs
 * (Media, Files, Voice, Links). Handles deletions and, for the Media tab's list
 * (non-grid) mode where message body is displayed, message edits.
 * Channel renames do not affect attachment items visually.
 */
fun List<GlobalSearchListItem>.applyAttachmentUpdateEvent(
    event: GlobalSearchUpdateEvent,
): List<GlobalSearchListItem> = when (event) {
    is GlobalSearchUpdateEvent.ChannelUpdated -> this

    is GlobalSearchUpdateEvent.ChannelsDeleted -> filter { item ->
        item !is GlobalSearchListItem.AttachmentItem || item.result.channel.id !in event.ids
    }.removeEmptyDateSeparators()

    is GlobalSearchUpdateEvent.MessagesUpdated -> {
        val (deleted, updated) = event.messages.partition {
            it.state == MessageState.Deleted || it.state == MessageState.DeletedHard
        }
        val deletedIds = deleted.mapTo(mutableSetOf()) { it.id }
        val updatedMap = updated.associateBy { it.id }
        mapNotNull { item ->
            if (item is GlobalSearchListItem.AttachmentItem) {
                if (item.result.message.id in deletedIds) null
                else updatedMap[item.result.message.id]
                    ?.let { item.copy(result = item.result.copy(message = it)) }
                    ?: item
            } else item
        }.removeEmptyDateSeparators()
    }

    is GlobalSearchUpdateEvent.TransferUpdated -> {
        map { item ->
            if (item is GlobalSearchListItem.AttachmentItem
                && item.result.attachment.messageTid == event.transferData.messageTid
            ) {
                val updatedAttachment = item.result.attachment.mergeTransferUpdate(
                    transferData = event.transferData
                )
                item.copy(result = item.result.copy(attachment = updatedAttachment))
            } else item
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

private fun SceytAttachment.mergeTransferUpdate(
    transferData: TransferData
): SceytAttachment = copy(
    transferState = transferData.state,
    progressPercent = transferData.progressPercent,
    filePath = transferData.filePath,
    url = transferData.url ?: url
)
