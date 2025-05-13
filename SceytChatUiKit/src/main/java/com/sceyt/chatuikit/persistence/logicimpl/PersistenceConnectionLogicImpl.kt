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
import com.sceyt.chatuikit.data.models.onError
import com.sceyt.chatuikit.data.repositories.Keys
import com.sceyt.chatuikit.data.repositories.getUserId
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.extensions.broadcastSharedFlow
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.persistence.repositories.UsersRepository
import com.sceyt.chatuikit.push.service.PushService
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.services.SceytSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

internal class PersistenceConnectionLogicImpl(
        private var preference: SceytSharedPreference,
        private val usersDao: UserDao,
        private val usersRepository: UsersRepository,
        private val pushService: PushService
) : PersistenceConnectionLogic, SceytKoinComponent {

    private val messageLogic: PersistenceMessagesLogic by inject()
    private val reactionsLogic: PersistenceReactionsLogic by inject()
    private val sceytSyncManager: SceytSyncManager by inject()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _allPendingEventsSentFlow = broadcastSharedFlow<Unit>()

    companion object {
        private const val TAG = "PersistenceConnectionLogic"
        private const val MAX_RETRY_COUNT = 5
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    init {
        if (ConnectionEventManager.connectionState == ConnectionState.Connected)
            scope.launch(Dispatchers.IO) {
                onChangedConnectStatus(ConnectionStateData(ConnectionState.Connected))
            }

        scope.launch {
            ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (ConnectionEventManager.isConnected)
                    setUserPresence()
            }
        }
    }

    override fun onChangedConnectStatus(state: ConnectionStateData) {
        if (state.state == ConnectionState.Connected) {
            scope.launch {
                SceytPresenceChecker.startPresenceCheck()
                pushService.ensurePushTokenRegistered()
                insertCurrentUser()
                if (isAppOnForeground())
                    setUserPresence()
            }

            scope.launch(Dispatchers.IO) {
                messageLogic.sendAllPendingMarkers()
                messageLogic.sendAllPendingMessages()
                messageLogic.sendAllPendingMessageStateUpdates()
                reactionsLogic.sendAllPendingReactions()
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
            usersDao.insertUserWithMetadata(it.toUserDb())
            preference.setString(Keys.KEY_USER_ID, it.id)
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

    private suspend fun setUserPresence() = withContext(Dispatchers.IO) {
        setUserPresenceWithRetry()
    }

    private suspend fun setUserPresenceWithRetry(
            retryCount: Int = 0,
            delayMs: Long = INITIAL_RETRY_DELAY_MS
    ) {
        val state = SceytChatUIKit.config.presenceConfig.defaultPresenceState
        SceytChatUIKit.chatUIFacade.userInteractor.setPresenceState(state).onError { exception ->
            if (retryCount < MAX_RETRY_COUNT) {
                SceytLog.i(TAG, "setUserPresence state:$state failed, retrying (${retryCount + 1}/$MAX_RETRY_COUNT): ${exception?.message}")
                // Exponential backoff
                delay(delayMs)
                setUserPresenceWithRetry(retryCount + 1, delayMs * 2)
            } else {
                SceytLog.e(TAG, "setUserPresence state:$state failed after $MAX_RETRY_COUNT retries: ${exception?.message}")
            }
        }
    }
}