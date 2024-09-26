package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.date.PresenceDateFormatter
import com.sceyt.chatuikit.persistence.mappers.isDeleted
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

open class DefaultUserPresenceDateFormatter : Formatter<User> {
    override fun format(context: Context, from: User): String {
        if (from.isDeleted() || from.blocked)
            return ""

        return when (from.presence?.state ?: return "") {
            PresenceState.Online -> {
                context.getString(R.string.sceyt_online)
            }

            else -> {
                val lastActiveAt = from.presence?.lastActiveAt ?: 0
                if (lastActiveAt == 0L)
                    return ""

                DateTimeUtil.getPresenceDateFormatData(
                    context = context,
                    date = Date(lastActiveAt),
                    dateFormatter = presenceDateFormatter
                )
            }
        }
    }

    protected open val presenceDateFormatter = PresenceDateFormatter()
}
