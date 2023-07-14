package com.sceyt.chat.ui

import android.app.Application
import android.util.Log
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SCTLogLevel
import com.sceyt.chat.ui.connection.SceytConnectionProvider
import com.sceyt.chat.ui.di.apiModule
import com.sceyt.chat.ui.di.appModules
import com.sceyt.chat.ui.di.repositoryModule
import com.sceyt.chat.ui.di.viewModelModules
import com.sceyt.sceytchatuikit.SceytUIKitInitializer
import com.sceyt.sceytchatuikit.extensions.TAG
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.UUID

class SceytUiKitApp : Application() {
    private val connectionProvider by inject<SceytConnectionProvider>()

    private lateinit var chatClient: ChatClient

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SceytUiKitApp)
            modules(arrayListOf(appModules, viewModelModules, apiModule, repositoryModule))
        }

        initSceyt()
        connectionProvider.init()
    }

    private fun initSceyt() {
        ChatClient.setEnableNetworkAwarenessReconnection(true)
        ChatClient.setEnableNetworkChangeDetection(false)
        chatClient = SceytUIKitInitializer(this).initialize(
            clientId = UUID.randomUUID().toString(),
            appId = "yzr58x11rm",
            host = "https://uk-london-south-api-2-staging.waafi.com",
            enableDatabase = true)

        ChatClient.setSceytLogLevel(SCTLogLevel.Info) { i: Int, s: String, s1: String ->
            when (i) {
                Log.DEBUG, Log.INFO -> Log.i(TAG, "$s $s1")
                Log.WARN -> Log.w(TAG, "$s $s1")
                Log.ERROR, Log.ASSERT -> Log.e(TAG, "$s $s1")
            }
        }
    }

    fun connectChatClient() {
        connectionProvider.connectChatClient()
    }
}