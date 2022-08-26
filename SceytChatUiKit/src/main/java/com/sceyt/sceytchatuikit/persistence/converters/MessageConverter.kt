package com.sceyt.sceytchatuikit.persistence.converters

import androidx.room.TypeConverter
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.persistence.extensions.toEnum

class MessageConverter {
    @TypeConverter
    fun deliveryStatusToTnt(value: DeliveryStatus) = value.ordinal

    @TypeConverter
    fun intToDeliveryStatus(value: Int) = value.toEnum<DeliveryStatus>()

    @TypeConverter
    fun messageStateToTnt(value: MessageState) = value.ordinal

    @TypeConverter
    fun intToMessageState(value: Int) = value.toEnum<MessageState>()
}