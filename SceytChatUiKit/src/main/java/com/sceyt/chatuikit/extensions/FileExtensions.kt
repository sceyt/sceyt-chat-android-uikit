package com.sceyt.chatuikit.extensions

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Base64OutputStream
import android.webkit.MimeTypeMap
import com.sceyt.chat.util.FileUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.net.toUri

fun getMimeType(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return try {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(url.substring(url.lastIndexOf(".") + 1))
    } catch (_: Exception) {
        null
    }
}

fun getMimeTypeTakeFirstPart(url: String?): String? {
    return getMimeType(url)?.takeWhile { it != '/' }
}

fun getMimeTypeTakeExtension(url: String?): String? {
    return getMimeType(url)?.split("/")?.getOrNull(1)?.let {
        ".$it"
    }
}

fun getFileSize(fileUri: String): Long {
    return try {
        var sizeInBytes: Long = 0
        val file = File(fileUri)
        if (file.isFile) sizeInBytes = file.length()
        sizeInBytes
    } catch (_: Exception) {
        0
    }
}

fun getFileSizeMb(filePath: String): Double {
    return try {
        var sizeInBytes: Long = 0
        val file = File(filePath)
        if (file.isFile) sizeInBytes = file.length()
        sizeInBytes / 1000.0 / 1000.0
    } catch (_: Exception) {
        0.0
    }
}

fun File.convertImageFileToBase64(): String {
    return ByteArrayOutputStream().use { outputStream ->
        Base64OutputStream(outputStream, Base64.NO_WRAP).use { base64FilterStream ->
            inputStream().use { inputStream ->
                inputStream.copyTo(base64FilterStream)
            }
        }
        return@use outputStream.toString()
    }
}

@Throws(IOException::class)
fun copyFile(context: Context, uri: String, name: String): File {
    val file = File(uri)
    if (file.exists()) return file
    return File(context.filesDir, name)
        .apply {
            if (!exists()) {
                outputStream().use { cache ->
                    context.contentResolver.openInputStream(uri.toUri()).use { inputStream ->
                        inputStream?.copyTo(cache)
                    }
                }
            }
        }
}

fun ByteArray.toBase64(): String = String(Base64.encode(this, Base64.NO_WRAP))

fun Bitmap?.bitmapToByteArray(): ByteArray? {
    this ?: return null
    return try {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.toByteArray()
    } catch (_: Exception) {
        null
    }

}

fun Context.getPathFromFile(uri: Uri?): String? {
    uri ?: return null
    try {
        return FileUtils(this).getPath(uri)
    } catch (_: Exception) {
    }
    return null
}

fun saveToGallery(context: Context, path: String, name: String, mimeType: String): File? {
    val environment = when (mimeType) {
        "image/jpeg" -> Environment.DIRECTORY_PICTURES
        "video/mp4" -> Environment.DIRECTORY_MOVIES
        else -> Environment.DIRECTORY_DOWNLOADS
    }
    Environment.getExternalStoragePublicDirectory(environment)?.let { parent ->
        try {
            val file = checkAndCreateUniqueFile(parent, name)
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
            e.printStackTrace()
            return null
        }
    }
    return null
}

fun checkAndCreateUniqueFile(parent: File, name: String): File {
    val nameWithoutExt = name.substringBeforeLast(".")
    val ext = name.substringAfterLast(".", "")
    var counter = 0

    var newFile = File(parent, name)
    while (newFile.exists()) {
        counter++
        val newName = if (ext.isNotEmpty())
            "$nameWithoutExt($counter).$ext"
        else
            "$nameWithoutExt($counter)"
        newFile = File(parent, newName)
    }
    return newFile
}