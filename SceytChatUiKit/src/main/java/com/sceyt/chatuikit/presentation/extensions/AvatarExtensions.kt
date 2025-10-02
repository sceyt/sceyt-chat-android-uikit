package com.sceyt.chatuikit.presentation.extensions

import com.bumptech.glide.RequestBuilder
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.AvatarErrorPlaceHolder
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.AvatarPlaceholder

fun AvatarView.setUserAvatar(
        user: SceytUser?
) {
    if (user == null) {
        setImageUrl(null)
        return
    }
    appearanceBuilder()
        .setDefaultAvatar(SceytChatUIKit.providers.userDefaultAvatarProvider.provide(context, user))
        .setImageUrl(user.avatarURL)
        .build()
        .applyToAvatar()
}

fun <T> RequestBuilder<T>.applyPlaceHolder(
        placeholder: AvatarPlaceholder?,
): RequestBuilder<T> {
    val placeholder = placeholder ?: return this
    return when (placeholder) {
        is AvatarPlaceholder.FromDrawable -> placeholder(placeholder.drawable)
        is AvatarPlaceholder.FromDrawableRes -> placeholder(placeholder.id)
    }
}

fun <T> RequestBuilder<T>.applyError(
        errorPlaceholder: AvatarErrorPlaceHolder?,
): RequestBuilder<T> {
    val errorPlaceholder = errorPlaceholder ?: return this
    return when (errorPlaceholder) {
        is AvatarErrorPlaceHolder.FromBitmap -> error(errorPlaceholder.bitmap)
        is AvatarErrorPlaceHolder.FromDrawable -> error(errorPlaceholder.drawable)
        is AvatarErrorPlaceHolder.FromDrawableRes -> error(errorPlaceholder.id)
    }
}