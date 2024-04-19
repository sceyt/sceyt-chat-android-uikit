package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.data.connectionobserver.ConnectionStateData

interface PersistenceConnectionLogic {
    fun onChangedConnectStatus(state: ConnectionStateData)
}