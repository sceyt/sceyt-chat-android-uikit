package com.sceyt.chatuikit.shared.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.sceyt.chatuikit.extensions.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


object FilePathUtil {

    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        // If it's a file URI, just return the path directly
        if ("file" == uri.scheme) {
            return uri.path
        }

        // Handle content URIs
        if ("content" == uri.scheme) {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    val fileName = it.getString(nameIndex)
                    val fileSize = it.getLong(sizeIndex)

                    val directory = File(context.cacheDir, "copied_files")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val file = getUniqueFileDirectory(directory, fileName, fileSize)

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        copyStreamToFile(inputStream, file)
                        return file.absolutePath
                    }
                }
            }
        }

        return null
    }

    private fun getUniqueFileDirectory(rootDir: File, fileName: String, fileSize: Long): File {
        val file = File(rootDir, fileName)
        // After adding tid to Attachment, we can check [fileSize] to avoid copying the same file.
        // Now we cant do that because we need unique fileName for each attachment.
        Log.i(TAG, "getUniqueFileDirectory: file: $file, fileSize: $fileSize")
        if (!file.exists() /*|| file.length() == fileSize*/) {
            return file
        }
        var counter = 1
        var newRoot = File(rootDir, "/$counter")
        while (File(newRoot, fileName).exists()) {
            newRoot = File(rootDir, "/$counter")
            counter++
        }
        // Create the directory if it doesn't exist
        if (!newRoot.exists()) {
            newRoot.mkdirs()
        }

        return File(newRoot, fileName)
    }

    private fun copyStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { outputStream ->
            val buffer = ByteArray(4 * 1024) // 4 KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
        }
    }
}