package com.sceyt.chatuikit

import android.content.Context
import androidx.core.provider.FontRequest
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.data.di.repositoryModule
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.di.appModules
import com.sceyt.chatuikit.persistence.di.cacheModule
import com.sceyt.chatuikit.persistence.di.coroutineModule
import com.sceyt.chatuikit.persistence.di.databaseModule
import com.sceyt.chatuikit.persistence.di.interactorModule
import com.sceyt.chatuikit.persistence.di.logicModule
import com.sceyt.chatuikit.presentation.di.viewModelModule
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

class SceytChatUIKit(private val context: Context) {

    fun initialize(apiUrl: String,
                   appId: String,
                   clientId: String,
                   enableDatabase: Boolean = true): ChatClient {
        //Set static flags before calling initialize
        val chatClient = ChatClient.initialize(context, apiUrl, appId, clientId)
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
            interactorModule,
            logicModule,
            repositoryModule,
            cacheModule,
            viewModelModule,
            coroutineModule))
    }
}

