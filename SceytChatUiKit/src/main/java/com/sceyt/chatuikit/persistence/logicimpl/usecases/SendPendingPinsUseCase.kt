package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SendPendingPinsUseCase(
    private val pendingPinDao: PendingPinDao,
    private val sendPinUseCase: SendPinUseCase,
    private val sendUnpinUseCase: SendUnpinUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(channelId: Long? = null) = withContext(ioDispatcher) {
        val pending = channelId?.let {
            pendingPinDao.getByChannel(it)
        } ?: pendingPinDao.getAll()

        pending.forEach { entity ->
            if (entity.isPin)
                sendPinUseCase(entity) else sendUnpinUseCase(entity)
        }
    }
}
