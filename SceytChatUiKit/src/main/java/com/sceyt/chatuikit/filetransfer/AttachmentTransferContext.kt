package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

/**
 * Attachment information supplied by the UI kit for customer transport integrations.
 * The attachment is a snapshot; file-transfer coordination uses the request's file and operation ID.
 */
data class AttachmentTransferContext(
    val attachment: SceytAttachment,
    val isSharedUpload: Boolean = false,
) : FileTransferContext
