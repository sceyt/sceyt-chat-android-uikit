package com.sceyt.chat.ui

import android.app.Application
import com.sceyt.chat.ChatClient
import com.sceyt.chat.ui.di.appModules
import com.sceyt.chat.ui.di.databaseModule
import com.sceyt.chat.ui.di.repositoryModule
import com.sceyt.chat.ui.di.viewModels
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SceytUIKitInitializer(private val application: Application) {

    fun initialize(userId: String, enableDatabase: Boolean): ChatClient {
        val serverUrl = "https://us-ohio-api.sceyt.com/"
//        val serverUrl = "http://192.168.178.213:3002"
        ChatClient.setEnableNetworkAwarenessReconnection(true)
        initKoin(enableDatabase)
        return ChatClient.setup(application, serverUrl, "89p65954oj", userId)
    }


    private fun initKoin(enableDatabase: Boolean) {
        startKoin {
            androidContext(application)
            modules(arrayListOf(
                appModules,
                databaseModule(enableDatabase),
                repositoryModule,
                viewModels))
        }
    }
}