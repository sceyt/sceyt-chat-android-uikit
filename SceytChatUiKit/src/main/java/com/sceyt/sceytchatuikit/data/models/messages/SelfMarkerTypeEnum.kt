package com.sceyt.sceytchatuikit.data.models.messages

enum class SelfMarkerTypeEnum {
    Displayed,
    Received;

    override fun toString(): String {
        return when (this) {
            Displayed -> "displayed"
            Received -> "received"
        }
    }
}