package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.google.gson.Gson
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.DisappearingMessageMetadata
import com.sceyt.chatuikit.data.models.messages.MembersMetaData
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.messages.SystemMsgBodyEnum
import com.sceyt.chatuikit.data.models.messages.SystemMsgBodyEnum.Companion.getTypeFromString
import com.sceyt.chatuikit.extensions.formatDisappearingMessagesDuration
import com.sceyt.chatuikit.formatters.Formatter

open class DefaultSystemMessageBodyFormatter : Formatter<SceytMessage> {
    override fun format(context: Context, from: SceytMessage): CharSequence {
        val fromName = from.user?.let {
            SceytChatUIKit.formatters.userNameFormatter.format(context, it)
        } ?: context.getString(R.string.sceyt_you)

        return getSystemMessageBody(context, from, fromName.toString())
    }

    private fun getSystemMessageBody(context: Context, message: SceytMessage, fromName: String): String {
        val string = StringBuilder()
        return when (getTypeFromString(message.body)) {
            SystemMsgBodyEnum.GroupCreated, SystemMsgBodyEnum.ChannelCreated -> {
                "$fromName ${SystemMsgBodyEnum.getTitle(message.body, context)}"
            }

            SystemMsgBodyEnum.MemberAdded -> {
                message.metadata?.jsonToObject(MembersMetaData::class.java)?.let {
                    if (!it.members.isNullOrEmpty()) {
                        string.append("$fromName ${context.getString(R.string.sceyt_added)}")
                        initNames(context, it, message.mentionedUsers, string)
                    }
                }
                string.toString()
            }

            SystemMsgBodyEnum.MemberRemoved -> {
                message.metadata?.jsonToObject(MembersMetaData::class.java)?.let {
                    if (!it.members.isNullOrEmpty()) {
                        string.append("$fromName ${context.getString(R.string.sceyt_removed)}")
                        initNames(context, it, message.mentionedUsers, string)
                    }
                }
                string.toString()
            }

            SystemMsgBodyEnum.MemberLeaved -> {
                string.append("$fromName ${context.getString(R.string.sceyt_left_group)}")
                string.toString()
            }

            SystemMsgBodyEnum.JoinByInviteLink -> {
                context.getString(R.string.sceyt_joined_via_invite_link, fromName)
            }

            SystemMsgBodyEnum.DisappearingMessage -> {
                message.metadata?.jsonToObject(DisappearingMessageMetadata::class.java)?.let { data ->
                    val durationMillis = data.duration?.toLongOrNull() ?: 0L
                    val durationSeconds = durationMillis / 1000

                    if (durationSeconds == 0L) {
                        string.append(context.getString(R.string.sceyt_disappearing_message_disabled, fromName))
                    } else {
                        val timeText = durationSeconds.formatDisappearingMessagesDuration(context)
                        string.append(context.getString(R.string.sceyt_disappearing_message_set, fromName, timeText))
                    }
                }
                string.toString()
            }

            else -> ""
        }
    }

    private fun initNames(
        context: Context,
        data: MembersMetaData,
        users: List<SceytUser>?,
        builder: StringBuilder,
    ) {
        if (!data.members.isNullOrEmpty()) {
            for ((index, memberId) in data.members.withIndex()) {
                if (index > 0)
                    builder.append(",")
                val user = users?.find { it.id == memberId }
                if (user != null) {
                    val userName = SceytChatUIKit.formatters.userNameFormatter.format(context, user)
                    builder.append(" $userName")
                } else {
                    builder.append(" $memberId")
                }
                if (index == 4 && data.members.size > 5) {
                    val moreCount = data.members.size - 5
                    builder.append(" ${context.getString(R.string.sceyt_and)} $moreCount ${context.getString(R.string.sceyt_more)}")
                    return
                }
            }
        }
    }

    private inline fun <reified T> String.jsonToObject(type: Class<T>): T? {
        return try {
            Gson().fromJson(this, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
