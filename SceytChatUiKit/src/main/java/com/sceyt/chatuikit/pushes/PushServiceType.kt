package com.sceyt.chatuikit.pushes

enum class PushServiceType {
    Fcm, Hms;

    fun stingValue(): String {
        return when (this) {
            Fcm -> "fcm"
            Hms -> "hms"
        }
    }
}