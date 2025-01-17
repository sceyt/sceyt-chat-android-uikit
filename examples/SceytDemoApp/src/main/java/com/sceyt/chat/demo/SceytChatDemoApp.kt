package com.sceyt.chat.demo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.di.apiModule
import com.sceyt.chat.demo.di.appModules
import com.sceyt.chat.demo.di.repositoryModule
import com.sceyt.chat.demo.di.viewModelModules
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.PushNotificationConfig
import com.sceyt.chatuikit.config.UploadNotificationPendingIntentData
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.notifications.FileTransferNotificationData
import com.sceyt.chatuikit.notifications.defaults.DefaultFileTransferNotificationBuilder
import com.sceyt.chatuikit.notifications.defaults.DefaultPushNotificationBuilder
import com.sceyt.chatuikit.notifications.defaults.DefaultPushNotificationChannelProvider
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.push.PushData
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

        SceytChatUIKit.config.uploadNotificationPendingIntentData = UploadNotificationPendingIntentData(
            classToOpen = ChannelActivity::class.java,
            extraKey = ChannelActivity.CHANNEL
        )

        SceytChatUIKit.config.notificationConfig = PushNotificationConfig(
            pushProviders = listOf(FirebasePushServiceProvider())
        )

        SceytChatUIKit.notifications.pushNotification.apply {
            notificationBuilder = object : DefaultPushNotificationBuilder(this@SceytChatDemoApp) {
                override fun providePendingIntent(context: Context, data: PushData): PendingIntent {
                    val intent = Intent(context, ChannelActivity::class.java).apply {
                        putExtra(ChannelActivity.CHANNEL, data.channel)
                    }
                    return PendingIntent.getActivity(context, data.channel.id.toInt(), intent, pendingIntentFlags)
                }
            }

            notificationChannelProvider = object : DefaultPushNotificationChannelProvider(this@SceytChatDemoApp) {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun createChannel(context: Context): NotificationChannel {
                    return if (isAppOnForeground()) {
                        NotificationChannel(
                            context.getString(R.string.sceyt_chat_notifications_channel_id) + "_silent",
                            "${context.getString(R.string.sceyt_chat_notifications_channel_name)} Silent",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            setSound(null, null)
                            enableVibration(true)
                            NotificationManagerCompat.from(context).createNotificationChannel(this)
                        }
                    } else super.createChannel(context)
                }
            }
        }

        SceytChatUIKit.notifications.fileTransferServiceNotification.notificationBuilder = object : DefaultFileTransferNotificationBuilder(this) {
            override fun providePendingIntent(context: Context, data: FileTransferNotificationData): PendingIntent {
                val intent = Intent(context, ChannelActivity::class.java).apply {
                    putExtra(ChannelActivity.CHANNEL, data.channel)
                }
                return PendingIntent.getActivity(context, data.channel.id.toInt(), intent, pendingIntentFlags)
            }
        }
    }
}