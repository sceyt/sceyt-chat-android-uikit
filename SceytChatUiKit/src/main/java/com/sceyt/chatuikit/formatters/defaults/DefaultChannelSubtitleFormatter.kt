package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.formatMemberCount
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.getPeer

open class DefaultChannelSubtitleFormatter : Formatter<SceytChannel> {

    override fun format(context: Context, from: SceytChannel): CharSequence {
        return when (from.getChannelType()) {
            ChannelTypeEnum.Direct -> {
                val user = from.getPeer()?.user ?: return ""
                SceytChatUIKit.formatters.userPresenceDateFormatter.format(context, user)
            }

            ChannelTypeEnum.Group -> {
                val memberCount = from.memberCount
                val formattedCount = memberCount.formatMemberCount()
                if (memberCount > 1)
                    context.getString(R.string.sceyt_members_count, formattedCount)
                else context.getString(R.string.sceyt_member_count, formattedCount)
            }

            ChannelTypeEnum.Public -> {
                val memberCount = from.memberCount
                val formattedCount = memberCount.formatMemberCount()
                if (memberCount > 1)
                    context.getString(R.string.sceyt_subscribers_count, formattedCount)
                else context.getString(R.string.sceyt_subscriber_count, formattedCount)
            }
        }
    }
}
