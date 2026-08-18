package com.sceyt.chatuikit.filetransfer

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import java.io.File

/**
 * Selects the local destination for a downloaded attachment file.
 *
 * The returned path must be deterministic for the same attachment and role. This allows the UI kit
 * to find an existing file and reuse a partial download when an operation is resumed or restarted.
 * Two roles of one attachment must resolve to different files.
 */
fun interface FileTransferDestinationProvider {
    fun provideDestination(
        context: Context,
        attachment: SceytAttachment,
        role: TransferRole,
    ): File
}