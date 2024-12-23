package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getFileUriWithProvider
import com.sceyt.chatuikit.extensions.getMimeType
import java.io.File

fun SceytAttachment.openFile(context: Context) {
    context.openFile(filePath)
}

fun Attachment.openFile(context: Context) {
    context.openFile(filePath)
}

private fun Context.openFile(filePath: String?) {
    try {
        var uri: Uri? = null
        val loadedFile = File(filePath ?: "")
        if (loadedFile.exists()) {
            uri = getFileUriWithProvider(loadedFile)
        }

        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, getMimeType(filePath) ?: "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)

        }
    } catch (e: Exception) {
        Toast.makeText(this, getString(R.string.sceyt_no_proper_app_to_open_file), Toast.LENGTH_SHORT).show()
    }
}