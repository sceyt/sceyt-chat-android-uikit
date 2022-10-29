package com.sceyt.sceytchatuikit.persistence.logics.connectionlogic

import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.Types
import com.sceyt.chat.models.Status
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker

internal class PersistenceConnectionLogicImpl(
        private var preference: SceytSharedPreference,
        private val usersDao: UserDao) : PersistenceConnectionLogic {

    init {
        if (ConnectionEventsObserver.connectionState == Types.ConnectState.StateConnected)
            onChangedConnectStatus(Types.ConnectState.StateConnected, null)
    }

    override fun onChangedConnectStatus(connectStatus: Types.ConnectState, status: Status?) {
        if (connectStatus == Types.ConnectState.StateConnected) {
            preference.setUserId(ClientWrapper.currentUser?.id)
            insertCurrentUser()
            SceytPresenceChecker.startPresenceCheck()
        } else SceytPresenceChecker.stopPresenceCheck()
    }

    private fun insertCurrentUser() {
        ClientWrapper.currentUser?.let {
            usersDao.insertUser(it.toUserEntity())
        }
    }
}