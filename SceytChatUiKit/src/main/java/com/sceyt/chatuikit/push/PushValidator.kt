package com.sceyt.chatuikit.push

import com.sceyt.chatuikit.notifications.NotificationType

object PushValidator {
    // Keys
    private const val KEY_APP = "app"
    private const val KEY_TYPE = "type"
    private const val KEY_USER = "user"
    private const val KEY_CHANNEL = "channel"
    private const val KEY_MESSAGE = "message"

    // Values
    private const val APP_VALUE = "vt_chat"

    fun isChatPushNotification(data: Map<String, String>): Boolean {
        val type = data[KEY_TYPE]?.toIntOrNull()
        return data[KEY_APP] == APP_VALUE && type in NotificationType.entries.indices
    }

    @Suppress("unused")
    fun isPushPayloadValid(data: Map<String, String>): Boolean {
        return !data[KEY_USER].isNullOrBlank() &&
                !data[KEY_CHANNEL].isNullOrBlank() &&
                !data[KEY_MESSAGE].isNullOrBlank()
    }
}