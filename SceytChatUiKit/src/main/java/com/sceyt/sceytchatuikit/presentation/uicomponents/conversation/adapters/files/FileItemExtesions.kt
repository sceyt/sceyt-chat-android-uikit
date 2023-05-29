package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.extensions.getFileUriWithProvider
import java.io.File


fun SceytAttachment.getFileFromMetadata(): File? {
    val path = filePath ?: return null
    try {
        return File(path)
    } catch (_: Exception) {
    }
    return null
}

fun SceytAttachment.openFile(context: Context) {
    try {
        var uri: Uri? = null
        val loadedFile = File(filePath ?: "")
        if (loadedFile.exists()) {
            uri = context.getFileUriWithProvider(loadedFile)
        }

        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
                .setData(uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)

        }
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.sceyt_no_proper_app_to_open_file), Toast.LENGTH_SHORT).show()
    }
}