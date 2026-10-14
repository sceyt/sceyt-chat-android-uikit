package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SendUnpinUseCase(
    private val pendingPinDao: PendingPinDao,
    private val pinRepository: PinRepository,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend operator fun invoke(pending: PendingPinEntity): SceytResponse<Boolean> = withContext(ioDispatcher) {
        val response = pinRepository.unpinMessages(
            channelId = pending.channelId,
            messageIds = listOf(pending.messageId),
        )
        val errorType = (response as? SceytResponse.Error)?.let { SDKErrorTypeEnum.fromValue(it.exception?.type) }

        if (response is SceytResponse.Success || errorType == SDKErrorTypeEnum.NotFound) {
            pendingPinDao.settleUnpin(pending)
            refreshPinnedMessageCache(pending.channelId, pending.messageTid)
            return@withContext SceytResponse.Success(true)
        }

        SceytLog.e(TAG, "Unpin failed: type=${errorType?.value}, message=${response.message}")
        if (errorType?.isResendable == false) {
            pendingPinDao.deleteById(pending.id)
            refreshPinnedMessageCache(pending.channelId, pending.messageTid)
        }
        SceytResponse.Error((response as SceytResponse.Error).exception)
    }
}
