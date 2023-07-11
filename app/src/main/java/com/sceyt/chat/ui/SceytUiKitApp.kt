package com.sceyt.chat.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SCTLogLevel
import com.sceyt.chat.ui.data.AppSharedPreference
import com.sceyt.chat.ui.di.appModules
import com.sceyt.chat.ui.di.viewModelModules
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.SceytUIKitInitializer
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.extensions.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.UUID

class SceytUiKitApp : Application() {
    private val preference by inject<AppSharedPreference>()

    private lateinit var chatClient: ChatClient

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SceytUiKitApp)
            modules(arrayListOf(appModules, viewModelModules))
        }

        initSceyt()
        setNetworkListeners()

        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!ConnectionEventsObserver.isConnected)
                        connect()
                }

                Lifecycle.Event.ON_DESTROY, Lifecycle.Event.ON_PAUSE -> {
                    SceytKitClient.disconnect()
                }

                else -> {}
            }
        })
    }

    private fun initSceyt() {
        ChatClient.setEnableNetworkAwarenessReconnection(true)
        ChatClient.setEnableNetworkChangeDetection(false)
        chatClient = SceytUIKitInitializer(this).initialize(
            clientId = UUID.randomUUID().toString(),
            appId = "yzr58x11rm",
            host = "https://uk-london-south-api-2-staging.waafi.com",
            enableDatabase = true)

        ChatClient.setSceytLogLevel(SCTLogLevel.Info) { i: Int, s: String, s1: String ->
            when (i) {
                Log.DEBUG, Log.INFO -> Log.i(TAG, "$s $s1")
                Log.WARN -> Log.w(TAG, "$s $s1")
                Log.ERROR, Log.ASSERT -> Log.e(TAG, "$s $s1")
            }
        }
    }

    private fun setNetworkListeners() {
        SceytKitClient.getConnectionService().getOnAvailableLiveData().observeForever {
            if (ClientWrapper.currentUser != null)
                SceytKitClient.reconnect()
            else
                connect()
        }

        CoroutineScope(Dispatchers.IO).launch {
            SceytKitClient.onTokenExpired.collect {
                preference.getUserName()?.let {
                    connectWithoutToken(it)
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            SceytKitClient.onTokenWillExpire.collect {
                preference.getUserName()?.let {
                    getTokenByUserName(it, { jsonObject ->
                        (jsonObject.get("token") as? String)?.let { token ->
                            SceytKitClient.updateToken(token) { success, errorMessage ->
                                if (!success)
                                    Log.e("sceytConnectError", errorMessage.toString())
                            }
                        }
                    }, {}, applicationContext)
                }
            }
        }
    }

    private fun connect() {
        val token = preference.getToken()
        val userName = preference.getUserName()
        if (token.isNullOrBlank()) {
            connectWithoutToken(userName ?: return)
        } else if (token.isNotEmpty())
            connectWithToken(token, userName ?: return)
    }

    private fun connectWithToken(token: String, userName: String) {
        SceytKitClient.connect(token, userName)
    }

    fun connectWithoutToken(userName: String): MutableLiveData<Boolean> {
        val successLiveData: MutableLiveData<Boolean> = MutableLiveData()
        if (userName.isBlank()) return successLiveData
        preference.setUserName(userName)

        getTokenByUserName(userName, {
            (it.get("token") as? String)?.let { token ->
                SceytKitClient.addConnectionStateListener(TAG) { success, errorMessage ->
                    successLiveData.postValue(success)
                    if (!success)
                        Log.e("sceytConnectError", errorMessage.toString())
                }
                SceytKitClient.connect(token, userName)
            }
        }, {
            successLiveData.postValue(false)
        }, this)

        return successLiveData
    }

    private fun getTokenByUserName(userName: String, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, context: Context) {
        val queue: RequestQueue = Volley.newRequestQueue(context)
        val url = "https://hm25ehfh6i.execute-api.eu-central-1.amazonaws.com/load-test/user/genToken?user=$userName"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null, listener, errorListener)
        queue.add(jsonObjectRequest)
    }
}