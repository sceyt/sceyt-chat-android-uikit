package com.sceyt.chatuikit.persistence.database.converters

import androidx.room.TypeConverter
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.extensions.toEnum
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class MessageConverter {

    companion object {
        private val moshi: Moshi = Moshi.Builder().build()

        private val markerTotalListAdapter =
            moshi.adapter<List<MarkerTotal>>(
                Types.newParameterizedType(List::class.java, MarkerTotal::class.java)
            )

        private val bodyAttributeListAdapter =
            moshi.adapter<List<BodyAttribute>>(
                Types.newParameterizedType(List::class.java, BodyAttribute::class.java)
            )
    }

    // ---------- ENUMS ----------

    @TypeConverter
    fun deliveryStatusToInt(value: MessageDeliveryStatus) = value.ordinal

    @TypeConverter
    fun intToDeliveryStatus(value: Int) = value.toEnum<MessageDeliveryStatus>()

    @TypeConverter
    fun messageStateToInt(value: MessageState) = value.ordinal

    @TypeConverter
    fun intToMessageState(value: Int) = value.toEnum<MessageState>()

    @TypeConverter
    fun transferStateEnumToInt(value: TransferState?) = value?.ordinal

    @TypeConverter
    fun intToTransferStateEnum(value: Int?) = value?.toEnum<TransferState>()

    // ---------- MARKER TOTAL ----------

    @TypeConverter
    fun stringToMarkerTotal(json: String?): List<MarkerTotal>? {
        if (json.isNullOrEmpty()) return null
        return try {
            markerTotalListAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun markerTotalToString(obj: List<MarkerTotal>?): String? {
        if (obj.isNullOrEmpty()) return null
        return markerTotalListAdapter.toJson(obj)
    }

    // ---------- BODY ATTRIBUTES ----------

    @TypeConverter
    fun stringToBodyAttribute(json: String?): List<BodyAttribute>? {
        if (json.isNullOrEmpty()) return null
        return try {
            bodyAttributeListAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun bodyAttributeToString(obj: List<BodyAttribute>?): String? {
        if (obj.isNullOrEmpty()) return null
        return bodyAttributeListAdapter.toJson(obj)
    }

    // ---------- INT LIST ----------

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? =
        value?.joinToString(",")

    @TypeConverter
    fun toIntList(value: String?): List<Int>? =
        value?.split(",")?.mapNotNull { it.toIntOrNull() }
}
