package com.sceyt.sceytchatuikit.persistence.logics.connectionlogic

import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionStateData
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.repositories.UsersRepository
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class PersistenceConnectionLogicImpl(
        private var preference: SceytSharedPreference,
        private val usersDao: UserDao,
        private val usersRepository: UsersRepository) : PersistenceConnectionLogic, CoroutineScope {

    init {
        if (ConnectionEventsObserver.connectionState == ConnectionState.Connected)
            launch { onChangedConnectStatus(ConnectionStateData(ConnectionState.Connected)) }
    }

    override suspend fun onChangedConnectStatus(state: ConnectionStateData) {
        if (state.state == ConnectionState.Connected) {
            preference.setUserId(ClientWrapper.currentUser?.id)
            insertCurrentUser()
            SceytPresenceChecker.startPresenceCheck()
        } else SceytPresenceChecker.stopPresenceCheck()
    }

    private suspend fun insertCurrentUser() {
        (preference.getUserId() ?: ClientWrapper.currentUser?.id)?.let {
            val response = usersRepository.getSceytUserById(it)
            if (response is SceytResponse.Success)
                response.data?.toUserEntity()?.let { entity -> usersDao.insertUser(entity) }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}