package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class UnpinMessageUseCase(
    private val messageDao: MessageDao,
    private val pendingPinDao: PendingPinDao,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend operator fun invoke(
        channelId: Long,
        messageTid: Long,
    ): SceytResponse<Boolean> = withContext(ioDispatcher) {
        val message = messageDao.getMessageByTid(messageTid)?.toSceytMessage()
            ?: return@withContext createErrorResponse("Message not found in database")
        if (message.pinDetails?.isPinned != true)
            return@withContext createErrorResponse("Message is not pinned")

        pendingPinDao.replace(
            PendingPinEntity(
                messageTid = messageTid,
                channelId = channelId,
                messageId = message.id,
                isPin = false,
                pinScope = StoredPinScope.Unspecified.value,
                createdAt = System.currentTimeMillis(),
            )
        ) ?: return@withContext createErrorResponse("Message not found in database")

        refreshPinnedMessageCache(channelId, messageTid)
        SceytResponse.Success(true)
    }
}
