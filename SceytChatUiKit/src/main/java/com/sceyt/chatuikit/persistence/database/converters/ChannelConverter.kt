package com.sceyt.chatuikit.persistence.database.converters

import androidx.room.TypeConverter
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.persistence.extensions.toEnum

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
    fun userActivityStatusToTnt(value: UserState) = value.ordinal

    @TypeConverter
    fun intToUserActivityStatus(value: Int) = value.toEnum<UserState>()

    @TypeConverter
    fun memberTypeEnumToTnt(value: RoleTypeEnum?) = value?.ordinal

    @TypeConverter
    fun intToMemberTypeEnum(value: Int?) = value?.toEnum<RoleTypeEnum>()
}