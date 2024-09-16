package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getFileUriWithProvider
import com.sceyt.chatuikit.extensions.getMimeType
import java.io.File

fun SceytAttachment.openFile(context: Context) {
    try {
        var uri: Uri? = null
        val loadedFile = File(filePath ?: "")
        if (loadedFile.exists()) {
            uri = context.getFileUriWithProvider(loadedFile)
        }

        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, getMimeType(filePath) ?: "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)

        }
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.sceyt_no_proper_app_to_open_file), Toast.LENGTH_SHORT).show()
    }
}