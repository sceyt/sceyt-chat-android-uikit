package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.util.Log
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
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
internal class CheckDeletedMessagesUseCase(
    private val deletedNearMessagesUseCase: CheckDeletedNearMessagesUseCase,
    private val deleteByLoadType: HandleDeleteMessagesByLoadTypeUseCase,
    private val handleMessagesInRange: HandleMessagesInRangeUseCase
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
            deletedNearMessagesUseCase(
                channelId = channelId,
                messageId = messageId,
                limit = limit,
                serverMessages = serverMessages
            )
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
            LoadType.LoadPrev -> {
                // Loading older messages before messageId
                if (reachedEnd) {
                    // Reached beginning, check from 0 to catch all old messages
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
                start
            }
        }

        val endId = when (loadType) {
            LoadType.LoadPrev -> {
                // Loading older messages before messageId
                // max() includes gap: if server returns [30-50] and messageId=1000, checks until 1000
                val end = max(messageId, serverIds.last())
                end
            }

            else -> {
                // LoadNext/LoadNewest: Loading newer messages after messageId
                if (reachedEnd) {
                    // Reached end, check until MAX_VALUE to catch all new messages
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
}

