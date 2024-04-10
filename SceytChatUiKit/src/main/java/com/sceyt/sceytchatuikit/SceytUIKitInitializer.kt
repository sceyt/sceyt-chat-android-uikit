package com.sceyt.sceytchatuikit

import android.content.Context
import androidx.core.provider.FontRequest
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ChatClient
import com.sceyt.sceytchatuikit.di.SceytKoinApp
import com.sceyt.sceytchatuikit.di.appModules
import com.sceyt.sceytchatuikit.di.cacheModule
import com.sceyt.sceytchatuikit.di.coroutineModule
import com.sceyt.sceytchatuikit.di.databaseModule
import com.sceyt.sceytchatuikit.di.repositoryModule
import com.sceyt.sceytchatuikit.di.viewModelModule
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

class SceytUIKitInitializer(private val context: Context) {

    fun initialize(clientId: String, appId: String, host: String, enableDatabase: Boolean): ChatClient {
        //Set static flags before calling initialize
        val chatClient = ChatClient.initialize(context, host, appId, clientId)
        initKoin(enableDatabase)
        initEmojiSupport()
        return chatClient
    }

    private fun initKoin(enableDatabase: Boolean) {
        val koin = GlobalContext.getOrNull()
        if (koin == null) {
            SceytKoinApp.koinApp = startKoin {
                init(enableDatabase)
            }
        } else {
            SceytKoinApp.koinApp = koinApplication {
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
            val config = FontRequestEmojiCompatConfig(context, fontRequest)
                .setReplaceAll(true)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        SceytLog.d(TAG, "EmojiCompat initialized")
                    }

                    override fun onFailed(throwable: Throwable?) {
                        SceytLog.e(TAG, "EmojiCompat initialization failed", throwable)
                    }
                })

            EmojiCompat.init(config)
            EmojiManager.install(GoogleEmojiProvider())
        }
    }

    private fun KoinApplication.init(enableDatabase: Boolean) {
        androidContext(context)
        modules(arrayListOf(
            appModules,
            databaseModule(enableDatabase),
            repositoryModule,
            cacheModule,
            viewModelModule,
            coroutineModule))
    }
}

