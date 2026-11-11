package com.sceyt.chatuikit.filetransfer

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import java.io.File

/**
 * Selects the local destination for a downloaded attachment.
 *
 * The returned path must be deterministic for the same attachment and unique to that attachment.
 * This allows the UI kit to find an existing file and reuse a partial download on resume or restart.
 *
 * The UI kit may overwrite the destination or delete it after a failed download.
 */
fun interface FileTransferDestinationProvider {
    fun provideDestination(
        context: Context,
        attachment: SceytAttachment,
    ): File
}
