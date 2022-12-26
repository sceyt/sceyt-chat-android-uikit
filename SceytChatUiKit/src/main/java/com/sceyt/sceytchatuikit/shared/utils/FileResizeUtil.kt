package com.sceyt.sceytchatuikit.shared.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.bitmapToByteArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.roundToInt

object FileResizeUtil {

    fun resizeAndCompressImage(context: Context, filePath: String, fileName: String,
                               reqSize: Int = 800, reqWith: Int = reqSize, reqHeight: Int = reqSize): File {
        val initialSize = getImageSize(Uri.parse(filePath))
        var bmpPic = BitmapFactory.decodeFile(filePath, BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(initialSize, reqWith, reqHeight)
        })

        try {
            bmpPic = getResizedBitmap(bitmap = bmpPic, filePath)
            val bmpFile = FileOutputStream(context.cacheDir.toString() + fileName)
            bmpPic.compress(Bitmap.CompressFormat.JPEG, 80, bmpFile)
            bmpFile.flush()
            bmpFile.close()
        } catch (e: java.lang.Exception) {
            Log.i(TAG, e.message.toString())
        }
        return File(context.cacheDir.toString() + fileName)
    }

    fun getImageSize(image: Uri): Size {
        val input = FileInputStream(image.path)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(input, null, options)
        return Size(options.outWidth, options.outHeight)
    }

    fun getVideoSize(path: String): Size {
        val metaRetriever = MediaMetadataRetriever()
        metaRetriever.setDataSource(path)
        val height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                ?: 0
        val width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                ?: 0
        return Size(width, height)
    }

    fun getVideoDuration(context: Context, path: String): Long? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.parse(path))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMilliSec = time?.toLongOrNull()
        retriever.release()
        return timeInMilliSec
    }

    fun getImageThumbByUrlAsByteArray(url: String, maxImageSize: Float): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeFile(url)
            createThumbFromBitmap(bitmap, maxImageSize).bitmapToByteArray()
        } catch (ex: Exception) {
            null
        }
    }

    fun getVideoThumbByUrlAsByteArray(url: String, maxImageSize: Float): ByteArray? {
        return try {
            val bitmap = MediaMetadataRetriever().apply {
                setDataSource(url)
            }.getFrameAtTime(1000)
            createThumbFromBitmap(bitmap ?: return null, maxImageSize).bitmapToByteArray()
        } catch (ex: Exception) {
            null
        }
    }

    fun scaleDownBitmap(realImage: Bitmap, maxImageSize: Float): Bitmap {
        val ratio = (maxImageSize / realImage.width).coerceAtMost(maxImageSize / realImage.height)
        val width = (ratio * realImage.width).roundToInt()
        val height = (ratio * realImage.height).roundToInt()
        return Bitmap.createScaledBitmap(realImage, width, height, true)
    }

    fun createThumbFromBitmap(realImage: Bitmap, maxImageSize: Float): Bitmap {
        val ratio = (maxImageSize / realImage.width).coerceAtMost(maxImageSize / realImage.height)
        val width = (ratio * realImage.width).roundToInt()
        val height = (ratio * realImage.height).roundToInt()
       return ThumbnailUtils.extractThumbnail(realImage, width, height)
    }

    private fun getResizedBitmap(bitmap: Bitmap, filePath: String): Bitmap {
        val matrix = Matrix()
        val rotationAngle = getFileOrientation(imagePath = filePath)
        if (rotationAngle != 0)
            matrix.setRotate(rotationAngle.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun createFileFromBitmap(context: Context, bitmap: Bitmap, fileName: String): File? {
        return try {
            val bmpFile = FileOutputStream(context.cacheDir.toString() + fileName)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bmpFile)
            bmpFile.flush()
            bmpFile.close()
            File(context.cacheDir.toString() + fileName)

        } catch (e: java.lang.Exception) {
            null
        }
    }

    private fun calculateInSampleSize(size: Size, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = size.run { height to width }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight || halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getFileOrientation(imagePath: String): Int {
        var rotate = 0
        try {
            val exif = ExifInterface(imagePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
                ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
                ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rotate
    }

    fun checkIfImagePathIsLocale(path: String?): Boolean {
        if (path != null) {
            val file = File(path)
            return file.exists()
        }
        return false
    }

    fun getFileSizeMb(context: Context, uri: Uri): Double {
        return ImageUriPathUtil.getPath(context, uri)?.let {
            File(it).length() / 1024.0 / 1024.0
        } ?: 0.0
    }
}