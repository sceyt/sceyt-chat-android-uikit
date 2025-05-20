package com.sceyt.chatuikit.shared.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.extensions.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


object FilePathUtil {

    fun getFilePathFromUri(
            context: Context,
            parentDirToCopy: File,
            uri: Uri,
    ): String? {
        // If it's a file URI, just return the path directly
        if ("file" == uri.scheme) {
            return uri.path
        }

        // Handle content URIs
        if ("content" == uri.scheme) {
            val projection = arrayOf(
                FileColumns._ID,
                FileColumns.DATA,
                FileColumns.DATE_ADDED,
                FileColumns.MEDIA_TYPE,
                FileColumns.DISPLAY_NAME,
                FileColumns.SIZE,
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(FileColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(FileColumns.SIZE)
                    val fileName = it.getString(nameIndex)
                    val fileSize = it.getLong(sizeIndex)

                    val directory = File(parentDirToCopy, SceytConstants.CopyFileDirName)
                    val file = getOrCreateUniqueFileDirectory(directory, fileName, fileSize)

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        copyStreamToFile(inputStream, file)
                        return file.absolutePath
                    }
                }
            }
        }

        return null
    }

    fun getOrCreateUniqueFileDirectory(rootDir: File, fileName: String, fileSize: Long): File {
        // Create the directory if it doesn't exist
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        val file = File(rootDir, fileName)
        // After adding tid to Attachment, we can check [fileSize] to avoid copying the same file.
        // Now we cant do that because we need unique fileName for each attachment.
        Log.i(TAG, "getUniqueFileDirectory: file: $file, fileSize: $fileSize")
        if (!file.exists() /*|| file.length() == fileSize*/) {
            runCatching {
                file.createNewFile()
            }.onSuccess {
                return file
            }
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