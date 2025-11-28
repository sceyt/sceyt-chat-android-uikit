package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.util.Log
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache

/**
 * Check for deleted messages in LoadNear scenario by comparing server response with local database
 *
 * LoadNear loads messages both before and after a given messageId. This use case handles
 * the unique deletion logic for this bidirectional loading pattern.
 *
 * Strategy:
 * 1. Empty response → Delete ALL channel messages except pending (server has no messages)
 * 2. Size < limit → Delete all messages NOT in server response except pending (we have complete list)
 * 3. Split into top (≤ messageId) and bottom (> messageId) and check each direction
 * 4. Check within-range for any gap deletions (except pending messages)
 *
 * Note: Pending messages are never deleted as they haven't been sent to the server yet.
 */
internal class CheckDeletedMessagesByNearMessagesUseCase(
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache
) {
    private val tag = "CheckDeletedMessages"

    suspend operator fun invoke(
        channelId: ChannelId,
        messageId: Long,
        limit: Int,
        serverMessages: List<SceytMessage>
    ) {
        // Case 1: Empty response means server has no messages at all
        // Delete ALL messages in the channel (except pending) because server returned nothing
        if (serverMessages.isEmpty()) {
            Log.i(tag, "LoadNear: Empty server response, deleting ALL messages in channel (except pending)")
            messageDao.deleteAllMessagesByChannelIgnorePending(channelId)
            messagesCache.forceDeleteAllMessagesWhere { message ->
                message.channelId == channelId && message.deliveryStatus != DeliveryStatus.Pending
            }
            return
        }

        val serverIds = serverMessages.map { it.id }.sorted()
        
        // Case 2: Server returned fewer messages than requested limit
        // This means we have the COMPLETE message list from server, so delete all local messages
        // that don't exist in this complete list (except pending)
        if (serverIds.size < limit) {
            Log.i(
                tag,
                "LoadNear: Server returned ${serverIds.size} < limit $limit, treating as complete message list, deleting all not in response"
            )
            messageDao.deleteNotContainsMessagesIgnorePending(channelId, serverIds)
            messagesCache.forceDeleteAllMessagesWhere { message ->
                message.channelId == channelId && !serverIds.contains(message.id) &&
                        message.deliveryStatus != DeliveryStatus.Pending
            }
            return
        }

        // Case 3: Normal LoadNear with full response (size == limit)
        // Split messages into top (messages ≤ messageId) and bottom (messages > messageId)
        // LoadNear loads limit/2 messages in each direction (rounded up for odd limits)
        val topNearIds = serverIds.filter { it <= messageId }.sorted()
        val bottomNearIds = serverIds.filter { it > messageId }.sorted()
        val normalCountTop = (limit.toDouble() / 2).roundUp()  // Expected count in top direction
        val normalCountBottom = limit - normalCountTop          // Expected count in bottom direction

        // Case 3a: No top messages found
        // This means there are no messages ≤ messageId on the server, delete all messages < first returned message
        if (topNearIds.isEmpty()) {
            Log.i(
                tag,
                "LoadNear: No top messages found (topNearIds is empty), deleting all messages before ${bottomNearIds.first()}"
            )
            val deleteFromId = bottomNearIds.first()
            deleteByLoadType(LoadPrev, channelId, deleteFromId, includeMessage = false)
        }

        // Case 3b: No bottom messages found
        // This means there are no messages > messageId on the server, delete all messages > last returned message
        if (bottomNearIds.isEmpty()) {
            Log.i(
                tag,
                "LoadNear: No bottom messages found (bottomNearIds is empty), deleting all messages after ${topNearIds.last()}"
            )
            val deleteFromId = topNearIds.last()
            deleteByLoadType(LoadNext, channelId, deleteFromId, includeMessage = false)
        }

        // Case 3c: Fewer top messages than expected (reached end in top/prev direction)
        // If messageId exists in the small top list, we've reached the beginning, delete all messages before first
        if (topNearIds.size < normalCountTop) {
            val existInTop = topNearIds.contains(messageId)
            if (existInTop) {
                Log.i(
                    tag,
                    "LoadNear: Top count ${topNearIds.size} < normalCountTop $normalCountTop, reached end in top direction"
                )
                val deleteFromId = topNearIds.first()
                deleteByLoadType(LoadPrev, channelId, deleteFromId, includeMessage = true)
            } else {
                // messageId not in top results, will be handled by handleMessagesInRange below
            }
        }

        // Case 3d: Fewer bottom messages than expected (reached end in bottom/next direction)
        // If messageId exists in the small bottom list, we've reached the end, delete all messages after last
        if (bottomNearIds.size < normalCountBottom) {
            val existInBottom = bottomNearIds.contains(messageId)
            if (existInBottom) {
                Log.i(
                    tag,
                    "LoadNear: Bottom count ${bottomNearIds.size} < normalCountBottom $normalCountBottom, reached end in bottom direction"
                )
                val deleteFromId = bottomNearIds.last()
                deleteByLoadType(LoadNext, channelId, deleteFromId, includeMessage = true)
            } else {
                // messageId not in bottom results, will be handled by handleMessagesInRange below
            }
        }

        // Case 4: Check for within-range deletions (messages that exist locally but not in server response)
        // This handles gap detection and any missing messages within the returned range
        Log.i(
            tag,
            "LoadNear: Checking for within-range deletions between [${serverIds.first()}, ${serverIds.last()}]"
        )
        handleMessagesInRange(
            channelId = channelId,
            startId =  serverIds.first(),
            endId = serverIds.last(),
            serverIds = serverIds
        )
    }

    private suspend fun deleteByLoadType(
        loadType: LoadType,
        channelId: ChannelId,
        messageId: Long,
        includeMessage: Boolean
    ) {
        when (loadType) {
            LoadPrev -> deletePreviousMessages(
                channelId = channelId,
                messageId = messageId,
                includeMessage = includeMessage
            )

            LoadNext, LoadNewest -> deleteNextMessages(
                channelId = channelId,
                messageId = messageId,
                includeMessage = includeMessage
            )

            else -> Unit
        }
    }

    /**
     * Check for deleted messages within the specified range
     *
     * This function handles ALL deletion scenarios in a unified way:
     * 1. Gap deletions - Messages between messageId and returned range
     *    Example: messageId=100, returned [150-170] → checks [100-170], finds gap 101-149
     *
     * 2. Within-range deletions - Missing messages in the returned range itself
     *    Example: Server returns [100, 200, 300] but local has [100, 150, 200, 300] → deletes 150
     *
     * 3. Beyond-range deletions - When reached end (startId=0 or endId=MAX_VALUE)
     *    Example: LoadPrev reached end, startId=0 → deletes all messages < returned range
     *
     * Strategy:
     * - Query local DB for all messages in [startId, endId]
     * - Compare with server message IDs
     * - Delete messages that exist locally but not in server response
     */
    private suspend fun handleMessagesInRange(
        channelId: ChannelId,
        startId: Long,
        endId: Long,
        serverIds: List<Long>
    ) {
        Log.i(tag, "Querying local DB for messages in range [$startId, $endId]")

        // Get all message IDs from local database in the range
        val localIds = messageDao.getMessagesIdsByRange(
            channelId = channelId,
            startId = startId,
            endId = endId
        ).toSet()

        Log.i(
            tag,
            "Found ${localIds.size} local messages, server returned ${serverIds.size} messages"
        )

        // Find messages that exist locally but not in server response (deleted on server)
        val deletedMessageIds = localIds.minus(serverIds.toSet())

        if (deletedMessageIds.isNotEmpty()) {
            Log.i(
                tag,
                "Found ${deletedMessageIds.size} deleted messages in range $startId-$endId"
            )

            // Get TIDs (transaction IDs) for the deleted messages
            val tIds = messageDao.getMessageTIdsByIds(*deletedMessageIds.toLongArray())

            // Delete from database
            val count = messageDao.deleteMessagesByTid(tIds)

            // Delete from cache and notify UI
            if (count > 0) {
                messagesCache.hardDeleteMessage(channelId, *tIds.toLongArray())
                Log.i(tag, "Successfully deleted $count messages from DB and cache")
            }
        } else {
            Log.i(tag, "No deleted messages found in range $startId-$endId")
        }
    }

    /**
     * Delete all messages before (and optionally including) the given messageId
     * Used when LoadPrev returns empty or single messageId
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
                message.channelId == channelId && message.deliveryStatus != DeliveryStatus.Pending &&
                        message.id <= compareMessageId
            }
        } else {
            Log.i(tag, "No messages to delete")
        }
    }

    /**
     * Delete all messages after (and optionally including) the given messageId
     * Used when LoadNext returns empty or single messageId
     */
    private suspend fun deleteNextMessages(
        channelId: ChannelId,
        messageId: Long,
        includeMessage: Boolean
    ) {
        val operator = if (includeMessage) ">=" else ">"
        Log.i(tag, "Deleting messages $operator $messageId (includeMessage=$includeMessage)")

        val compareMessageId = if (includeMessage) messageId else messageId + 1
        val count = messageDao.deleteAllMessagesGreaterThenMessageIdIgnorePending(
            channelId = channelId,
            messageId = compareMessageId
        )

        if (count > 0) {
            Log.i(tag, "Deleted $count messages from DB, updating cache")
            messagesCache.forceDeleteAllMessagesWhere { message ->
                message.channelId == channelId && message.deliveryStatus != DeliveryStatus.Pending &&
                        message.id >= compareMessageId
            }
        } else {
            Log.i(tag, "No messages to delete")
        }
    }
}
