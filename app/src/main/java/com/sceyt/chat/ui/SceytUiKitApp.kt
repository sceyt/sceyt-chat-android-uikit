package com.sceyt.chat.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.Types
import com.sceyt.chat.connectivity_change.NetworkChangeDetector
import com.sceyt.chat.connectivity_change.NetworkMonitor
import com.sceyt.chat.models.Status
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.sceyt_listeners.ClientListener
import com.sceyt.chat.ui.data.AppSharedPreference
import com.sceyt.chat.ui.di.appModules
import com.sceyt.sceytchatuikit.SceytUIKitInitializer
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.*

class SceytUiKitApp : Application() {
    private val preference by inject<AppSharedPreference>()

    private val _sceytConnectionStatus: MutableLiveData<Types.ConnectState> = MutableLiveData()
    val sceytConnectionStatus: LiveData<Types.ConnectState> = _sceytConnectionStatus

    private lateinit var chatClient: ChatClient

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SceytUiKitApp)
            modules(arrayListOf(appModules))
        }

        initSceyt()
        setSceytListeners()
        connect()
    }

    private fun initSceyt() {
        chatClient = SceytUIKitInitializer(this).initialize(UUID.randomUUID().toString(), true)
        _sceytConnectionStatus.postValue(Types.ConnectState.StateDisconnect)
    }

    private fun setSceytListeners() {
        val noNetworkObserver = NetworkMonitor.NetworkObserver { connectionType ->
            if (connectionType != NetworkChangeDetector.ConnectionType.CONNECTION_NONE) {
                if (ClientWrapper.currentUser != null)
                    chatClient.reconnect()
                else
                    connect()
            }
        }

        NetworkMonitor.getInstance().addObserver(noNetworkObserver)
    }

    private fun connect() {
        val token = preference.getToken()
        val userName = preference.getUsername()
        if (token.isNullOrBlank()) {
            connectWithoutToken(userName ?: return)
        } else if (!token.isNullOrEmpty())
            connectWithToken(token, userName ?: return)
    }

    private fun connectWithToken(token: String, userName: String): LiveData<Boolean> {
        val success: MutableLiveData<Boolean> = MutableLiveData()
        chatClient.connect(token)
        addListener(success, token, userName)
        return success
    }

    fun connectWithoutToken(username: String): LiveData<Boolean> {
        val success: MutableLiveData<Boolean> = MutableLiveData()

        getTokenByUserName(username, {
            val token = it.get("token")
            chatClient.connect(token as String?)
            addListener(success, token, username)
        }, {
            success.postValue(false)
        }, this)

        return success
    }


    private fun addListener(success: MutableLiveData<Boolean>, token: String, username: String) {
        chatClient.addClientListener("main", object : ClientListener {
            override fun onChangedConnectStatus(connectStatus: Types.ConnectState?, status: Status?) {
                if (connectStatus == Types.ConnectState.StateConnected) {
                    preference.setToken(token)
                    preference.setUsername(username)
                    success.postValue(true)
                    ClientWrapper.setPresence(PresenceState.Online, "") {

                    }
                } else if (connectStatus == Types.ConnectState.StateFailed)
                    success.postValue(false)
                else if (connectStatus == Types.ConnectState.StateDisconnect) {
                    if (status?.error?.code == 40102)
                        connectWithoutToken(preference.getUsername()
                                ?: return)
                    else success.postValue(false)
                }
                connectStatus?.let {
                    _sceytConnectionStatus.postValue(it)
                }
            }

            override fun onTokenExpired() {
                connectWithoutToken(preference.getUsername() ?: return)
            }
        })
    }

    private fun getTokenByUserName(userName: String, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, context: Context) {
        val queue: RequestQueue = Volley.newRequestQueue(context)
        val url = "https://tlnig20qy7.execute-api.us-east-2.amazonaws.com/dev/user/genToken?user=$userName"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null, listener, errorListener)
        queue.add(jsonObjectRequest)
    }
}