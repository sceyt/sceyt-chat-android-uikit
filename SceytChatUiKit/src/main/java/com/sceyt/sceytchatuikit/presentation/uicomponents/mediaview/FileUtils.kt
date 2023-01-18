package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun saveToGallery(context: Context, path: String, name: String, mimeType: String): File? {
    Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES
    )?.let { it ->
        try {
            val file = File(it, name)
            FileOutputStream(file).use { fileOutputStream ->
                File(path).inputStream().use { inputStream ->
                    inputStream.copyTo(fileOutputStream)
                }
            }
            MediaScannerConnection.scanFile(
                context, arrayOf(file.path), arrayOf(mimeType)
            ) { _, _ -> }
            return file
        } catch (e: IOException) {
            return null
        }
    }
    return null
}

