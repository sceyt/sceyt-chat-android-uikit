package com.sceyt.chatuikit.data.models.channels

/**
 * Avatar of a channel that is being created or edited.
 *
 * In [CreateChannelData] and [EditChannelData] a `null` avatar means no avatar:
 * nothing is set when creating, and the current avatar is removed when editing.
 */
sealed interface ChannelAvatar {

    /**
     * Image file on the device. It is uploaded first, and the uploaded url is set as the avatar.
     */
    data class Local(val filePath: String) : ChannelAvatar

    /**
     * Already uploaded image. The [url] is set as the avatar as is, without uploading.
     */
    data class Remote(val url: String) : ChannelAvatar
}
