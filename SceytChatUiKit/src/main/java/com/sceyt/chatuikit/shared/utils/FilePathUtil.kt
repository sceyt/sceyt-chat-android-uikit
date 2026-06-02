package com.sceyt.chatuikit.shared.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
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
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            return uri.path
        }

        // Handle content URIs
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
            val resolver = context.contentResolver

            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    var fileName = cursor.getStringOrNull(nameIndex)
                        ?.trim()
                        ?.trimEnd('.')
                        ?.takeUnless { it.isBlank() } ?: "file_${System.currentTimeMillis()}"


                    val currentExtension = fileName.substringAfterLast('.', "")

                    if (currentExtension.isBlank()) {
                        val mimeType = resolver.getType(uri)
                        val extension = MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(mimeType)

                        if (!extension.isNullOrBlank()) {
                            fileName += ".$extension"
                        }
                    }

                    Log.d(TAG, "File info - name: $fileName")

                    val directory = File(parentDirToCopy, SceytConstants.CopyFileDirName)
                    val file = getOrCreateUniqueFileDirectory(directory, fileName)

                    resolver.openInputStream(uri)?.use { inputStream ->
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
                Log.d(
                    TAG,
                    "Successfully created file: ${file.absolutePath} (attempts: ${counter + 1})"
                )
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