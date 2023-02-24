package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention

data class MentionUserData(
        val id: String,
        val name: String) {

    override fun toString(): String {
        return "@$name"
    }
}