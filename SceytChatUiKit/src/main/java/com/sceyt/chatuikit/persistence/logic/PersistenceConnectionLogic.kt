package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import kotlinx.coroutines.flow.Flow

interface PersistenceConnectionLogic {
    fun onChangedConnectStatus(state: ConnectionStateData)
    val allPendingEventsSentFlow: Flow<Boolean>
}