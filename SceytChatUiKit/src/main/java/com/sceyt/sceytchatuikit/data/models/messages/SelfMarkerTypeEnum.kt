package com.sceyt.sceytchatuikit.data.models.messages

import com.sceyt.chat.models.message.DeliveryStatus

enum class SelfMarkerTypeEnum {
    Displayed,
    Received;

    fun value(): String {
        return when (this) {
            Displayed -> "displayed"
            Received -> "received"
        }
    }

    fun toDeliveryStatus(): DeliveryStatus {
        return when (this) {
            Displayed -> DeliveryStatus.Read
            Received -> DeliveryStatus.Delivered
        }
    }
}