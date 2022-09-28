package com.sceyt.sceytchatuikit.persistence.logics.connectionlogic

import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.Types
import com.sceyt.chat.models.Status
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionObserver
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker

internal class PersistenceConnectionLogicImpl(private var preference: SceytSharedPreference) : PersistenceConnectionLogic {

    init {
        if (ConnectionObserver.connectionState == Types.ConnectState.StateConnected)
            onChangedConnectStatus(Types.ConnectState.StateConnected, null)
    }

    override fun onChangedConnectStatus(connectStatus: Types.ConnectState, status: Status?) {
        if (connectStatus == Types.ConnectState.StateConnected) {
            preference.setUserId(ClientWrapper.currentUser?.id)
            SceytPresenceChecker.startPresenceCheck()
        } else SceytPresenceChecker.stopPresenceCheck()
    }
}