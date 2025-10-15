package com.sceyt.chat.demo

import android.app.Application
import android.util.Log
import com.sceyt.chat.ChatClient
import com.sceyt.chat.demo.connection.ChatClientConnectionInterceptor
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.di.apiModule
import com.sceyt.chat.demo.di.appModules
import com.sceyt.chat.demo.di.repositoryModule
import com.sceyt.chat.demo.di.viewModelModules
import com.sceyt.chat.demo.notifications.CustomFileTransferNotificationBuilder
import com.sceyt.chat.demo.notifications.CustomPushNotificationBuilder
import com.sceyt.chat.demo.notifications.CustomPushNotificationChannelProvider
import com.sceyt.chat.models.SCTLogLevel
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ChannelInviteDeepLinkConfig
import com.sceyt.chatuikit.config.PushNotificationConfig
import com.sceyt.chatuikit.providers.ChatTokenProvider
import com.sceyt.chatuikit.push.providers.firebase.FirebasePushServiceProvider
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.UUID

class SceytChatDemoApp : Application() {
    private val connectionProvider by inject<SceytConnectionProvider>()
    private val chatClientConnectionInterceptor by inject<ChatClientConnectionInterceptor>()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SceytChatDemoApp)
            modules(arrayListOf(appModules, viewModelModules, apiModule, repositoryModule))
        }

        initSceyt()
        connectionProvider.init()
    }

    private fun initSceyt() {
        SceytChatUIKit.initialize(
            appContext = this,
            apiUrl = BuildConfig.API_URL,
            appId = BuildConfig.APP_ID,
            clientId = UUID.randomUUID().toString(),
            enableDatabase = true
        )

        setupNotifications()

        ChatClient.setSceytLogLevel(SCTLogLevel.Info) { i: Int, s: String, s1: String ->
            when (i) {
                Log.DEBUG, Log.INFO, Log.VERBOSE -> Log.i(s, s1)
                Log.WARN -> Log.w(s, s1)
                Log.ERROR, Log.ASSERT -> Log.e(s, s1)
            }
        }
    }

    private fun setupNotifications() {
        // Setting Firebase push notification provider
        SceytChatUIKit.config.notificationConfig = PushNotificationConfig(
            pushProviders = listOf(FirebasePushServiceProvider()),
            suppressWhenAppIsInForeground = false
        )

        SceytChatUIKit.notifications.apply {
            // Customizing the push notifications
            pushNotification.apply {
                notificationBuilder = CustomPushNotificationBuilder(this@SceytChatDemoApp)
                notificationChannelProvider = CustomPushNotificationChannelProvider(this@SceytChatDemoApp)
            }

            // Customizing the file transfer notification
            fileTransferServiceNotification.notificationBuilder =
                    CustomFileTransferNotificationBuilder(this@SceytChatDemoApp)
        }

        // Sets the token provider for the SceytChatUIKit.
        // This provider is responsible for supplying authentication tokens required by the ChatClient to establish a connection
        // and mark messages as received when a push notification is received.
        // It retrieves the current user's ID and uses it to fetch a chat token via the chat client connection interceptor.
        SceytChatUIKit.chatTokenProvider = ChatTokenProvider {
            val userId = SceytChatUIKit.currentUserId ?: return@ChatTokenProvider null
            chatClientConnectionInterceptor.getChatToken(userId)
        }
    }
}