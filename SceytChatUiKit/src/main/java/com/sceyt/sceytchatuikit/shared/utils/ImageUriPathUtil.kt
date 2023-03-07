package com.sceyt.sceytchatuikit.shared.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.*
import java.net.URLEncoder

object ImageUriPathUtil {

    fun getRealPathFromURI(context: Context, contentURI: Uri): String {
        val result: String?
        val cursor = context.contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) {
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result.toString()
    }

    fun getUriFromPath(context: Context, filePath: String): Uri? {
        val photoId: Long
        val photoUri = MediaStore.Images.Media.getContentUri("external")
        val projection = arrayOf(MediaStore.Images.ImageColumns._ID)
        val cursor = context.contentResolver.query(photoUri, projection, MediaStore.Images.ImageColumns.DATA + " LIKE ?", arrayOf(filePath), null)!!
        cursor.moveToFirst()
        val columnIndex = cursor.getColumnIndex(projection[0])
        photoId = cursor.getLong(columnIndex)
        cursor.close()
        return Uri.parse("$photoUri/$photoId")
    }

    fun getContentFileName(context: Context?, uri: Uri?): String {
        var mimeType: String? = null
        if (context != null && uri != null)
            mimeType = context.contentResolver.getType(uri)
        val filename = if (mimeType == null) {
            val path = getPath(context!!, uri!!)
            if (path == null) {
                getName(uri.toString())
            } else {
                val file = File(path)
                file.name
            }
        } else {
            @SuppressLint("Recycle") val returnCursor = context!!.contentResolver.query(uri!!, null, null, null, null)
            val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            returnCursor.getString(nameIndex)
        }
        return filename
    }

    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) { // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("filePath" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) { // Return the remote address
            if (isGooglePhotosUri(uri)) {
                try {
                    val `is` = context.contentResolver.openInputStream(uri)
                    if (`is` != null) {
                        val pictureBitmap = BitmapFactory.decodeStream(`is`)
                        return getImageUriFromBitmap(context, pictureBitmap).path
                    }
                } catch (ignored: FileNotFoundException) {
                }
            }
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    fun creteNewUriFromGooglePhoto(context: Context, uri: Uri?): Uri? {
        try {
            val `is` = context.contentResolver.openInputStream(uri!!)
            if (`is` != null) {
                val pictureBitmap = BitmapFactory.decodeStream(`is`)
                return getImageUriFromBitmap(context, pictureBitmap)
            }
        } catch (ignored: FileNotFoundException) {
        }
        return null
    }

    private fun getImageUriFromBitmap(context: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    private fun getName(filename: String?): String {
        return if (filename == null) {
            ""
        } else {
            val index = indexOfLastSeparator(filename)
            filename.substring(index + 1)
        }
    }

    private fun indexOfLastSeparator(filename: String?): Int {
        return if (filename == null) {
            -1
        } else {
            val lastUnixPos = filename.lastIndexOf(47.toChar())
            val lastWindowsPos = filename.lastIndexOf(92.toChar())
            Math.max(lastUnixPos, lastWindowsPos)
        }
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        val column = "_data"
        val projection = arrayOf(column)
        context.contentResolver.query(uri!!, projection, selection, selectionArgs, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        }
        return null
    }

    @SuppressLint("DefaultLocale")
    fun getMimeType(url: String): String? {
        var url1 = url
        var type: String? = null
        url1 = url1.replace(" ", "")
        var extension: String? = null
        try {
            extension = MimeTypeMap.getFileExtensionFromUrl(URLEncoder.encode(url1, "UTF-8")).lowercase()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        if (extension != null) {
            try {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            } catch (ignored: NullPointerException) {
            }
        }
        return type
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return uri.toString().startsWith("content://com.google.android.apps.photos.content")
    }

    @Throws(IOException::class)
    fun copyFile(context: Context, uri: String, name: String): File {
        val file = File(uri)
        if (file.exists()) return file
        return File(context.cacheDir, name)
            .apply {
                if (!exists()) {
                    outputStream().use { cache ->
                        context.contentResolver.openInputStream(Uri.parse(uri)).use { inputStream ->
                            inputStream?.copyTo(cache)
                        }
                    }
                }
            }
    }
}