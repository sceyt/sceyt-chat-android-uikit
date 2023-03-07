package com.sceyt.sceytchatuikit.persistence.logics.connectionlogic

import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionStateData

interface PersistenceConnectionLogic {
    suspend fun onChangedConnectStatus(state: ConnectionStateData)
}