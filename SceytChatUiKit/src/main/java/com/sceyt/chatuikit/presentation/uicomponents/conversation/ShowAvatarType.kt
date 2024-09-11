package com.sceyt.chatuikit.presentation.uicomponents.conversation

enum class ShowAvatarType {
    OnlyAvatar, OnlyName, Both, NotShow;

    companion object {
        val avatarSupport = arrayOf(OnlyAvatar, Both)
    }
}