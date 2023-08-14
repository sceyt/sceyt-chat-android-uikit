package com.sceyt.sceytchatuikit.persistence.converters

import androidx.room.TypeConverter
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserState
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.persistence.extensions.toEnum

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