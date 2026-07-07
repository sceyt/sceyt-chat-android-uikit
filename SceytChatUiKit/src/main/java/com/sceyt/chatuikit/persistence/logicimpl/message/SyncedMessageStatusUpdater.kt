package com.sceyt.chatuikit.persistence.logicimpl.message

import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao

internal class SyncedMessageStatusUpdater(
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache,
) {
    suspend fun updatePreviousMessagesIfNeeded(
        channelId: Long,
        newestMessage: SceytMessage?,
    ) {
        if (newestMessage == null || newestMessage.incoming || newestMessage.id == 0L)
            return

        val syncedStatus = newestMessage.deliveryStatus
        if (!syncedStatus.canAdvancePreviousMessages())
            return

        val localStatus = messageDao.getMessageDeliveryStatus(
            channelId = channelId,
            messageId = newestMessage.id
        ) ?: return

        if (!localStatus.canAdvanceTo(syncedStatus))
            return

        val statusTids = messageDao.updateMessageStatusWithBefore(
            channelId = channelId,
            status = syncedStatus,
            id = newestMessage.id
        ).map { it.tid }.toLongArray()

        if (statusTids.isEmpty())
            return

        messagesCache.applyMessageMarkerChanges(
            channelId = channelId,
            markersByTid = emptyMap(),
            status = syncedStatus,
            statusTids = statusTids
        )
    }

    private fun MessageDeliveryStatus.canAdvancePreviousMessages(): Boolean {
        return this == MessageDeliveryStatus.Received ||
                this == MessageDeliveryStatus.Displayed
    }

    private fun MessageDeliveryStatus.canAdvanceTo(
        syncedStatus: MessageDeliveryStatus,
    ): Boolean {
        return when (syncedStatus) {
            MessageDeliveryStatus.Received -> this == MessageDeliveryStatus.Sent
            MessageDeliveryStatus.Displayed -> this == MessageDeliveryStatus.Sent ||
                    this == MessageDeliveryStatus.Received

            else -> false
        }
    }
}
