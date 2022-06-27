package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.diff

data class MemberItemPayloadDiff(
        var avatarChanged: Boolean,
        var nameChanged: Boolean,
        var onlineStateChanged: Boolean,
        var roleChanged: Boolean,
        var showMorItemChanged: Boolean,
) {
    fun hasDifference(): Boolean {
        return avatarChanged || nameChanged || onlineStateChanged || roleChanged || showMorItemChanged
    }

    companion object {
        val DEFAULT = MemberItemPayloadDiff(
            avatarChanged = true,
            nameChanged = true,
            onlineStateChanged = true,
            roleChanged = true,
            showMorItemChanged = true
        )

        val NOT_CHANGED_STATE = MemberItemPayloadDiff(
            avatarChanged = false,
            nameChanged = false,
            onlineStateChanged = false,
            roleChanged = false,
            showMorItemChanged = false
        )
    }
}