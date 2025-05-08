package com.sceyt.chatuikit.presentation.helpers

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.data.constants.SceytConstants.AvatarCacheFilesDirName
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.extensions.applyError
import com.sceyt.chatuikit.presentation.extensions.applyPlaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * AvatarImageLoader handles loading and caching of avatar images.
 * It uses Glide for efficient memory and disk caching to ensure images
 * are available offline when needed.
 */
@Suppress("unused")
object AvatarImageLoader {
    private val scope by lazy { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    private val messageDigest by lazy { MessageDigest.getInstance("MD5") }

    /**
     * Loads an avatar image with built-in caching. The image will be available offline
     * if the user has previously loaded it and hasn't cleared the cache.
     *
     * @param context Application context for Glide
     * @param imageUrl URL of the avatar image to load
     * @param imageView Target ImageView to load the image into
     * @param placeholder Optional placeholder to show while loading
     * @param errorPlaceholder Optional placeholder to show on error
     * @param loadCallback Optional callback for load state events
     * @param preloadForOffline Whether to save the image to custom cache directory for persistent storage
     */
    fun loadAvatar(
            context: Context,
            imageUrl: String?,
            imageView: ImageView,
            placeholder: AvatarView.AvatarPlaceholder? = null,
            errorPlaceholder: AvatarView.AvatarErrorPlaceHolder? = null,
            loadCallback: ((loading: Boolean) -> Unit)? = null,
            preloadForOffline: Boolean = true
    ) {
        if (imageUrl.isNullOrBlank()) {
            loadCallback?.invoke(false)
            return
        }

        loadCallback?.invoke(true)

        // Check if the image exists in our custom cache
        val cachedFile = getCachedImageFile(context, imageUrl)
        val imageToLoad = if (cachedFile?.exists() == true) cachedFile else imageUrl

        Glide.with(context.applicationContext)
            .load(imageToLoad)
            .override(imageView.width.takeIf { it > 0 } ?: 200)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .applyPlaceHolder(placeholder)
            .applyError(errorPlaceholder)
            .transition(DrawableTransitionOptions.withCrossFade(100))
            .listener(glideRequestListener(
                onResourceReady = { resource, model, _, _, _ ->
                    // If we loaded from URL (not our cached file) and caching is not done yet, save to our custom cache
                    if (preloadForOffline && model == imageUrl && (cachedFile == null || !cachedFile.exists())) {
                        saveImageToCache(context, imageUrl)
                    }
                },
                onFinish = {
                    loadCallback?.invoke(false)
                }
            ))
            .into(imageView)
    }

    /**
     * Checks if an avatar image exists in our custom cache
     *
     * @param context Application context
     * @param imageUrl URL of the avatar to check
     * @return true if the image exists in cache, false otherwise
     */
    fun isImageCached(context: Context, imageUrl: String?): Boolean {
        if (imageUrl.isNullOrBlank()) return false

        val cachedFile = getCachedImageFile(context, imageUrl)
        return cachedFile?.exists() == true
    }

    /**
     * Gets the cached file for an avatar image if it exists in our custom cache
     *
     * @param context Application context
     * @param imageUrl URL of the avatar
     * @return The cached File or null if not cached
     */
    fun getCachedImageFile(context: Context, imageUrl: String?): File? {
        if (imageUrl.isNullOrBlank()) return null

        val cacheDir = getAvatarCacheDir(context)
        val fileName = generateFileNameForUrl(imageUrl)
        val file = File(cacheDir, fileName)

        return if (file.exists()) file else null
    }

    /**
     * Gets the file path for an avatar image URL whether it exists or not
     * This is useful when you need to know where a file would be stored
     * without checking if it exists.
     *
     * @param context Application context
     * @param imageUrl URL of the avatar
     * @return The File object representing where the avatar would be cached
     */
    fun getFilePathForUrl(context: Context, imageUrl: String?): File? {
        if (imageUrl.isNullOrBlank()) return null

        val cacheDir = getAvatarCacheDir(context)
        val fileName = generateFileNameForUrl(imageUrl)
        return File(cacheDir, fileName)
    }

    /**
     * Saves an image to our custom cache directory
     *
     * @param context Application context
     * @param imageUrl URL of the image (used to generate filename)
     * @param bitmap Optional bitmap to save directly. If null, will be downloaded.
     */
    private fun saveImageToCache(context: Context, imageUrl: String, bitmap: Bitmap? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val cacheDir = getAvatarCacheDir(context)
                val fileName = generateFileNameForUrl(imageUrl)
                val outputFile = File(cacheDir, fileName)

                // If file already exists, don't re-download
                if (outputFile.exists()) return@launch

                val imageToSave = bitmap ?: try {
                    Glide.with(context.applicationContext)
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@launch
                }

                FileOutputStream(outputFile).use { out ->
                    imageToSave.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    out.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Gets or creates the custom avatar cache directory
     */
    private fun getAvatarCacheDir(context: Context): File {
        val cacheDir = File(context.filesDir, AvatarCacheFilesDirName)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    /**
     * Generates a unique file name for a URL using MD5 hash
     */
    private fun generateFileNameForUrl(url: String): String {
        val digest = messageDigest.digest(url.toByteArray())
        val hexString = StringBuilder()

        for (b in digest) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }

        return "$hexString.jpeg"
    }

    /**
     * Clears specific avatar from custom cache
     *
     * @param context Application context
     * @param imageUrl URL of the avatar to remove
     */
    fun clearFromCache(context: Context, imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) return

        scope.launch(Dispatchers.IO) {
            try {
                getCachedImageFile(context, imageUrl)?.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Clears all avatars from the custom cache
     *
     * @param context Application context
     */
    fun clearAllCache(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val cacheDir = getAvatarCacheDir(context)
                cacheDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}