package com.sceyt.chat.ui.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.*

object FileCompressorUtil {

    fun compress(context: Context, uri: Uri): File? {
        if (!uri.path.isNullOrBlank()) {
            val tmpFile = File(context.cacheDir, getFileName(context, uri))
            if (tmpFile.isDirectory)
                tmpFile.delete()
            return context.contentResolver.openInputStream(uri)?.let { compress(it, tmpFile) }
        }
        return null
    }

    private fun compress(originalStream: InputStream, tmpFile: File): File {
        val fout: OutputStream = FileOutputStream(tmpFile)
        val out = BufferedOutputStream(fout)
        val gzOut = GzipCompressorOutputStream(out)
        val buffer = ByteArray(1024)
        var n = 0
        while (-1 != originalStream.read(buffer).also { n = it }) {
            gzOut.write(buffer, 0, n)
        }
        gzOut.close()
        originalStream.close()

        return tmpFile
    }

    fun getMimeType(url: String?): String? {
        if (url.isNullOrBlank()) return null
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type?.takeWhile { it != '/' }
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
}