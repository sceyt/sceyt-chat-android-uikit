package com.sceyt.chat.ui.extensions

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.sceyt.chat.util.FileUtils
import java.io.File

fun getMimeType(url: String?): String? {
    if (url.isNullOrBlank()) return null
    var type: String? = null
    try {
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(url.substring(url.lastIndexOf(".") + 1))
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

fun Context.getPathFromFile(uri: Uri?): String? {
    uri ?: return null
    try {
        return FileUtils(this).getPath(uri)
    } catch (ex: Exception) {
    }
    return null
}