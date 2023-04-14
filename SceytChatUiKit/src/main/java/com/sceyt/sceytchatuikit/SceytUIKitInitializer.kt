package com.sceyt.sceytchatuikit

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.sceyt.chat.ChatClient
import com.sceyt.sceytchatuikit.di.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

class SceytUIKitInitializer(private val application: Application) {

    fun initialize(clientId: String, appId: String, host: String, enableDatabase: Boolean): ChatClient {
        //Set static flags before calling setup
        ChatClient.setEnableNetworkAwarenessReconnection(true)
        val chatClient = ChatClient.initialize(application, host, appId, clientId)
        initKoin(enableDatabase)
        initTheme()
        EmojiCompat.init(BundledEmojiCompatConfig(application))
        EmojiManager.install(GoogleEmojiProvider())
        return chatClient
    }

    private fun initTheme() {
        if (SceytKitConfig.isDarkMode.not())
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
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
            cacheModule,
            viewModelModule))
    }
}

