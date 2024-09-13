package com.sceyt.chatuikit

import android.content.Context
import androidx.core.provider.FontRequest
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.data.di.repositoryModule
import com.sceyt.chatuikit.data.transformers.MessageTransformer
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.di.appModules
import com.sceyt.chatuikit.persistence.di.cacheModule
import com.sceyt.chatuikit.persistence.di.coroutineModule
import com.sceyt.chatuikit.persistence.di.databaseModule
import com.sceyt.chatuikit.persistence.di.interactorModule
import com.sceyt.chatuikit.persistence.di.logicModule
import com.sceyt.chatuikit.persistence.lazyVar
import com.sceyt.chatuikit.presentation.di.viewModelModule
import com.sceyt.chatuikit.sceytconfigs.SceytChatUIKitConfig
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

object SceytChatUIKit : SceytKoinComponent {
    private lateinit var appContext: Context
    val chatUIFacade: SceytChatUIFacade by inject()
    var theme: SceytChatUIKitTheme by lazyVar { SceytChatUIKitTheme() }
    var config: SceytChatUIKitConfig by lazyVar { SceytChatUIKitConfig() }
    val formatters: SceytChatUIKitFormatters by lazy { SceytChatUIKitFormatters() }

    @JvmField
    var messageTransformer: MessageTransformer? = null

    fun initialize(
            appContext: Context,
            apiUrl: String,
            appId: String,
            clientId: String,
            enableDatabase: Boolean = true): ChatClient {

        val chatClient = ChatClient.initialize(appContext, apiUrl, appId, clientId)
        this.appContext = appContext
        initKoin(enableDatabase)
        initEmojiSupport()
        return chatClient
    }

    fun connect(token: String) {
        ChatClient.getClient().connect(token)
    }

    fun reconnect() {
        ChatClient.getClient().reconnect()
    }

    fun disconnect() {
        ChatClient.getClient().disconnect()
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
            val config = FontRequestEmojiCompatConfig(appContext, fontRequest)
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
        androidContext(appContext)
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

