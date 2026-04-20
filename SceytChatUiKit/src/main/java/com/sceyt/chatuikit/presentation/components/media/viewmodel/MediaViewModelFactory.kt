package com.sceyt.chatuikit.presentation.components.media.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum.Image
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum.Video
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewTransferHolder

class MediaViewModelFactory(
    private val intent: Intent,
    private val mediaTypes: List<AttachmentTypeEnum> = listOf(Image, Video)
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val reversed = intent.getBooleanExtra(KEY_REVERSED, false)
        val channelId = intent.getLongExtra(KEY_CHANNEL_ID, 0L)
        val attachment = intent.extras?.parcelable<SceytAttachment>(KEY_ATTACHMENT)
        val user = intent.extras?.parcelable<SceytUser>(KEY_USER)
        val openedAttachmentData = attachment?.let {
            AttachmentWithUserData(it, user)
        }
        val types = mediaTypes.map { it.value }
        val preloadedData = MediaPreviewTransferHolder.consume()

        @Suppress("UNCHECKED_CAST")
        return MediaViewModel(reversed, channelId, types, openedAttachmentData, preloadedData) as T
    }

    companion object {
        const val KEY_ATTACHMENT = "KEY_ATTACHMENT"
        const val KEY_USER = "KEY_USER"
        const val KEY_REVERSED = "KEY_REVERSED"
        const val KEY_CHANNEL_ID = "KEY_CHANNEL_ID"
    }
}
