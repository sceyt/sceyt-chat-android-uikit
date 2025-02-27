package com.sceyt.chatuikit.data.models.channels

enum class UserPermission(val value: String) {
    DeleteChannel("deleteChannel"),
    AddMember("addMember"),
    KickMember("kickMember"),
    KickAndBlockMember("kickAndBlockMember"),
    ChangeMemberRole("changeMemberRole"),
    SendMessage("sendMessage"),
    EditOwnMessage("editOwnMessage"),
    EditAnyMessage("editAnyMessage"),
    DeleteOwnMessage("deleteOwnMessage"),
    DeleteAnyMessage("deleteAnyMessage"),
    SendAttachmentMessage("sendAttachmentMessage"),
    AddMessageReaction("addMessageReaction"),
    DeleteOwnMessageReaction("deleteOwnMessageReaction"),
    DeleteAnyMessageReaction("deleteAnyMessageReaction"),
    MentionMember("mentionMember"),
    GetMembers("getMembers"),
    ForwardMessage("forwardMessage"),
    ClearAllMessages("clearAllMessages"),
    ReplyMessage("replyMessage"),
    PinMessage("pinMessage"),
    UpdateChannel("updateChannel"),
    FreezeChannel("freezeChannel")
}