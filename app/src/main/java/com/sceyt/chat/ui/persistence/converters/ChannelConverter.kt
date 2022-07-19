package com.sceyt.chat.ui.persistence.converters

import androidx.room.TypeConverter
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserActivityStatus
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.persistence.extensions.toEnum

class ChannelConverter {
    @TypeConverter
    fun channelTypeEnumToTnt(value: ChannelTypeEnum) = value.ordinal

    @TypeConverter
    fun intToChannelTypeEnum(value: Int) = value.toEnum<ChannelTypeEnum>()

    @TypeConverter
    fun presenceStateToTnt(value: PresenceState) = value.ordinal

    @TypeConverter
    fun intToPresenceState(value: Int) = value.toEnum<PresenceState>()

    @TypeConverter
    fun userActivityStatusToTnt(value: UserActivityStatus) = value.ordinal

    @TypeConverter
    fun intToUserActivityStatus(value: Int) = value.toEnum<UserActivityStatus>()
}