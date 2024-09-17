package com.sceyt.chatuikit.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.data.repositories.SceytSharedPreferenceImpl.Companion.KEY_FCM_TOKEN
import com.sceyt.chatuikit.data.repositories.SceytSharedPreferenceImpl.Companion.KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import org.koin.core.component.inject

object FirebaseMessagingDelegate : SceytKoinComponent {
    private val context: Context by inject()
    private val messagesLogic: PersistenceMessagesLogic by inject()
    private val preferences: SceytSharedPreference by inject()
    private val firebaseMessaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

    fun registerFirebaseToken(token: String) {
        ensureClientInitialized()

        preferences.setString(KEY_FCM_TOKEN, token)
        registerClientPushToken(token)
    }

    fun unregisterFirebaseToken(
            unregisterPushCallback: ((success: Boolean, error: String?) -> Unit)? = null
    ) {
        ChatClient.getClient().unregisterPushToken(object : ActionCallback {
            override fun onSuccess() {
                preferences.setBoolean(KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION, false)
                preferences.setString(KEY_FCM_TOKEN, null)
                unregisterPushCallback?.invoke(true, null)
            }

            override fun onError(exception: SceytException?) {
                unregisterPushCallback?.invoke(false, exception?.message)
            }
        })
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
        if (FirebaseApp.getApps(context).size > 0) {
            firebaseMessaging.token.addOnCompleteListener {
                if (it.isSuccessful) {
                    onToken(it.result)
                } else
                    SceytLog.e(this@FirebaseMessagingDelegate.TAG, "Error: Firebase didn't returned token")
            }
        } else onToken(null)
    }

    private fun registerClientPushToken(fcmToken: String?) {
        fcmToken ?: return
        ChatClient.getClient().registerPushToken(fcmToken, PushServiceType.Fcm.stingValue(), object : ActionCallback {
            override fun onSuccess() {
                preferences.setString(KEY_FCM_TOKEN, fcmToken)
                preferences.setBoolean(KEY_SUBSCRIBED_FOR_PUSH_NOTIFICATION, true)
                SceytLog.i(this@FirebaseMessagingDelegate.TAG, "push token successfully registered")
            }

            override fun onError(e: SceytException) {
                SceytLog.e(this@FirebaseMessagingDelegate.TAG, "push token couldn't register error: $e")
            }
        })
    }

    @JvmStatic
    suspend fun handleRemoteMessage(remoteMessage: RemoteMessage): Boolean {
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
    suspend fun handleRemoteMessageGetData(remoteMessage: RemoteMessage): RemoteMessageData? {
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
        val channel = getChannelFromPushJson(remoteMessage)?.toSceytUiChannel()
        val message = getMessageFromPushJson(remoteMessage, channel?.id, user)?.toSceytUiMessage()
        val reaction = getReactionFromRemoteMessage(remoteMessage, message?.id, user)
        return RemoteMessageData(channel, message, user, reaction)
    }

    fun getReactionFromRemoteMessage(remoteMessage: RemoteMessage, id: Long?, user: User?): SceytReaction? {
        return getReactionFromPushJson(remoteMessage.data["reaction"], id, user)
    }

    @Throws(IllegalStateException::class)
    private fun ensureClientInitialized() {
        check(ChatClient.getClient() != null) { "ChatClient should be initialized first!" }
    }

    private fun RemoteMessage.isValid() = !data["user"].isNullOrBlank() &&
            !data["channel"].isNullOrBlank() &&
            !data["message"].isNullOrBlank()
}