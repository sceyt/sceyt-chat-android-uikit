package com.sceyt.chatuikit.persistence.logicimpl

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.dao.UserDao
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.mappers.toUserEntity
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.persistence.repositories.UsersRepository
import com.sceyt.chatuikit.push.FirebaseMessagingDelegate
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.services.SceytSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.inject

internal class PersistenceConnectionLogicImpl(
        private var preference: SceytSharedPreference,
        private val usersDao: UserDao,
        private val usersRepository: UsersRepository,
        private val channelsCache: ChannelsCache) : PersistenceConnectionLogic, SceytKoinComponent {

    private val messageLogic: PersistenceMessagesLogic by inject()
    private val reactionsLogic: PersistenceReactionsLogic by inject()
    private val sceytSyncManager: SceytSyncManager by inject()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        if (ConnectionEventManager.connectionState == ConnectionState.Connected)
            scope.launch(Dispatchers.IO) {
                onChangedConnectStatus(ConnectionStateData(ConnectionState.Connected))
            }

        scope.launch {
            ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (ConnectionEventManager.isConnected) {
                    val state = SceytChatUIKit.config.presenceConfig.defaultPresenceState
                    SceytChatUIKit.chatUIFacade.userInteractor.setPresenceState(state)
                }
            }
        }
    }

    override fun onChangedConnectStatus(state: ConnectionStateData) {
        if (state.state == ConnectionState.Connected) {
            scope.launch {
                ClientWrapper.currentUser?.id?.let { preference.setUserId(it) }
                insertCurrentUser()
                SceytPresenceChecker.startPresenceCheck()
                FirebaseMessagingDelegate.checkNeedRegisterForPushToken()
            }

            scope.launch(Dispatchers.IO) {
                messageLogic.sendAllPendingMarkers()
                messageLogic.sendAllPendingMessages()
                messageLogic.sendAllPendingMessageStateUpdates()
                reactionsLogic.sendAllPendingReactions()
                if (SceytChatUIKit.config.syncChannelsAfterConnect) {
                    if (!channelsCache.initialized)
                    // Await 1 second before sync maybe channel cache will be initialized,
                    // otherwise no need to sync
                        delay(1000)
                    sceytSyncManager.startSync(false)
                }
            }
        } else SceytPresenceChecker.stopPresenceCheck()
    }

    private suspend fun insertCurrentUser() {
        ClientWrapper.currentUser?.let {
            usersDao.insertUser(it.toUserEntity())
        } ?: run {
            preference.getUserId()?.let {
                val response = usersRepository.getSceytUserById(it)
                if (response is SceytResponse.Success)
                    response.data?.toUserEntity()?.let { entity -> usersDao.insertUser(entity) }
            }
        }
    }
}