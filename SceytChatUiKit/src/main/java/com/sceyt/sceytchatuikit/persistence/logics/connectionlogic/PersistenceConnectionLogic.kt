package com.sceyt.sceytchatuikit.persistence.logics.connectionlogic

import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionStateData

interface PersistenceConnectionLogic {
    fun onChangedConnectStatus(state: ConnectionStateData)
}