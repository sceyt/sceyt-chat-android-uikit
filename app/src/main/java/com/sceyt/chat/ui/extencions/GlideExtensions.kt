package com.sceyt.chat.ui.extencions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import java.io.File

fun getImageBitmapWithGlide(context: Context, url: String?, size: Int = 0, callback: (bitmap: Bitmap?) -> Unit) {
    Glide.with(context)
        .asBitmap()
        .apply {
            if (size > 0)
                override(size)
        }
        .load(url)
        .into(glideCustomTarget(onResourceReady = { resource, _ ->
            callback.invoke(resource)
        }))

}

fun Context.downloadOnlyWithGlide(url: String?, endListener: (() -> Unit)? = null): FutureTarget<File> {
    return Glide.with(this)
        .downloadOnly()
        .diskCacheStrategy(DiskCacheStrategy.DATA)
        .load(url)
        .listener(glideRequestListener { endListener?.invoke() })
        .submit()
}

inline fun <T> glideCustomTarget(
        crossinline onLoadCleared: (placeholder: Drawable?) -> Unit = { _ -> },
        crossinline onResourceReady: (resource: T, transition: Transition<in T>?) -> Unit = { _, _ -> },
        crossinline onFinish: (placeholder: Bitmap?) -> Unit = { _ -> },
): CustomTarget<T> {
    return object : CustomTarget<T>() {
        override fun onLoadCleared(placeholder: Drawable?) {
            onLoadCleared.invoke(placeholder)
            onFinish.invoke(placeholder?.toBitmap())
        }

        override fun onResourceReady(resource: T, transition: Transition<in T>?) {
            onResourceReady.invoke(resource, transition)
            onFinish.invoke(if (resource is Bitmap) resource else null)
        }
    }
}


inline fun <T> glideRequestListener(
        crossinline onLoadFailed: (e: GlideException?) -> Unit = { },
        crossinline onResourceReady: () -> Unit = { },
        crossinline onFinish: (Boolean) -> Unit = { }): RequestListener<T> {
    return object : RequestListener<T> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<T>?, isFirstResource: Boolean): Boolean {
            onLoadFailed.invoke(e)
            onFinish.invoke(false)
            return false
        }

        override fun onResourceReady(resource: T?, model: Any?, target: Target<T>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            onResourceReady.invoke()
            onFinish.invoke(true)
            return false
        }
    }
}