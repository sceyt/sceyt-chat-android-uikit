package com.sceyt.chat.sceytsimplechatapp

import android.app.Application
import com.sceyt.sceytchatuikit.SceytUIKitInitializer
import java.util.UUID

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        SceytUIKitInitializer(this).initialize(
            clientId = UUID.randomUUID().toString(),
            appId = "8lwox2ge93",
            host = "https://us-ohio-api.sceyt.com",
            enableDatabase = true)
    }
}