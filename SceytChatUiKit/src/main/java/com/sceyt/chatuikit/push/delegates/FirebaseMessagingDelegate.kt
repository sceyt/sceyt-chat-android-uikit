package com.sceyt.chatuikit.push.delegates

import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice
import com.sceyt.chatuikit.push.PushHandleResult
import com.sceyt.chatuikit.push.PushServiceType
import com.sceyt.chatuikit.push.PushValidator

/**
 * Entry point for routing Firebase Cloud Messaging events into the chat SDK.
 * Call it from your [com.google.firebase.messaging.FirebaseMessagingService].
 */
object FirebaseMessagingDelegate : MessagingDelegate() {

    /**
     * Registers the FCM token on the chat server, so the device can receive chat pushes.
     * Call it from `onNewToken`. Does nothing if the token is already registered.
     */
    @JvmStatic
    fun registerFirebaseToken(token: String) {
        pushService.registerPushDevice(PushDevice(token, PushServiceType.Fcm))
    }

    /**
     * Unregisters the current push token from the chat server, e.g. on logout.
     *
     * @param unregisterPushCallback optional, invoked with the result of the operation.
     */
    @JvmStatic
    fun unregisterFirebaseToken(unregisterPushCallback: ((Result<Boolean>) -> Unit)? = null) {
        pushService.unregisterPushDevice(unregisterPushCallback)
    }

    /**
     * Handles the message in the background and returns the parsed [PushData] immediately,
     * or null if the message isn't a chat push notification.
     *
     * @param callback optional, invoked with the [PushHandleResult] once handling is done.
     */
    @JvmStatic
    @JvmOverloads
    fun handleRemoteMessage(
        remoteMessage: RemoteMessage,
        callback: ((PushHandleResult) -> Unit)? = null,
    ): PushData? {
        val data = parseChatPushData(remoteMessage) ?: return null
        pushService.handlePush(data, callback)
        return data
    }

    /**
     * Handles the message and suspends until it's persisted and the notification work
     * is scheduled.
     *
     * @return the [PushHandleResult] carrying the parsed [PushData], or null if the message
     * isn't a chat push notification.
     */
    suspend fun handleRemoteMessageSuspended(remoteMessage: RemoteMessage): PushHandleResult? {
        val data = parseChatPushData(remoteMessage) ?: return null
        return pushService.handlePushSuspended(data)
    }

    /** Returns the parsed [PushData], or null if the message isn't a valid chat push. */
    private fun parseChatPushData(remoteMessage: RemoteMessage): PushData? {
        if (!isChatPushNotification(remoteMessage)) {
            return null
        }
        return getDataFromRemoteMessage(remoteMessage)
    }

    /**
     * Checks whether the message was sent by the chat server, so your own pushes
     * can be routed elsewhere.
     */
    @JvmStatic
    fun isChatPushNotification(remoteMessage: RemoteMessage): Boolean {
        return PushValidator.isChatPushNotification(remoteMessage.data)
    }

    /**
     * Parses the message payload without handling it.
     *
     * @return the [PushData], or null if the payload is missing or malformed.
     */
    @JvmStatic
    fun getDataFromRemoteMessage(remoteMessage: RemoteMessage): PushData? {
        return getDataFromPayload(remoteMessage.data)
    }
}