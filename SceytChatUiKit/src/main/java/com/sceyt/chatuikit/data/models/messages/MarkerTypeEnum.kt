package com.sceyt.chatuikit.data.models.messages

enum class MarkerTypeEnum {
    Displayed,
    Received,
    Played;

    override fun toString(): String {
        return value()
    }

    fun value(): String {
        return when (this) {
            Displayed -> "displayed"
            Received -> "received"
            Played -> "played"
        }
    }
}