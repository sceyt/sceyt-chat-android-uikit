package com.sceyt.sceytchatuikit.shared.utils

import android.graphics.Bitmap

object BitmapUtil {

    fun bitmapToRgba(bitmap: Bitmap): ByteArray {
        require(bitmap.config == Bitmap.Config.ARGB_8888) { "Bitmap must be in ARGB_8888 format" }
        val pixels = IntArray(bitmap.width * bitmap.height)
        val bytes = ByteArray(pixels.size * 4)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var i = 0
        for (pixel in pixels) {
            // Get components assuming is ARGB
            val a = pixel shr 24 and 0xff
            val r = pixel shr 16 and 0xff
            val g = pixel shr 8 and 0xff
            val b = pixel and 0xff
            bytes[i++] = r.toByte()
            bytes[i++] = g.toByte()
            bytes[i++] = b.toByte()
            bytes[i++] = a.toByte()
        }
        return bytes
    }

    fun bitmapFromRgba(width: Int, height: Int, bytes: ByteArray): Bitmap? {
        val pixels = IntArray(bytes.size / 4)
        var j = 0
        for (i in pixels.indices) {
            val r = bytes[j++].toInt() and 0xff
            val g = bytes[j++].toInt() and 0xff
            val b = bytes[j++].toInt() and 0xff
            val a = bytes[j++].toInt() and 0xff
            val pixel = a shl 24 or (r shl 16) or (g shl 8) or b
            pixels[i] = pixel
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}