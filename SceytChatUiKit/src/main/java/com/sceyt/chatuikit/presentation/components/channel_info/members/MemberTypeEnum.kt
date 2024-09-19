package com.sceyt.chatuikit.presentation.components.channel_info.members

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum

enum class MemberTypeEnum {
    Member, Subscriber, Admin;

    fun toRole(): String {
        return when (this) {
            Member, Subscriber -> RoleTypeEnum.Member.toString()
            Admin -> RoleTypeEnum.Admin.toString()
        }
    }

    fun getPageTitle(context: Context): String {
        return when (this) {
            Member -> context.getString(R.string.sceyt_add_members)
            Subscriber -> context.getString(R.string.sceyt_add_subscribers)
            Admin -> context.getString(R.string.sceyt_add_admins)
        }
    }
}