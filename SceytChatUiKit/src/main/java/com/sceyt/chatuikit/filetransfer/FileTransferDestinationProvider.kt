package com.sceyt.chatuikit.filetransfer

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import java.io.File

fun interface FileTransferDestinationProvider {
    fun provideDestination(
        context: Context,
        attachment: SceytAttachment,
    ): File
}
