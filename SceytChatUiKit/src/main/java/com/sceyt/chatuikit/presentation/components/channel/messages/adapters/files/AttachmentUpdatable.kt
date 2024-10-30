package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.util.Log
import com.sceyt.chatuikit.data.models.messages.SceytAttachment

object AttachmentUpdater {

    fun updateAttachment(old: SceytAttachment, new: SceytAttachment): SceytAttachment {
        if (new.messageTid == old.messageTid
                && new.filePath.isNullOrBlank() && !old.filePath.isNullOrBlank()) {
            Log.w("AttachmentUpdaterTag", "file has been downloaded ignore: " +
                    "old ${old.filePath}, new ${new.transferState}, progress${new.progressPercent}")
            return old
        }
        return new
    }
}