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
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.Status
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.sceyt_listeners.ClientListener
import com.sceyt.chat.sceyt_listeners.MessageListener
import org.json.JSONObject
import java.util.*

class SceytUiKitApp : Application() {

    private val mSceytConnectionStatus: MutableLiveData<Types.ConnectState> = MutableLiveData()
    val sceytConnectionStatus: LiveData<Types.ConnectState>
        get() = mSceytConnectionStatus

    private val mCurrentUser: MutableLiveData<User> = MutableLiveData()
    val currentUser: LiveData<User>
        get() = mCurrentUser

    private val mConnectionError: MutableLiveData<SceytException> = MutableLiveData()
    val connectionError: LiveData<SceytException>
        get() = mConnectionError

    val totalUnreadCount: MutableLiveData<Int> = MutableLiveData(0)


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
//        ChatClient.setSceytLogLevel(SCTLogLevel.Verbose)
        mSceytConnectionStatus.postValue(Types.ConnectState.StateDisconnect)
    }

    private fun setSceytListeners() {

      /*  chatClient.addMessageListener("main", object : MessageListener {
            override fun onMessage(channel: Channel, message: Message) {
                totalUnreadCount.postValue(totalUnreadCount.value?.plus(1))
                ClientWrapper.markMessagesAsReceived(channel.id, longArrayOf(message.id)) { _, _ ->

                }
            }
        })*/


        var noNetworkObserver = NetworkMonitor.NetworkObserver { connectionType ->
            if (connectionType != NetworkChangeDetector.ConnectionType.CONNECTION_NONE) {
                if (currentUser.value != null)
                    chatClient.reconnect()
                else
                    connect()
            }
        }

        NetworkMonitor.getInstance().addObserver(noNetworkObserver)
    }

    fun connect() {
        //  val token = getToken()
        if (/*token.isEmpty()*/ true) {
            connectWithoutToken("37494202829")
        }/* else {
            connectWithToken(token)
        }*/
    }

    fun connectWithToken(token: String): LiveData<Boolean> {
        val success: MutableLiveData<Boolean> = MutableLiveData()
        chatClient.connect(token)
        return success
    }

    fun connectWithoutToken(username: String): LiveData<Boolean> {
        val success: MutableLiveData<Boolean> = MutableLiveData()

        if (currentUser.value == null) {
            getTokenByUserName(username, {
                val token = it.get("token")
                chatClient.connect(token as String?)

                chatClient.addClientListener("main", object : ClientListener {
                    override fun onChangedConnectStatus(
                            connectStatus: Types.ConnectState?,
                            status: Status?
                    ) {

                        if (connectStatus == Types.ConnectState.StateConnected) {
                            /*fetchCurrentUser()
                            saveToken(token)
                            registerPushToken()*/
                            success.postValue(true)
                            ClientWrapper.setPresence(PresenceState.Online, "") {

                            }

                        } else {
                            mSceytConnectionStatus.postValue(Types.ConnectState.StateFailed)
                            mConnectionError.postValue(status?.error)
                            success.postValue(false)
                        }

                        mSceytConnectionStatus.postValue(connectStatus)
                    }
                })

            }, {
                success.postValue(false)
            }, this)
        }

        return success
    }

    fun getTokenByUserName(userName: String, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener, context: Context) {

        val queue: RequestQueue = Volley.newRequestQueue(context)
        val url =
                "https://tlnig20qy7.execute-api.us-east-2.amazonaws.com/dev/user/genToken?user=$userName"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null, listener, errorListener)

        queue.add(jsonObjectRequest)
    }
}