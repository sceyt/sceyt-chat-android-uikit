package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao

internal class SendPendingPinsUseCase(
    private val pendingPinDao: PendingPinDao,
    private val sendPinUseCase: SendPinUseCase,
    private val sendUnpinUseCase: SendUnpinUseCase,
) {

    suspend operator fun invoke(channelId: Long? = null) {
        val pending = channelId?.let { pendingPinDao.getByChannel(it) } ?: pendingPinDao.getAll()
        pending.forEach { if (it.isPin) sendPinUseCase(it) else sendUnpinUseCase(it) }
    }
}