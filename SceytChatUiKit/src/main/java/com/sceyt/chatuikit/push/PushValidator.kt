package com.sceyt.chatuikit.push

object PushValidator {

    fun isSceytPushNotification(data: Map<String, String>): Boolean {
        return !data["user"].isNullOrBlank() &&
                !data["channel"].isNullOrBlank() &&
                !data["message"].isNullOrBlank()
    }
}