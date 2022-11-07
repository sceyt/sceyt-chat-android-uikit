package com.sceyt.sceytchatuikit

import android.util.Log
import com.sceyt.chat.Types
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import kotlin.coroutines.CoroutineContext

class SceytSyncManager : SceytKoinComponent, CoroutineScope {
    private val channelsMiddleWare: PersistenceChanelMiddleWare by inject()
    private val messagesMiddleWare: PersistenceMessagesMiddleWare by inject()

    init {
        addConnectionListener()
    }

    private fun addConnectionListener() {

        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            ConnectionEventsObserver.onChangedConnectStatusFlow.collect {
                if (it.first == Types.ConnectState.StateConnected) {
                    messagesMiddleWare.sendAllPendingMessages()
                }
            }
        }
    }

    suspend fun startSync() {
        getChannels()
    }

    suspend fun getChannels() {

        channelsMiddleWare.loadAllChannels(5).collect {
            Log.i("sdfsdfdsf", it.toString())
        }
    }

    suspend fun loadMessages(channel: SceytChannel) {
        if (channel.lastReadMessageId == channel.lastMessage?.id) {
            val result = messagesMiddleWare.loadNewestMessages(channel.id,  false, 0,  true)

        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
}