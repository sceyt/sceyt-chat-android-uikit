package com.sceyt.sceytchatuikit.persistence.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MarkerCount
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum
import com.sceyt.sceytchatuikit.persistence.extensions.toEnum
import com.sceyt.sceytchatuikit.persistence.filetransfer.ProgressState

class MessageConverter {
    @TypeConverter
    fun deliveryStatusToTnt(value: DeliveryStatus) = value.ordinal

    @TypeConverter
    fun intToDeliveryStatus(value: Int) = value.toEnum<DeliveryStatus>()

    @TypeConverter
    fun messageStateToTnt(value: MessageState) = value.ordinal

    @TypeConverter
    fun intToMessageState(value: Int) = value.toEnum<MessageState>()

    @TypeConverter
    fun selfMarkerTypeEnumToTnt(value: SelfMarkerTypeEnum) = value.ordinal

    @TypeConverter
    fun intToSelfMarkerTypeEnum(value: Int) = value.toEnum<SelfMarkerTypeEnum>()

    @TypeConverter
    fun stringToMarkerCount(json: String?): List<MarkerCount>? {
        val type = object : TypeToken<List<MarkerCount>?>() {}.type
        return Gson().fromJson(json, type)
    }

    @TypeConverter
    fun markerCountToString(obj: List<MarkerCount>?): String? {
        if (obj == null)
            return null

        val gson = Gson()
        val type = object : TypeToken<List<MarkerCount>>() {}.type
        return gson.toJson(obj, type)
    }
}