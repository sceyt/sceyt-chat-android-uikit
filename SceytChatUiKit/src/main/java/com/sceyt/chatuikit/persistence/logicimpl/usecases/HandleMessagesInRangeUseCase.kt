package com.sceyt.chatuikit.persistence.logicimpl.usecases

import android.util.Log
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache

/**
 * Check for deleted messages within a specified range
 * 
 * This use case handles ALL deletion scenarios in a unified way:
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
internal class HandleMessagesInRangeUseCase(
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache
) {
    private val tag = "MessageDeletion"

    /**
     * Find and delete messages within a range that don't exist in server response
     * 
     * @param channelId The channel ID
     * @param startId The starting message ID (inclusive)
     * @param endId The ending message ID (inclusive)
     * @param serverIds List of message IDs returned by the server
     */
    suspend operator fun invoke(
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
}

