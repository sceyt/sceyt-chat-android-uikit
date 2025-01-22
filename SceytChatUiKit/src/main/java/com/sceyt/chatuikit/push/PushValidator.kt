package com.sceyt.chatuikit.push

object PushValidator {
    private const val APP_VALUE = "vt_chat"
    private const val KEY_APP = "app"
    private const val KEY_USER = "user"
    private const val KEY_CHANNEL = "channel"
    private const val KEY_MESSAGE = "message"

    fun isSceytPushNotification(data: Map<String, String>): Boolean {
        return data[KEY_APP] == APP_VALUE
    }

    fun isPushPayloadValid(data: Map<String, String>): Boolean {
        return !data[KEY_USER].isNullOrBlank() &&
                !data[KEY_CHANNEL].isNullOrBlank() &&
                !data[KEY_MESSAGE].isNullOrBlank()
    }
}