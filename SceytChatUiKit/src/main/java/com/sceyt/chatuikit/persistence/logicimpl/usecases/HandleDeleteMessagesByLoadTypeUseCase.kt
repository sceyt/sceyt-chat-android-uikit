package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.util.Log
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.presentation.extensions.isNotPending

/**
 * Handle deletion of messages based on LoadType direction
 *
 * This use case handles directional message deletion for LoadPrev, LoadNext, and LoadNewest.
 * LoadNear is not supported as it requires bidirectional logic handled separately.
 *
 * @return true if deletion was handled, false for LoadNear (not supported)
 */
internal class HandleDeleteMessagesByLoadTypeUseCase(
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache
) {
    private val tag = "MessageDeletion"

    /**
     * Delete messages in the direction specified by LoadType
     *
     * @param loadType The direction to delete messages (LoadPrev, LoadNext, LoadNewest)
     * @param channelId The channel ID
     * @param messageId The reference message ID for directional deletion
     * @param includeMessage Whether to include the messageId itself in deletion
     * @return true if deletion was handled, false for LoadNear
     */
    suspend operator fun invoke(
        loadType: LoadType,
        channelId: ChannelId,
        messageId: Long,
        includeMessage: Boolean,
        syncStartTime: Long
    ): Boolean {
        return when (loadType) {
            LoadPrev -> {
                deletePreviousMessages(channelId, messageId, includeMessage)
                true
            }

            LoadNext, LoadNewest -> {
                deleteNextMessages(
                    channelId = channelId,
                    messageId = messageId,
                    includeMessage = includeMessage,
                    syncStartTime = syncStartTime
                )
                true
            }

            LoadNear -> {
                // LoadNear has separate bidirectional handling logic
                Log.i(tag, "LoadNear not handled by this use case")
                false
            }
        }
    }

    /**
     * Delete all messages before (and optionally including) the given messageId
     * Used when LoadPrev returns empty or reaches the beginning
     */
    private suspend fun deletePreviousMessages(
        channelId: ChannelId,
        messageId: Long,
        includeMessage: Boolean
    ) {
        val operator = if (includeMessage) "<=" else "<"
        Log.i(tag, "Deleting messages $operator $messageId (includeMessage=$includeMessage)")

        val compareMessageId = if (includeMessage) messageId else messageId - 1
        val count = messageDao.deleteAllMessagesLowerThenMessageIdIgnorePending(
            channelId = channelId,
            messageId = compareMessageId
        )

        if (count > 0) {
            Log.i(tag, "Deleted $count messages from DB, updating cache")
            messagesCache.forceDeleteAllMessagesWhere { message ->
                message.channelId == channelId && message.isNotPending() &&
                        message.id <= compareMessageId
            }
        } else {
            Log.i(tag, "No messages to delete")
        }
    }

    /**
     * Delete all messages after (and optionally including) the given messageId
     * Used when LoadNext returns empty or reaches the end
     */
    private suspend fun deleteNextMessages(
        channelId: ChannelId,
        messageId: Long,
        includeMessage: Boolean,
        syncStartTime: Long
    ) {
        val operator = if (includeMessage) ">=" else ">"
        Log.i(tag, "Deleting messages $operator $messageId (includeMessage=$includeMessage)")

        val compareMessageId = if (includeMessage) messageId else messageId + 1
        val count = messageDao.deleteAllMessagesGreaterThenMessageIdIgnorePending(
            channelId = channelId,
            messageId = compareMessageId,
            deleteUntil = syncStartTime
        )

        if (count > 0) {
            Log.i(tag, "Deleted $count messages from DB, updating cache")
            messagesCache.forceDeleteAllMessagesWhere { message ->
                message.channelId == channelId && message.isNotPending() &&
                        message.id >= compareMessageId &&
                        (syncStartTime == 0L || message.createdAt < syncStartTime)
            }
        } else {
            Log.i(tag, "No messages to delete")
        }
    }
}

