package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider

fun AudioPlayerHelper.alreadyInitialized(provider: AttachmentDataProvider): Boolean {
    val path = provider.attachment.filePath ?: return false
    return alreadyInitialized(
        path = path,
        messageTid = provider.messageTid
    )
}

fun AudioPlayerHelper.alreadyInitialized(attachment: SceytAttachment): Boolean {
    val path = attachment.filePath ?: return false
    return alreadyInitialized(
        path = path,
        messageTid = attachment.messageTid
    )
}

fun AudioPlayerHelper.isCurrentPlayer(provider: AttachmentDataProvider): Boolean {
    val path = provider.filePath ?: return false
    return isCurrentPlayer(
        path = path,
        messageTid = provider.messageTid
    )
}

fun AudioPlayerHelper.seek(provider: AttachmentDataProvider, position: Long) {
    val path = provider.filePath ?: return
    seek(
        filePath = path,
        messageTid = provider.messageTid,
        position = position
    )
}

fun AudioPlayerHelper.toggle(attachment: SceytAttachment) {
    val path = attachment.filePath ?: return
    toggle(
        filePath = path,
        messageTid = attachment.messageTid
    )
}

fun AudioPlayerHelper.stop(provider: AttachmentDataProvider) {
    val path = provider.filePath ?: return
    stop(
        filePath = path,
        messageTid = provider.messageTid
    )
}

fun AudioPlayerHelper.pause(provider: AttachmentDataProvider) {
    val path = provider.filePath ?: return
    pause(
        filePath = path,
        messageTid = provider.messageTid
    )
}

fun AudioPlayerHelper.isPlaying(provider: AttachmentDataProvider): Boolean {
    val path = provider.filePath ?: return false
    return isPlaying(
        path = path,
        messageTid = provider.messageTid
    )
}