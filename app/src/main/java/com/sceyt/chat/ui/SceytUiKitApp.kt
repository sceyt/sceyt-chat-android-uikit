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
import com.emojiview.emojiview.AXEmojiManager
import com.emojiview.emojiview.provider.AXGoogleEmojiProvider
import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.Types
import com.sceyt.chat.connectivity_change.NetworkChangeDetector
import com.sceyt.chat.connectivity_change.NetworkMonitor
import com.sceyt.chat.models.Status
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.sceyt_listeners.ClientListener
import com.sceyt.chat.ui.data.UserSharedPreference
import org.json.JSONObject
import java.util.*

class SceytUiKitApp : Application() {

    private val _sceytConnectionStatus: MutableLiveData<Types.ConnectState> = MutableLiveData()
    val sceytConnectionStatus: LiveData<Types.ConnectState> = _sceytConnectionStatus

    private lateinit var chatClient: ChatClient

    override fun onCreate() {
        super.onCreate()
        initSceyt()
        setSceytListeners()
        connect()

        AXEmojiManager.install(applicationContext, AXGoogleEmojiProvider(applicationContext))
    }

    private fun initSceyt() {
        val serverUrl = "https://us-ohio-api.sceyt.com/"
//        val serverUrl = "http://192.168.178.213:3002"
        val userId = UUID.randomUUID().toString() //Some unique userId
        ChatClient.setEnableNetworkAwarenessReconnection(true)
        chatClient = ChatClient.setup(this, serverUrl, "89p65954oj", userId)
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

    fun connect() {
        val token = UserSharedPreference.getToken(this)
        val userName = UserSharedPreference.getUsername(this)
        if (token.isNullOrBlank() && !userName.isNullOrEmpty()) {
            connectWithoutToken(userName)
        } else if (!token.isNullOrEmpty()) {
            connectWithToken(token)
        }
    }

    private fun connectWithToken(token: String): LiveData<Boolean> {
        val success: MutableLiveData<Boolean> = MutableLiveData()
        chatClient.connect(token)
        addListener(success, token)
        return success
    }

    fun connectWithoutToken(username: String): LiveData<Boolean> {
        val success: MutableLiveData<Boolean> = MutableLiveData()

        if (ClientWrapper.currentUser == null) {
            getTokenByUserName(username, {
                val token = it.get("token")
                chatClient.connect(token as String?)
                addListener(success, token)
            }, {
                success.postValue(false)
            }, this)
        }

        return success
    }


    private fun addListener(success: MutableLiveData<Boolean>, token: String) {
        chatClient.addClientListener("main", object : ClientListener {
            override fun onChangedConnectStatus(connectStatus: Types.ConnectState?, status: Status?) {
                if (connectStatus == Types.ConnectState.StateConnected) {
                    UserSharedPreference.setToken(this@SceytUiKitApp, token)
                    success.postValue(true)
                    ClientWrapper.setPresence(PresenceState.Online, "") {

                    }
                } else if (connectStatus == Types.ConnectState.StateFailed)
                    success.postValue(false)

                _sceytConnectionStatus.postValue(connectStatus)
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