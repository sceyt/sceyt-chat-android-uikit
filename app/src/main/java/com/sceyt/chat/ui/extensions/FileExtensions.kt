package com.sceyt.chat.ui.extensions

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.net.URLEncoder

fun getMimeType(url: String?): String? {
    if (url.isNullOrBlank()) return null
    var type: String? = null
    try {
        val extension = MimeTypeMap.getFileExtensionFromUrl(URLEncoder.encode(url.trim(), "UTF-8"))?.lowercase()
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    } catch (ex: Exception) {
    }
    return type
}

fun getMimeTypeTakeFirstPart(url: String?): String? {
    return getMimeType(url)?.takeWhile { it != '/' }
}

fun getFileSize(fileUri: String): Long {
    return try {
        var sizeInBytes: Long = 0
        val file = File(fileUri)
        if (file.isFile) sizeInBytes = file.length()
        sizeInBytes
    } catch (e: Exception) {
        0
    }
}

@SuppressLint("Range")
fun getFileName(context: Context, fileUri: Uri): String {
    var fileName = ""

    //Check uri format to avoid null
    val scheme = fileUri.scheme
    if (scheme != null && scheme == ContentResolver.SCHEME_CONTENT) {
        //If scheme is a content
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(fileUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        if (cursor!!.moveToFirst()) {
            fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        cursor.close()
    } else {
        //If scheme is a File
        //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
        fileName = File(fileUri.path).name
    }
    return fileName
}