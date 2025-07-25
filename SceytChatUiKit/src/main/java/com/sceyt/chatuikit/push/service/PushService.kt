package com.sceyt.chatuikit.push.service

import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.push.PushDevice

interface PushService {
    fun handlePush(data: PushData)
    fun registerPushDevice(device: PushDevice)
    fun unregisterPushDevice(unregisterPushCallback: ((Result<Boolean>) -> Unit)?)
    fun ensurePushTokenRegistered()
}
