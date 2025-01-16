package com.sceyt.chatuikit.push

import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.push.service.PushService
import org.koin.core.component.inject

object FirebaseMessagingDelegate : SceytKoinComponent {
    private val pushService: PushService by inject()

    @JvmStatic
    fun registerFirebaseToken(token: String) {
        pushService.registerPushDevice(PushDevice(token, PushServiceType.Fcm))
    }

    @JvmStatic
    fun unregisterFirebaseToken(
            unregisterPushCallback: ((success: Boolean, error: String?) -> Unit)? = null
    ) {
        pushService.unregisterPushDevice(unregisterPushCallback)
    }

    @JvmStatic
    fun handleRemoteMessage(remoteMessage: RemoteMessage) {
        if (!isSceytPushNotification(remoteMessage)) {
            return
        }
        val data = getDataFromRemoteMessage(remoteMessage) ?: return
        pushService.handlePush(data)
    }

    @JvmStatic
    fun isSceytPushNotification(remoteMessage: RemoteMessage): Boolean {
        return remoteMessage.isValid()
    }

    fun getDataFromRemoteMessage(remoteMessage: RemoteMessage): PushData? {
        val user = getUserFromPushJson(remoteMessage) ?: return null
        val channel = getChannelFromPushJson(remoteMessage)?.toSceytUiChannel() ?: return null
        val message = getMessageFromPushJson(remoteMessage, channel.id, user)?.toSceytUiMessage()
                ?: return null
        val reaction = getReactionFromPushJson(remoteMessage, message.id, user)
        return PushData(channel, message, user.toSceytUser(), reaction)
    }

    private fun RemoteMessage.isValid() = !data["user"].isNullOrBlank() &&
            !data["channel"].isNullOrBlank() &&
            !data["message"].isNullOrBlank()
}