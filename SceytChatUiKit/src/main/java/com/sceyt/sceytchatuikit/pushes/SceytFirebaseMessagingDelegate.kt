package com.sceyt.sceytchatuikit.pushes

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.ReactionScore
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

object SceytFirebaseMessagingDelegate : SceytKoinComponent {
    private val application: Application by inject()
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
        if (FirebaseApp.getApps(application).size > 0) {
            firebaseMessaging.token.addOnCompleteListener {
                if (it.isSuccessful) {
                    onToken(it.result)
                } else
                    Log.e(this@SceytFirebaseMessagingDelegate.TAG, "Error: Firebase didn't returned token")
            }
        } else onToken(null)
    }

    private fun registerClientPushToken(fcmToken: String?) {
        fcmToken ?: return
        ChatClient.getClient().registerPushToken(fcmToken, object : ActionCallback {
            override fun onSuccess() {
                preferences.setString(KEY_FCM_TOKEN, fcmToken)
                preferences.setBoolean(KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION, true)
                Log.i(this@SceytFirebaseMessagingDelegate.TAG, "push token successfully registered")
            }

            override fun onError(e: SceytException) {
                Log.e(this@SceytFirebaseMessagingDelegate.TAG, "push token couldn't register error: $e")
            }
        })
    }

    @JvmStatic
    fun handleRemoteMessage(remoteMessage: RemoteMessage): Boolean {
        if (!remoteMessage.isValid()) {
            return false
        }

        val data = getDataFromRemoteMessage(remoteMessage)
        val channel = data.channel
        val message = data.message

        if (channel != null && message != null)
            messagesLogic.onFcmMessage(data)
        return true
    }

    @JvmStatic
    fun handleRemoteMessageGetData(remoteMessage: RemoteMessage): RemoteMessageData? {
        if (!remoteMessage.isValid()) {
            return null
        }

        val data = getDataFromRemoteMessage(remoteMessage)
        val channel = data.channel
        val message = data.message
        if (channel != null && message != null)
            messagesLogic.onFcmMessage(data)
        return data
    }

    fun getDataFromRemoteMessage(remoteMessage: RemoteMessage): RemoteMessageData {
        val user = getUserFromPushJson(remoteMessage)
        val channel = getChannelFromPushJson(remoteMessage, user)?.toSceytUiChannel()
        val message = getMessageBodyFromPushJson(remoteMessage, channel?.id, user)?.toSceytUiMessage()
        val reactionScore = getReactionScoreFromRemoteMessage(remoteMessage)
        return RemoteMessageData(channel, message, user, reactionScore)
    }

    fun getReactionScoreFromRemoteMessage(remoteMessage: RemoteMessage): ReactionScore? {
        return getReactionScoreFromPushJson(remoteMessage.data["reaction"])
    }

    @Throws(IllegalStateException::class)
    private fun ensureClientInitialized() {
        check(ChatClient.getClient() != null) { "ChatClient should be initialized first!" }
    }

    private fun RemoteMessage.isValid() = !data["user"].isNullOrBlank() &&
            !data["channel"].isNullOrBlank() &&
            !data["message"].isNullOrBlank()
}