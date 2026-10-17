package com.sceyt.chatuikit.push.service

import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice
import com.sceyt.chatuikit.push.PushHandleResult

/**
 * Handles incoming chat push notifications and the push token lifecycle.
 */
interface PushService {

    /**
     * Handles the push in the background, without waiting for the result.
     * Use [handlePushSuspended] if you need to await the outcome.
     *
     * @param data the parsed push payload.
     * @param callback optional, invoked with the [PushHandleResult] once handling is done,
     * on the push service coroutine scope.
     */
    fun handlePush(data: PushData, callback: ((PushHandleResult) -> Unit)? = null)

    /**
     * Persists the push and, if the notification should be displayed, enqueues the
     * notification work, suspending until the work is durably scheduled. Note that
     * the notification itself is displayed later, by the scheduled worker.
     *
     * @param data the parsed push payload.
     * @return the [PushHandleResult] describing the outcome. Never throws, failures are
     * reported as [PushHandleResult.Failed].
     */
    suspend fun handlePushSuspended(data: PushData): PushHandleResult

    /**
     * Registers the device push token on the chat server. Does nothing if the same token
     * and service are already registered.
     */
    fun registerPushDevice(device: PushDevice)

    /**
     * Unregisters the current push token from the chat server, e.g. on logout.
     *
     * @param unregisterPushCallback optional, invoked with the result of the operation.
     */
    fun unregisterPushDevice(unregisterPushCallback: ((Result<Boolean>) -> Unit)?)

    /**
     * Generates a token with the first supported push provider and registers it if needed.
     * Called on connect, to recover from a token that was never delivered to the server.
     */
    fun ensurePushTokenRegistered()
}
