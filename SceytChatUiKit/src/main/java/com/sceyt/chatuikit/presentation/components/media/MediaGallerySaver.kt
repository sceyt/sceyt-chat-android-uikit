package com.sceyt.chatuikit.presentation.components.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.OsConstants
import com.sceyt.chatuikit.extensions.saveToGallery
import java.io.File
import java.io.IOException

internal sealed interface GallerySaveResult {
    data class Saved(val uri: Uri) : GallerySaveResult
    data object StorageFull : GallerySaveResult
    data object Failed : GallerySaveResult
}

internal object MediaGallerySaver {

    fun save(context: Context, path: String, name: String, mimeType: String): GallerySaveResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val file = saveToGallery(context, path, name, mimeType) ?: return GallerySaveResult.Failed
            return GallerySaveResult.Saved(Uri.fromFile(file))
        }

        val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
        val (collection, directory) = when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.getContentUri(volume) to Environment.DIRECTORY_PICTURES
            mimeType.startsWith("video/") -> MediaStore.Video.Media.getContentUri(volume) to Environment.DIRECTORY_MOVIES
            else -> MediaStore.Downloads.getContentUri(volume) to Environment.DIRECTORY_DOWNLOADS
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        var pendingUri: Uri? = null
        return try {
            val uri = File(path).inputStream().use { input ->
                val destination = resolver.insert(collection, values) ?: return GallerySaveResult.Failed
                pendingUri = destination
                resolver.openOutputStream(destination)?.use { input.copyTo(it) } ?: return GallerySaveResult.Failed
                destination
            }
            val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            if (resolver.update(uri, published, null, null) == 0) return GallerySaveResult.Failed
            pendingUri = null
            GallerySaveResult.Saved(uri)
        } catch (e: IOException) {
            if (e.isStorageFull()) GallerySaveResult.StorageFull else GallerySaveResult.Failed
        } catch (_: SecurityException) {
            GallerySaveResult.Failed
        } catch (_: IllegalArgumentException) {
            GallerySaveResult.Failed
        } finally {
            pendingUri?.let { runCatching { resolver.delete(it, null, null) } }
        }
    }

    private fun Throwable.isStorageFull(): Boolean = generateSequence(this) { it.cause }.any {
        (it is ErrnoException && it.errno == OsConstants.ENOSPC) || it.message?.contains("ENOSPC") == true
    }
}
