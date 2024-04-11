package com.sceyt.chatuikit.persistence.logics.connectionlogic

import com.sceyt.chatuikit.data.connectionobserver.ConnectionStateData

interface PersistenceConnectionLogic {
    suspend fun onChangedConnectStatus(state: ConnectionStateData)
}