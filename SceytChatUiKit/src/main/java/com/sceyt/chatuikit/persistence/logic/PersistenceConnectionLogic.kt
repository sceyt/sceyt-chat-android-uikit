package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData

interface PersistenceConnectionLogic {
    fun onChangedConnectStatus(state: ConnectionStateData)
}