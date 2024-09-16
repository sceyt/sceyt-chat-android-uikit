package com.sceyt.chatuikit.persistence.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.persistence.extensions.toEnum
import com.sceyt.chatuikit.persistence.file_transfer.TransferState

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
    fun transferStateEnumToTnt(value: TransferState?) = value?.ordinal

    @TypeConverter
    fun intToTransferStateTypeEnum(value: Int?) = value?.toEnum<TransferState>()

    @TypeConverter
    fun stringToMarkerTotal(json: String?): List<MarkerTotal>? {
        json ?: return null
        val type = object : TypeToken<List<MarkerTotal>>() {}.type
        return try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun markerCountToString(obj: List<MarkerTotal>?): String? {
        if (obj == null)
            return null

        val gson = Gson()
        val type = object : TypeToken<List<MarkerTotal>>() {}.type
        return gson.toJson(obj, type)
    }

    @TypeConverter
    fun stringToBodyAttribute(json: String?): List<BodyAttribute>? {
        json ?: return null
        val type = object : TypeToken<List<BodyAttribute>>() {}.type
        return try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun bodyAttributeToString(obj: List<BodyAttribute>?): String? {
        if (obj == null)
            return null

        val gson = Gson()
        val type = object : TypeToken<List<BodyAttribute>>() {}.type
        return gson.toJson(obj, type)
    }
}