package com.sceyt.sceytchatuikit.extensions

import android.graphics.*
import android.util.Base64
import androidx.core.graphics.scale
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

fun getBitmapFromUrl(imageUrl: String?, isCircleImage: Boolean = true): Bitmap? {
    return try {
        val url = URL(imageUrl)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input: InputStream = connection.inputStream
        if (isCircleImage)
            BitmapFactory.decodeStream(input).getCircleBitmap()
        else BitmapFactory.decodeStream(input)
    } catch (e: Exception) {
        null
    }
}

fun Bitmap.getCircleBitmap(): Bitmap? {
    val output: Bitmap
    val srcRect: Rect
    val dstRect: Rect
    val r: Float
    if (width > height) {
        output = Bitmap.createBitmap(height, height, Bitmap.Config.ARGB_8888)
        val left = (width - height) / 2
        val right = left + height
        srcRect = Rect(left, 0, right, height)
        dstRect = Rect(0, 0, height, height)
        r = height / 2.toFloat()
    } else {
        output = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        val top = (height - width) / 2
        val bottom = top + width
        srcRect = Rect(0, top, width, bottom)
        dstRect = Rect(0, 0, width, width)
        r = width / 2.toFloat()
    }
    val canvas = Canvas(output)
    val color = -0xbdbdbe
    val paint = Paint()
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawCircle(r, r, r, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, srcRect, dstRect, paint)
    try {
        recycle()
    } catch (ex: java.lang.Exception) {
    }
    return output
}

fun Bitmap.setCircleBitmap(size: Int): Bitmap {
    val bitmap1: Bitmap = cropBitmap().scale(size, size, false)
    val output = Bitmap.createBitmap(bitmap1.width, bitmap1.height, Bitmap.Config.ARGB_8888)
    val radius = bitmap1.width / 2
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap1, 0f, 0f, paint)
    return output
}

private fun Bitmap.cropBitmap(): Bitmap {
    return if (width >= height) {
        Bitmap.createBitmap(this, width / 2 - height / 2, 0, height, height)
    } else {
        Bitmap.createBitmap(this, 0, height / 2 - width / 2, width, width)
    }
}

fun Bitmap.scaleBitmap(realImage: Bitmap, maxImageSize: Float): Bitmap {
    val ratio = (maxImageSize / realImage.width).coerceAtMost(maxImageSize / realImage.height)
    val width = (ratio * realImage.width).roundToInt()
    val height = (ratio * realImage.height).roundToInt()
    return Bitmap.createScaledBitmap(realImage, width, height, false)
}

fun convertString64ToImage(base64String: String): ByteArray? {
    return Base64.decode(base64String, Base64.DEFAULT)
}

fun ByteArray.decodeByteArrayToBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size)
}