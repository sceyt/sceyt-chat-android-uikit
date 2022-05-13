package com.sceyt.chat.ui.utils.binding

import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.presentation.customviews.Avatar

object MessageBindingUtil {

    @BindingAdapter("bind:setMessageAvatar")
    @JvmStatic
    fun setMessageAvatar(avatar: Avatar, message: SceytUiMessage) {
        if (message.isGroup) {
            if (message.showAvatarAndName) {
                avatar.setNameAndImageUrl(message.from.fullName, message.from.avatarURL)
                avatar.isVisible = true
            } else
                avatar.isInvisible = true
        } else
            avatar.isVisible = false
    }

    @BindingAdapter("bind:setMessageMemberName")
    @JvmStatic
    fun setMessageMemberName(tvName: TextView, message: SceytUiMessage) {
        if (message.showAvatarAndName) {
            tvName.text = message.from.fullName.trim()
            tvName.isVisible = true
        } else
            tvName.isVisible = false
    }
}