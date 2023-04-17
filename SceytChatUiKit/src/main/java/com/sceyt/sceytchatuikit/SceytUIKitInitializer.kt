package com.sceyt.sceytchatuikit

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.provider.FontRequest
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ChatClient
import com.sceyt.sceytchatuikit.di.*
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        initEmojiSupport()
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

    private fun initEmojiSupport() {
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs)
            val config = FontRequestEmojiCompatConfig(application, fontRequest)
                .setReplaceAll(true)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        Log.d(TAG, "EmojiCompat initialized")
                    }

                    override fun onFailed(throwable: Throwable?) {
                        Log.e(TAG, "EmojiCompat initialization failed", throwable)
                    }
                })

            EmojiCompat.init(config)
            EmojiManager.install(GoogleEmojiProvider())
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

