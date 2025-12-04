package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.util.Log
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.presentation.extensions.isNotPending

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
internal class CheckDeletedNearMessagesUseCase(
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache,
    private val deleteByLoadType: HandleDeleteMessagesByLoadTypeUseCase,
    private val handleMessagesInRange: HandleMessagesInRangeUseCase
) {
    private val tag = "CheckDeletedMessages"

    suspend operator fun invoke(
        channelId: ChannelId,
        messageId: Long,
        limit: Int,
        serverMessages: List<SceytMessage>,
        syncStartTime: Long
    ) {
        // Case 1: Empty response means server has no messages at all
        // Delete ALL messages in the channel (except pending)
        if (serverMessages.isEmpty()) {
            Log.i(
                tag,
                "LoadNear: Empty server response, deleting ALL messages in channel (except pending)"
            )
            messageDao.deleteAllMessagesByChannelIgnorePending(
                channelId = channelId,
                deleteUntil = syncStartTime
            )
            messagesCache.forceDeleteAllMessagesWhere { message ->
                message.channelId == channelId && message.isNotPending() &&
                        (syncStartTime == 0L || message.createdAt < syncStartTime)
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
            val count = messageDao.deleteNotContainsMessagesIgnorePending(
                channelId = channelId,
                messageIds = serverIds,
                deleteUntil = syncStartTime
            )
            messagesCache.forceDeleteAllMessagesWhere { message ->
                message.channelId == channelId && !serverIds.contains(message.id) &&
                        message.isNotPending() &&
                        (syncStartTime == 0L || message.createdAt < syncStartTime)
            }
            Log.i(
                tag,
                "LoadNear: Deleted $count messages from DB as they do not exist on server (syncStartTime: $syncStartTime)"
            )
            return
        }

        // Case 3: Normal LoadNear with full response (size == limit)
        // Split messages into top (messages ≤ messageId) and bottom (messages > messageId)
        // LoadNear loads limit/2 messages in each direction (rounded up for odd limits)
        val topNearIds = serverIds.filter { it <= messageId }.sorted()
        val bottomNearIds = serverIds.filter { it > messageId }.sorted()
        val normalCountTop = (limit.toDouble() / 2).roundUp()  // Expected count in top direction
        val normalCountBottom = limit - normalCountTop        // Expected count in bottom direction

        // Case 3a: No top messages found
        // This means there are no messages ≤ messageId on the server, delete all messages < first returned message
        if (topNearIds.isEmpty()) {
            Log.i(
                tag,
                "LoadNear: No top messages found (topNearIds is empty), deleting all messages before ${bottomNearIds.first()}"
            )
            val deleteFromId = bottomNearIds.first()
            deleteByLoadType(
                loadType = LoadPrev,
                channelId = channelId,
                messageId = deleteFromId,
                includeMessage = false,
                syncStartTime = syncStartTime
            )
        }

        // Case 3b: No bottom messages found
        // This means there are no messages > messageId on the server, delete all messages > last returned message
        if (bottomNearIds.isEmpty()) {
            Log.i(
                tag,
                "LoadNear: No bottom messages found (bottomNearIds is empty), deleting all messages after ${topNearIds.last()}"
            )
            val deleteFromId = topNearIds.last()
            deleteByLoadType(
                loadType = LoadNext,
                channelId = channelId,
                messageId = deleteFromId,
                includeMessage = false,
                syncStartTime = syncStartTime
            )
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
                deleteByLoadType(
                    loadType = LoadPrev,
                    channelId = channelId,
                    messageId = deleteFromId,
                    includeMessage = false,
                    syncStartTime = syncStartTime
                )
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
                deleteByLoadType(
                    loadType = LoadNext,
                    channelId = channelId,
                    messageId = deleteFromId,
                    includeMessage = false,
                    syncStartTime = syncStartTime
                )
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
            startId = serverIds.first(),
            endId = serverIds.last(),
            serverIds = serverIds,
            syncStartTime = syncStartTime
        )
    }
}

