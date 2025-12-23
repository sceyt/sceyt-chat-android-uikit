package com.sceyt.chatuikit.shared.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import com.sceyt.chatuikit.data.constants.SceytConstants
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


object FilePathUtil {
    private const val TAG = "FilePathUtilTag"

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
                    val fileName = it.getString(nameIndex)

                    val directory = File(parentDirToCopy, SceytConstants.CopyFileDirName)
                    val file = getOrCreateUniqueFileDirectory(directory, fileName)

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        copyStreamToFile(inputStream, file)
                        return file.absolutePath
                    }
                }
            }
        }

        return null
    }

    @Synchronized
    fun getOrCreateUniqueFileDirectory(rootDir: File, fileName: String): File {
        Log.d(TAG, "getOrCreateUniqueFileDirectory: rootDir=$rootDir, fileName=$fileName")
        
        // Ensure root directory exists
        rootDir.mkdirs()

        var counter = 0
        while (true) {
            val targetDir = if (counter == 0)
                rootDir else File(rootDir, counter.toString())

            if (!targetDir.exists()) {
                Log.d(TAG, "Creating directory: $targetDir")
                targetDir.mkdirs()
            }

            val file = File(targetDir, fileName)

            // Try atomic creation — safe across threads & external processes
            val created = runCatching {
                file.createNewFile()
            }.getOrDefault(false)

            if (created) {
                Log.d(TAG, "Successfully created file: ${file.absolutePath} (attempts: ${counter + 1})")
                return file // Successfully created — return it
            }

            Log.d(TAG, "File already exists, trying next directory (counter=$counter)")
            counter++
        }
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