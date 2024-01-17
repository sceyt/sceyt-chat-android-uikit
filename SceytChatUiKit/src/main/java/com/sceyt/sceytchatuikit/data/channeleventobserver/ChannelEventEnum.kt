package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.sceytchatuikit.data.models.channels.SceytMember

sealed class ChannelEventEnum {
    object Created : ChannelEventEnum()
    object Deleted : ChannelEventEnum()
    object Updated : ChannelEventEnum()
    object ClearedHistory : ChannelEventEnum()
    object Invited : ChannelEventEnum()
    data class Joined(val joinedMembers: List<SceytMember>) : ChannelEventEnum()
    data class Left(val leftMembers: List<SceytMember>) : ChannelEventEnum()
    data class Mute(val muted: Boolean) : ChannelEventEnum()
    data class Hide(val hidden: Boolean) : ChannelEventEnum()
    data class MarkedUs(val read: Boolean) : ChannelEventEnum()
    data class Block(val blocked: Boolean) : ChannelEventEnum()
}