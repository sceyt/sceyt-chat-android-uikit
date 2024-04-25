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
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.sceytconfigs.BackgroundUploadNotificationClickData
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
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

        SceytChatUIKit.initialize(this,
            apiUrl = BuildConfig.API_URL,
            appId = BuildConfig.APP_ID,
            clientId = UUID.randomUUID().toString(),
            enableDatabase = true)

        SceytChatUIKit.theme = SceytChatUIKitTheme(
            accentColor = com.sceyt.chatuikit.R.color.sceyt_color_error,
            //  backgroundColor = com.sceyt.chatuikit.R.color.sceyt_color_admin_role,
            textSecondaryColor = com.sceyt.chatuikit.R.color.sceyt_color_green,
            iconSecondaryColor = com.sceyt.chatuikit.R.color.sceyt_color_warning,
        )

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
}