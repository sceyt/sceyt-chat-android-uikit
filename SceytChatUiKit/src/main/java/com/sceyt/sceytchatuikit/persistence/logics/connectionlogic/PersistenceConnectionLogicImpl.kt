package com.sceyt.sceytchatuikit.persistence.logics.connectionlogic

import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.Types
import com.sceyt.chat.models.Status
import com.sceyt.sceytchatuikit.data.SceytSharedPreference

internal class PersistenceConnectionLogicImpl(private var preference: SceytSharedPreference) : PersistenceConnectionLogic {

    override fun onChangedConnectStatus(connectStatus: Types.ConnectState, status: Status?) {
        if (connectStatus == Types.ConnectState.StateConnected)
            preference.setUserId(ClientWrapper.currentUser?.id)
    }
}