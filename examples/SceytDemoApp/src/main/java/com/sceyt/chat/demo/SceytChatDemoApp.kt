package com.sceyt.chat.demo

import android.app.Application
import com.sceyt.chat.ChatClient
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.di.apiModule
import com.sceyt.chat.demo.di.appModules
import com.sceyt.chat.demo.di.repositoryModule
import com.sceyt.chat.demo.di.viewModelModules
import com.sceyt.chat.demo.notifications.CustomFileTransferNotificationBuilder
import com.sceyt.chat.demo.notifications.CustomPushNotificationBuilder
import com.sceyt.chat.demo.notifications.CustomPushNotificationChannelProvider
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.PushNotificationConfig
import com.sceyt.chatuikit.push.providers.firebase.FirebasePushServiceProvider
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.UUID

class SceytChatDemoApp : Application() {
    private val connectionProvider by inject<SceytConnectionProvider>()

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
        ChatClient.setEnableNetworkAwarenessReconnection(true)
        ChatClient.setEnableNetworkChangeDetection(true)

        SceytChatUIKit.initialize(
            appContext = this,
            apiUrl = BuildConfig.API_URL,
            appId = BuildConfig.APP_ID,
            clientId = UUID.randomUUID().toString(),
            enableDatabase = true
        )

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
    }
}