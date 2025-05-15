package com.sceyt.chatuikit.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.time.Duration.Companion.seconds

fun getImageBitmapWithGlide(context: Context, url: String?,
                            successCb: (bitmap: Bitmap) -> Unit,
                            errorCb: () -> Unit = {}) {
    Glide.with(context)
        .asBitmap()
        .load(url)
        .into(glideCustomTarget(onResourceReady = { resource, _ ->
            successCb.invoke(resource)
        }, onLoadCleared = {
            errorCb.invoke()
        }))
}

suspend fun getImageBitmapWithGlideWithTimeout(context: Context, url: String?,
                                               timeout: Long = 4.seconds.inWholeMilliseconds): Bitmap? {
    return withContext(Dispatchers.IO) {
        withTimeoutOrNull(timeout) {
            try {
                withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(url)
                        .submit()
                        .get()
                }
            } catch (e: Exception) {
                null // Handle the exception as needed
            }
        }
    }
}

fun Context.downloadOnlyWithGlide(url: String?, endListener: (() -> Unit)? = null): FutureTarget<File> {
    return Glide.with(this)
        .downloadOnly()
        .diskCacheStrategy(DiskCacheStrategy.DATA)
        .load(url)
        .listener(glideRequestListener { endListener?.invoke() })
        .submit()
}

inline fun <reified T : Any> glideCustomTarget(
        crossinline onLoadCleared: (placeholder: Drawable?) -> Unit = { _ -> },
        crossinline onResourceReady: (resource: T, transition: Transition<in T>?) -> Unit = { _, _ -> },
        crossinline onFinish: (placeholder: T?) -> Unit = { _ -> },
): CustomTarget<T> {
    return object : CustomTarget<T>() {
        override fun onLoadCleared(placeholder: Drawable?) {
            onLoadCleared.invoke(placeholder)
            onFinish.invoke(placeholder as? T)
        }

        override fun onResourceReady(resource: T, transition: Transition<in T>?) {
            onResourceReady.invoke(resource, transition)
            onFinish.invoke(resource)
        }
    }
}


inline fun <T : Any> glideRequestListener(
        crossinline onLoadFailed: (e: GlideException?) -> Unit = { },
        crossinline onResourceReady: (T, Any, Target<T>?, DataSource, Boolean) -> Unit = { _, _, _, _, _ -> },
        crossinline onFinish: (Boolean) -> Unit = { }): RequestListener<T> {
    return object : RequestListener<T> {

        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<T>, isFirstResource: Boolean): Boolean {
            onLoadFailed.invoke(e)
            onFinish.invoke(false)
            return false
        }

        override fun onResourceReady(resource: T, model: Any, target: Target<T>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
            onResourceReady.invoke(resource, model, target, dataSource, isFirstResource)
            onFinish.invoke(true)
            return false
        }
    }
}