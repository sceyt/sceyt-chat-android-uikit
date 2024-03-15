package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.sceytchatuikit.data.models.channels.SceytMember

sealed class ChannelEventEnum {
    data object Created : ChannelEventEnum()
    data object Deleted : ChannelEventEnum()
    data object Updated : ChannelEventEnum()
    data object ClearedHistory : ChannelEventEnum()
    data object Invited : ChannelEventEnum()
    data class Joined(val joinedMembers: List<SceytMember>) : ChannelEventEnum()
    data class Left(val leftMembers: List<SceytMember>) : ChannelEventEnum()
    data class Mute(val muted: Boolean) : ChannelEventEnum()
    data class Hide(val hidden: Boolean) : ChannelEventEnum()
    data class MarkedUs(val read: Boolean) : ChannelEventEnum()
    data class Block(val blocked: Boolean) : ChannelEventEnum()
}