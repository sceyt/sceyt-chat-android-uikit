package com.sceyt.sceytchatuikit

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.emojiview.emojiview.AXEmojiManager
import com.emojiview.emojiview.provider.AXGoogleEmojiProvider
import com.sceyt.chat.ChatClient
import com.sceyt.sceytchatuikit.di.appModules
import com.sceyt.sceytchatuikit.di.databaseModule
import com.sceyt.sceytchatuikit.di.repositoryModule
import com.sceyt.sceytchatuikit.di.viewModels
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

class SceytUIKitInitializer(private val application: Application) {

    fun initialize(userId: String, appId: String, host: String, enableDatabase: Boolean): ChatClient {
        // val serverUrl = "https://us-ohio-api.sceyt.com/"
        // val appId = "89p65954oj"
        ChatClient.setEnableNetworkAwarenessReconnection(true)
        AXEmojiManager.install(application, AXGoogleEmojiProvider(application))
        initKoin(enableDatabase)
        if (SceytUIKitConfig.isDarkMode.not())
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        else  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        return ChatClient.setup(application, host, appId, userId)
    }


    private fun initKoin(enableDatabase: Boolean) {
        val koin = GlobalContext.getOrNull()
        if (koin == null) {
            MyKoinContext.koinApp = startKoin {
                init(enableDatabase)
            }
        } else {
            MyKoinContext.koinApp = koinApplication {
                // declare used modules
                init(enableDatabase)
            }
        }
    }

    private fun KoinApplication.init(enableDatabase: Boolean) {
        androidContext(application)
        modules(arrayListOf(
            appModules,
            databaseModule(enableDatabase),
            repositoryModule,
            viewModels))
    }
}

