package com.sceyt.sceytchatuikit.persistence.logics.connectionlogic

import com.sceyt.chat.Types
import com.sceyt.chat.models.Status

interface PersistenceConnectionLogic {
    fun onChangedConnectStatus(connectStatus: Types.ConnectState, status: Status?)
}