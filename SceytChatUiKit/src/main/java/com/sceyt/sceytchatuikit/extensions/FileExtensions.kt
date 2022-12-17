package com.sceyt.sceytchatuikit.extensions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Base64OutputStream
import android.webkit.MimeTypeMap
import com.sceyt.chat.util.FileUtils
import java.io.ByteArrayOutputStream
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

fun File.convertImageFileToBase64(): String {
    return ByteArrayOutputStream().use { outputStream ->
        Base64OutputStream(outputStream, Base64.DEFAULT).use { base64FilterStream ->
            inputStream().use { inputStream ->
                inputStream.copyTo(base64FilterStream)
            }
        }
        return@use outputStream.toString()
    }
}

fun ByteArray.toBase64(): String = String(Base64.encode(this, Base64.DEFAULT))

fun Bitmap?.bitmapToByteArray(): ByteArray? {
    this ?: return null
    return try {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, stream)
        stream.toByteArray()
    } catch (ex: Exception) {
        null
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