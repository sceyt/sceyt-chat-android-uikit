package com.sceyt.chatuikit.persistence.extensions

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelAvatar
import com.sceyt.chatuikit.data.models.map

internal fun ChannelAvatar?.toAvatarUrl(): String = when (this) {
    is ChannelAvatar.Local -> filePath
    is ChannelAvatar.Remote -> url
    null -> ""
}

internal suspend fun ChannelAvatar?.uploadIfLocal(
    upload: suspend (filePath: String) -> SceytResponse<String>,
): SceytResponse<ChannelAvatar?> {
    if (this !is ChannelAvatar.Local) return SceytResponse.Success(this)
    return upload(filePath).map { url -> url?.let { ChannelAvatar.Remote(it) } }
}
