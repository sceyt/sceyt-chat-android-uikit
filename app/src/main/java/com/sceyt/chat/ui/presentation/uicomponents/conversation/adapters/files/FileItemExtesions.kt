package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.gson.Gson
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import com.sceyt.chat.ui.extensions.getFileUriWithProvider
import java.io.File


fun FileListItem.getFileFromMetadata(): File? {
    val metadata = file.metadata ?: return null
    try {
        val data = Gson().fromJson(metadata, AttachmentMetadata::class.java)
        return File(data.localPath)
    } catch (e: Exception) {
    }
    return null
}

fun FileListItem.openFile(context: Context) {
    if (fileLoadData.loading) return

    val fileName = file.name
    var uri: Uri? = null
    if (fileName != null) {
        val loadedFile = File(context.filesDir, fileName)
        if (loadedFile.exists()) {
            uri = context.getFileUriWithProvider(loadedFile)
        } else {
            getFileFromMetadata()?.let {
                uri = context.getFileUriWithProvider(it)
            }
        }
    }

    if (uri != null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
                .setData(uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.sceyt_no_proper_app_to_open_file), Toast.LENGTH_SHORT).show()
        }
    }
}