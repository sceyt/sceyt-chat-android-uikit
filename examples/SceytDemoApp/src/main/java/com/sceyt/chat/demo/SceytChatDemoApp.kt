package com.sceyt.chat.demo

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sceyt.chat.ChatClient
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.di.apiModule
import com.sceyt.chat.demo.di.appModules
import com.sceyt.chat.demo.di.repositoryModule
import com.sceyt.chat.demo.di.viewModelModules
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.PushNotificationConfig
import com.sceyt.chatuikit.config.UploadNotificationPendingIntentData
import com.sceyt.chatuikit.notifications.defaults.DefaultNotificationBuilder
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.providers.firebase.FirebasePushServiceProvider
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.UUID
import kotlin.random.Random

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

        SceytChatUIKit.config.uploadNotificationPendingIntentData = UploadNotificationPendingIntentData(
            classToOpen = ChannelActivity::class.java,
            extraKey = ChannelActivity.CHANNEL
        )

        SceytChatUIKit.config.notificationConfig = PushNotificationConfig(
            pushProviders = listOf(FirebasePushServiceProvider()),
            suppressWhenAppIsInForeground = true,
        )

        SceytChatUIKit.notifications.notificationBuilder = object : DefaultNotificationBuilder() {
            override fun providePendingIntent(context: Context, data: PushData): PendingIntent {
                val intent = Intent(context, ChannelActivity::class.java).apply {
                    putExtra(ChannelActivity.CHANNEL, data.channel)
                }
                return PendingIntent.getActivity(context, Random.nextInt(), intent, pendingIntentFlags)
            }
        }
    }
}