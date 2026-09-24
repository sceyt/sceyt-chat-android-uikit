package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.mappers.toSceytPinnedMessage

/** Only the acknowledgement that resolves pending intent may announce the pin. */
internal class ConfirmPinUseCase(
    private val pinnedMessageDao: PinnedMessageDao,
) {

    suspend operator fun invoke(
        channelId: Long,
        messageTid: Long,
        serverPinId: Long,
    ): ConfirmPinResponse {
        val wasPending = pinnedMessageDao.markSynced(messageTid, channelId, serverPinId) > 0

        val stored = pinnedMessageDao
            .getPinnedMessages(channelId, System.currentTimeMillis())
            .firstOrNull { it.pinnedMessageEntity.messageTid == messageTid }
            ?.toSceytPinnedMessage()

        return ConfirmPinResponse(stored, didFlipPendingIntent = wasPending)
    }
}

internal data class ConfirmPinResponse(
    val pinnedMessage: SceytPinnedMessage?,
    val didFlipPendingIntent: Boolean,
)