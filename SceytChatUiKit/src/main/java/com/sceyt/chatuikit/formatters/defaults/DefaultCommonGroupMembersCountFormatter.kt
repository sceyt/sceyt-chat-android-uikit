package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.formatters.Formatter

class DefaultCommonGroupMembersCountFormatter : Formatter<SceytChannel> {
    override fun format(
        context: Context,
        from: SceytChannel
    ): CharSequence {
      val memberCount = from.memberCount

       return if (memberCount > 1)
            context.getString(R.string.sceyt_members_count, memberCount)
        else context.getString(R.string.sceyt_member_count, memberCount)
    }
}