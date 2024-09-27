package com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.diff

data class MemberItemPayloadDiff(
        var avatarChanged: Boolean,
        var nameChanged: Boolean,
        var presenceStateChanged: Boolean,
        var roleChanged: Boolean,
        var showMorIconChanged: Boolean,
) {
    fun hasDifference(): Boolean {
        return avatarChanged || nameChanged || presenceStateChanged || roleChanged || showMorIconChanged
    }

    companion object {
        val DEFAULT = MemberItemPayloadDiff(
            avatarChanged = true,
            nameChanged = true,
            presenceStateChanged = true,
            roleChanged = true,
            showMorIconChanged = true
        )

        val NOT_CHANGED_STATE = MemberItemPayloadDiff(
            avatarChanged = false,
            nameChanged = false,
            presenceStateChanged = false,
            roleChanged = false,
            showMorIconChanged = false
        )
    }
}