package com.sceyt.sceytchatuikit

import com.sceyt.chat.Types
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SceytSyncManager(private val messagesMiddleWare: PersistenceMessagesMiddleWare) : SceytKoinComponent, CoroutineScope {


    private fun addConnectionListener() {
        launch {
            ConnectionEventsObserver.onChangedConnectStatusFlow.collect {
                if (it.first == Types.ConnectState.StateConnected) {
                    messagesMiddleWare.sendAllPendingMessages()
                }
            }
        }
    }

    fun startSync(){

    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
}