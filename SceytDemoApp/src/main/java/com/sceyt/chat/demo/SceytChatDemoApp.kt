package com.sceyt.chat.demo

import android.app.Application
import android.util.Log
import com.sceyt.chat.ChatClient
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.di.apiModule
import com.sceyt.chat.demo.di.appModules
import com.sceyt.chat.demo.di.repositoryModule
import com.sceyt.chat.demo.di.viewModelModules
import com.sceyt.chat.demo.presentation.conversation.ConversationActivity
import com.sceyt.chat.models.SCTLogLevel
import com.sceyt.sceytchatuikit.SceytUIKitInitializer
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.isAppInDarkMode
import com.sceyt.sceytchatuikit.sceytconfigs.BackgroundUploadNotificationClickData
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.UUID

class SceytChatDemoApp : Application() {
    private val connectionProvider by inject<SceytConnectionProvider>()

    private lateinit var chatClient: ChatClient

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SceytChatDemoApp)
            modules(arrayListOf(appModules, viewModelModules, apiModule, repositoryModule))
        }

        SceytKitConfig.SceytUITheme.isDarkMode = isAppInDarkMode()
        initSceyt()
        connectionProvider.init()
    }

    private fun initSceyt() {
        ChatClient.setEnableNetworkAwarenessReconnection(true)
        ChatClient.setEnableNetworkChangeDetection(true)
        chatClient = SceytUIKitInitializer(this).initialize(
            clientId = UUID.randomUUID().toString(),
            appId = BuildConfig.APP_ID,
            host = BuildConfig.HOST,
            enableDatabase = true)

        SceytKitConfig.backgroundUploadNotificationClickData = BackgroundUploadNotificationClickData(
            ConversationActivity::class.java, ConversationActivity.CHANNEL
        )

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