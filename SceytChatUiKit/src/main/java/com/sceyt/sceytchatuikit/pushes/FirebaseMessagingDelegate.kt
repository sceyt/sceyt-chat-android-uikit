package com.sceyt.sceytchatuikit.pushes

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.SceytSharedPreferenceImpl.Companion.KEY_FCM_TOKEN
import com.sceyt.sceytchatuikit.data.SceytSharedPreferenceImpl.Companion.KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION
import com.sceyt.sceytchatuikit.data.toSceytUiChannel
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import org.koin.core.component.inject

object FirebaseMessagingDelegate : SceytKoinComponent {
    private val messagesLogic: PersistenceMessagesLogic by inject()
    private val preferences: SceytSharedPreference by inject()
    private val firebaseMessaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

    fun registerFirebaseToken(token: String) {
        ensureClientInitialized()

        preferences.setString(KEY_FCM_TOKEN, token)
        registerClientPushToken(token)
    }

    fun checkNeedRegisterForPushToken() {
        preferences.getString(KEY_FCM_TOKEN)?.let { fcmToken ->
            if (preferences.getBoolean(KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION).not())
                registerClientPushToken(fcmToken)
        } ?: run {
            asyncGetDeviceToken { registerClientPushToken(it) }
        }
    }

    private fun asyncGetDeviceToken(onToken: (token: String?) -> Unit) {
        firebaseMessaging.token.addOnCompleteListener {
            if (it.isSuccessful) {
                onToken(it.result)
            } else
                Log.e(this@FirebaseMessagingDelegate.TAG, "Error: Firebase didn't returned token")
        }
    }

    private fun registerClientPushToken(fcmToken: String?) {
        fcmToken ?: return
        ChatClient.getClient().registerPushToken(fcmToken, object : ActionCallback {
            override fun onSuccess() {
                preferences.setBoolean(KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION, true)
                Log.i(this@FirebaseMessagingDelegate.TAG, "push token successfully registered")
            }

            override fun onError(e: SceytException) {
                Log.e(this@FirebaseMessagingDelegate.TAG, "push token couldn't register error: $e")
            }
        })
    }

    @Throws(IllegalStateException::class)
    @JvmStatic
    fun handleRemoteMessage(remoteMessage: RemoteMessage): Boolean {
        if (!remoteMessage.isValid()) {
            return false
        }


        val u = getUserFromPushJson(remoteMessage.data["user"])
        val c = getChannelFromPushJson(remoteMessage.data["channel"])
        val m = getMessageBodyFromPushJson(remoteMessage.data["message"], c?.id)

        if (c != null && m != null)
            messagesLogic.onFcmMessage(Pair(c.toSceytUiChannel(), m.toSceytUiMessage(c is GroupChannel)))
        return true
    }

    @Throws(IllegalStateException::class)
    @JvmStatic
    fun handleRemoteMessageGetData(remoteMessage: RemoteMessage): Triple<User?, Channel?, Message?>? {
        if (!remoteMessage.isValid()) {
            return null
        }


        val u = getUserFromPushJson(remoteMessage.data["user"])
        val c = getChannelFromPushJson(remoteMessage.data["channel"])
        val m = getMessageBodyFromPushJson(remoteMessage.data["message"], c?.id)

        val triple = Triple(u, c, m)
        if (c != null && m != null)
            messagesLogic.onFcmMessage(Pair(c.toSceytUiChannel(), m.toSceytUiMessage(c is GroupChannel)))
        return triple
    }

    @Throws(IllegalStateException::class)
    private fun ensureClientInitialized() {
        check(ChatClient.getClient() != null) { "ChatClient should be initialized first!" }
    }

    private fun RemoteMessage.isValid() = !data["user"].isNullOrBlank() &&
            !data["channel"].isNullOrBlank() &&
            !data["message"].isNullOrBlank()
}