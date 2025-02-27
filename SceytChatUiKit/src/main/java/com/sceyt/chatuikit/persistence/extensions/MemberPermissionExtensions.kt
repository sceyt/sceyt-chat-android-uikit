package com.sceyt.chatuikit.persistence.extensions

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.UserPermission

fun SceytChannel.haveDeleteChannelPermission(): Boolean {
    return userPermissions.any { it == UserPermission.DeleteChannel.value }
}

fun SceytChannel.haveAddMemberPermission(): Boolean {
    return userPermissions.any { it == UserPermission.AddMember.value }
}

fun SceytChannel.haveKickMemberPermission(): Boolean {
    return userPermissions.any { it == UserPermission.KickMember.value }
}

fun SceytChannel.haveKickAndBlockMemberPermission(): Boolean {
    return userPermissions.any { it == UserPermission.KickAndBlockMember.value }
}

fun SceytChannel.haveChangeMemberRolePermission(): Boolean {
    return userPermissions.any { it == UserPermission.ChangeMemberRole.value }
}

fun SceytChannel.haveSendMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.SendMessage.value }
}

fun SceytChannel.haveEditOwnMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.EditOwnMessage.value }
}

fun SceytChannel.haveEditAnyMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.EditAnyMessage.value }
}

fun SceytChannel.haveDeleteOwnMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.DeleteOwnMessage.value }
}

fun SceytChannel.haveDeleteAnyMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.DeleteAnyMessage.value }
}

fun SceytChannel.haveSendAttachmentMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.SendAttachmentMessage.value }
}

fun SceytChannel.haveAddMessageReactionPermission(): Boolean {
    return userPermissions.any { it == UserPermission.AddMessageReaction.value }
}

fun SceytChannel.haveDeleteOwnMessageReactionPermission(): Boolean {
    return userPermissions.any { it == UserPermission.DeleteOwnMessageReaction.value }
}

fun SceytChannel.haveDeleteAnyMessageReactionPermission(): Boolean {
    return userPermissions.any { it == UserPermission.DeleteAnyMessageReaction.value }
}

fun SceytChannel.haveMentionMemberPermission(): Boolean {
    return userPermissions.any { it == UserPermission.MentionMember.value }
}

fun SceytChannel.haveGetMembersPermission(): Boolean {
    return userPermissions.any { it == UserPermission.GetMembers.value }
}

fun SceytChannel.haveForwardMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.ForwardMessage.value }
}

fun SceytChannel.haveClearAllMessagesPermission(): Boolean {
    return userPermissions.any { it == UserPermission.ClearAllMessages.value }
}

fun SceytChannel.haveReplyMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.ReplyMessage.value }
}

fun SceytChannel.havePinMessagePermission(): Boolean {
    return userPermissions.any { it == UserPermission.PinMessage.value }
}

fun SceytChannel.haveUpdateChannelPermission(): Boolean {
    return userPermissions.any { it == UserPermission.UpdateChannel.value }
}

fun SceytChannel.haveFreezeChannelPermission(): Boolean {
    return userPermissions.any { it == UserPermission.FreezeChannel.value }
}
