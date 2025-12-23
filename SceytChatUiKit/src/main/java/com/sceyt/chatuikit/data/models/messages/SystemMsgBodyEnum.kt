package com.sceyt.chatuikit.data.models.messages

import android.content.Context
import com.sceyt.chatuikit.R

enum class SystemMsgBodyEnum(
    val value: String
) {
    GroupCreated("CG"),
    ChannelCreated("CC"),
    MemberAdded("AM"),
    MemberRemoved("RM"),
    MemberLeaved("LG"),
    JoinByInviteLink("JL"),
    DisappearingMessage("ADM");

    companion object {
        fun getTitle(string: String, context: Context): String {
            return when (string) {
                GroupCreated.value -> context.getString(R.string.sceyt_created_this_group)
                ChannelCreated.value -> context.getString(R.string.sceyt_created_this_channel)
                else -> ""
            }
        }

        fun getTypeFromString(type: String?): SystemMsgBodyEnum? {
            SystemMsgBodyEnum.entries.forEach {
                if (it.value == type) return it
            }
            return null
        }
    }
}
