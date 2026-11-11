package com.sceyt.chatuikit.filetransfer.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import java.io.File

object DefaultFileTransferDestinationProvider : FileTransferDestinationProvider {
    override fun provideDestination(
        context: Context,
        attachment: SceytAttachment,
    ): File {
        val config = SceytChatUIKit.config.attachmentTransferConfig
        val directoryName = when (attachment.type) {
            AttachmentTypeEnum.Image.value -> config.imageDownloadDirectoryName
            AttachmentTypeEnum.Video.value -> config.videoDownloadDirectoryName
            else -> config.fileDownloadDirectoryName
        }

        val rootDirectory = File(context.filesDir, directoryName).apply {
            if (!exists()) mkdirs()
        }
        val messageDirectory = File(rootDirectory, attachment.messageTid.toString()).apply {
            if (!exists()) mkdirs()
        }
        val fileName = attachment.name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace("\u0000", "")
            .trim()
            .takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: "attachment-${attachment.messageTid}"

        return File(messageDirectory, fileName)
    }
}
