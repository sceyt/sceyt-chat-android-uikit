package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.util.Log
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import kotlin.math.max
import kotlin.math.min

/**
 * Check for deleted messages by comparing server response with local database
 *
 * Strategy: Calculate a smart range using min/max that automatically includes:
 * - The returned messages from server
 * - The gap between messageId and returned messages (gap detection)
 * - Messages beyond the range if we've reached the end (end detection)
 *
 * Then perform a single range check to find and delete all missing messages.
 */
internal class CheckDeletedMessagesByRangeUseCase(
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache
) {
    private val tag = "CheckDeletedMessages"

    suspend operator fun invoke(
        channelId: ChannelId,
        loadType: LoadType,
        messageId: Long,
        limit: Int,
        serverMessages: List<SceytMessage>
    ) {
        val serverIds = serverMessages.map { it.id }.sorted()

        // LoadNear only checks within the returned range (no beyond-range or gap deletion)
        if (loadType == LoadNear) {
            handleLoadNearDeletion(channelId = channelId, serverIds = serverIds)
            return
        }

        // Case 1: Empty response - delete all messages in the load direction
        if (serverMessages.isEmpty()) {
            Log.i(
                tag,
                "Empty response for $loadType from messageId=$messageId, deleting all in direction"
            )
            deleteByLoadType(
                loadType = loadType,
                channelId = channelId,
                messageId = messageId,
                includeMessage = true
            )
            return
        }

        // Case 2: Single message edge case - only the messageId itself was returned
        if (serverMessages.size == 1 && serverMessages.first().id == messageId) {
            Log.i(
                tag,
                "Single message ($messageId) returned for $loadType, reached end in that direction"
            )
            deleteByLoadType(
                loadType = loadType,
                channelId = channelId,
                messageId = messageId,
                includeMessage = false
            )
            return
        }

        // Prepare for range calculation
        val reachedEnd = limit > serverMessages.size

        // Case 3: Calculate smart range boundaries using min/max
        // This elegantly handles gap detection + beyond-range deletion in one range
        val startId = when (loadType) {
            LoadPrev -> {
                // Loading older messages before messageId
                if (reachedEnd) {
                    // Reached beginning, check from 0 to catch all old messages
                    Log.i(
                        tag,
                        "LoadPrev: Reached beginning, checking from 0 to ${serverIds.last()}"
                    )
                    0
                } else {
                    // Not at end yet, just check returned range
                    serverIds.first()
                }
            }

            else -> {
                // LoadNext/LoadNewest: Loading newer messages after messageId
                // min() includes gap: if server returns [150-170] and messageId=100, checks from 100
                val start = min(messageId, serverIds.first())
                Log.i(
                    tag,
                    "$loadType: Checking from $start (messageId=$messageId, server returned from ${serverIds.first()})"
                )
                start
            }
        }

        val endId = when (loadType) {
            LoadPrev -> {
                // Loading older messages before messageId
                // max() includes gap: if server returns [30-50] and messageId=1000, checks until 1000
                val end = max(messageId, serverIds.last())
                Log.i(
                    tag,
                    "LoadPrev: Checking until $end (messageId=$messageId, server returned until ${serverIds.last()})"
                )
                end
            }

            else -> {
                // LoadNext/LoadNewest: Loading newer messages after messageId
                if (reachedEnd) {
                    // Reached end, check until MAX_VALUE to catch all new messages
                    Log.i(
                        tag,
                        "$loadType: Reached end, checking from ${serverIds.first()} to end"
                    )
                    Long.MAX_VALUE
                } else {
                    // Not at end yet, just check returned range
                    serverIds.last()
                }
            }
        }

        // Validation
        if (startId > endId) {
            Log.e(tag, "Invalid range: startId ($startId) > endId ($endId)")
            return
        }

        Log.i(
            tag,
            "Checking range [$startId, $endId] for deletions (returned ${serverIds.size} messages, reachedEnd=$reachedEnd)"
        )

        // Case 4: Check for deletions within the calculated range
        handleMessagesInRange(channelId, startId, endId, serverIds)
    }

    private suspend fun handleLoadNearDeletion(
        channelId: ChannelId,
        serverIds: List<Long>,
    ) {
        if (serverIds.isEmpty()) {
            Log.i(tag, "LoadNear: Empty response, no deletions")
            return
        }
        if (serverIds.size > 1) {
            Log.i(
                tag,
                "LoadNear: Checking within returned range [${serverIds.first()}, ${serverIds.last()}]"
            )
            handleMessagesInRange(channelId, serverIds.first(), serverIds.last(), serverIds)
        } else {
            Log.i(tag, "LoadNear: Single message returned, no range to check")
        }
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
