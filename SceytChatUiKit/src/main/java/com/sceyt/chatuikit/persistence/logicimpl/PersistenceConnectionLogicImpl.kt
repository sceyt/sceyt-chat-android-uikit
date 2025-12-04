package com.sceyt.chatuikit.persistence.logicimpl

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.repositories.Keys
import com.sceyt.chatuikit.data.repositories.getUserId
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.extensions.broadcastSharedFlow
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logic.PersistencePollLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SetUserPresenceUseCase
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.persistence.repositories.UsersRepository
import com.sceyt.chatuikit.push.service.PushService
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.services.SceytSyncManager
import com.sceyt.chatuikit.services.ServerTimeSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

internal class PersistenceConnectionLogicImpl(
    private val preference: SceytSharedPreference,
    private val usersDao: UserDao,
    private val usersRepository: UsersRepository,
    private val pushService: PushService,
    private val setUserPresenceUseCase: SetUserPresenceUseCase
) : PersistenceConnectionLogic, SceytKoinComponent {

    private val messageLogic: PersistenceMessagesLogic by inject()
    private val reactionsLogic: PersistenceReactionsLogic by inject()
    private val pollLogic: PersistencePollLogic by inject()
    private val sceytSyncManager: SceytSyncManager by inject()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _allPendingEventsSentFlow = broadcastSharedFlow<Unit>()

    init {
        if (ConnectionEventManager.connectionState == ConnectionState.Connected)
            scope.launch {
                onChangedConnectStatus(ConnectionStateData(ConnectionState.Connected))
            }

        scope.launch {
            ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (ConnectionEventManager.isConnected)
                    setUserPresenceUseCase()
            }
        }
    }

    override suspend fun onChangedConnectStatus(state: ConnectionStateData) {
        if (state.state == ConnectionState.Connected) {
            // Update server time synchronization after successful connection
            ServerTimeSync.updateServerTime()
            SceytPresenceChecker.startPresenceCheck()
            pushService.ensurePushTokenRegistered()
            insertCurrentUser()
            if (isAppOnForeground())
                setUserPresenceUseCase()

            withContext(Dispatchers.IO) {
                messageLogic.sendAllPendingMarkers()
                messageLogic.sendAllPendingMessages()
                messageLogic.sendAllPendingMessageStateUpdates()
                reactionsLogic.sendAllPendingReactions()
                pollLogic.sendAllPendingVotes()
                _allPendingEventsSentFlow.tryEmit(Unit)
                if (SceytChatUIKit.config.syncChannelsAfterConnect) {
                    sceytSyncManager.startSync(ChannelListConfig.default)
                }
            }
        } else SceytPresenceChecker.stopPresenceCheck()
    }

    override val allPendingEventsSentFlow: Flow<Unit>
        get() = _allPendingEventsSentFlow

    private suspend fun insertCurrentUser() = withContext(Dispatchers.IO) {
        ClientWrapper.currentUser?.let {
            preference.setString(Keys.KEY_USER_ID, it.id)
            usersDao.insertUserWithMetadata(it.toUserDb())
        } ?: run {
            preference.getUserId()?.let {
                val response = usersRepository.getUserById(it)
                if (response is SceytResponse.Success)
                    response.data?.toUserDb()?.let { userDb ->
                        usersDao.insertUserWithMetadata(userDb)
                    }
            }
        }
    }
}