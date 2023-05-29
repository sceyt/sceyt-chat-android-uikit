package com.sceyt.sceytchatuikit.shared.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import com.sceyt.sceytchatuikit.extensions.bitmapToByteArray
import com.sceyt.sceytchatuikit.extensions.getFileSizeMb
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

object FileResizeUtil {

    fun resizeAndCompressImage(context: Context, filePath: String,
                               reqSize: Int = 800, reqWith: Int = reqSize, reqHeight: Int = reqSize): File? {
        if (filePath.isBlank()) return null
        return try {
            val initialSize = getImageDimensionsSize(Uri.parse(filePath))
            if (initialSize.width == -1 || initialSize.height == -1) return null

            val inSimpleSize = calculateInSampleSize(initialSize, reqWith, reqHeight)
            val quality = calculateQuality(filePath, inSimpleSize)

            var bmpPic = BitmapFactory.decodeFile(filePath, BitmapFactory.Options().apply {
                inSampleSize = inSimpleSize
            })
            val dest = "${context.cacheDir}/" + UUID.randomUUID() + ".JPEG"

            bmpPic = getOrientationCorrectedBitmap(bitmap = bmpPic, filePath)
            val bmpFile = FileOutputStream(dest)
            bmpPic.compress(Bitmap.CompressFormat.JPEG, quality, bmpFile)
            bmpFile.flush()
            bmpFile.close()
            File(dest)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resizeAndCompressImageAsByteArray(filePath: String,
                                          reqSize: Int = 800, reqWith: Int = reqSize, reqHeight: Int = reqSize): ByteArray? {
        if (filePath.isBlank()) return null
        return try {
            val initialSize = getImageDimensionsSize(Uri.parse(filePath))
            if (initialSize.width == -1 || initialSize.height == -1) return null

            val inSimpleSize = calculateInSampleSize(initialSize, reqWith, reqHeight)
            val quality = calculateQuality(filePath, inSimpleSize)

            var bmpPic = BitmapFactory.decodeFile(filePath, BitmapFactory.Options().apply {
                inSampleSize = inSimpleSize
            })

            bmpPic = getOrientationCorrectedBitmap(bitmap = bmpPic, filePath)
            val bmpFile = ByteArrayOutputStream()
            bmpPic.compress(Bitmap.CompressFormat.JPEG, quality, bmpFile)
            bmpFile.flush()
            bmpFile.close()
            bmpFile.toByteArray()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resizeAndCompressImageAsByteArray(bitmap: Bitmap,
                                          reqSize: Int = 800, reqWith: Int = reqSize, reqHeight: Int = reqSize): Bitmap? {
        return try {
            val initialSize = Size(bitmap.width, bitmap.height)
            val byteArray = bitmap.bitmapToByteArray()

            val bmpPic = BitmapFactory.decodeByteArray(byteArray, 0, byteArray?.size
                    ?: return null, BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(initialSize, reqWith, reqHeight)
            })

            val bmpFile = ByteArrayOutputStream()
            bmpPic.compress(Bitmap.CompressFormat.JPEG, 80, bmpFile)
            bmpFile.flush()
            bmpFile.close()
            bmpPic
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeAndCompressImageAsFile(context: Context, bitmap: Bitmap,
                                             reqSize: Int = 800, reqWith: Int = reqSize, reqHeight: Int = reqSize): File? {
        return try {
            val initialSize = Size(bitmap.width, bitmap.height)
            val byteArray = bitmap.bitmapToByteArray()
            val bmpPic = BitmapFactory.decodeByteArray(byteArray, 0, byteArray?.size
                    ?: return null, BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(initialSize, reqWith, reqHeight)
            })

            val dest = "${context.cacheDir}/" + UUID.randomUUID() + ".JPEG"
            val bmpFile = FileOutputStream(dest)
            bmpPic.compress(Bitmap.CompressFormat.JPEG, 80, bmpFile)
            bmpFile.flush()
            bmpFile.close()
            File(dest)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageDimensionsSize(image: Uri): Size {
        val input = FileInputStream(image.path)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(input, null, options)
        return Size(options.outWidth, options.outHeight)
    }

    fun getImageSizeOriented(uri: Uri): Size {
        var size = Size(0, 0)
        try {
            size = getImageDimensionsSize(uri)
            val exif = ExifInterface(uri.path ?: return size)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_ROTATE_90 ->
                    size = Size(size.height, size.width)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
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

    fun getVideoSizeOriented(path: String): Size {
        val metaRetriever = MediaMetadataRetriever()
        metaRetriever.setDataSource(path)
        val rotation = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
                ?: 0
        val height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                ?: 0
        val width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                ?: 0
        if (rotation == 90 || rotation == 270)
            return Size(height, width)

        return Size(width, height)
    }

    fun getVideoDuration(context: Context, path: String): Long? {
        val timeInMilliSec: Long? = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(path))
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        return timeInMilliSec
    }

    fun getImageThumbByUrlAsByteArray(url: String, maxImageSize: Float): ByteArray? {
        return try {
            resizeAndCompressImageAsByteArray(url, reqSize = maxImageSize.roundToInt())
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    fun getVideoThumbByUrlAsByteArray(url: String, maxImageSize: Float): ByteArray? {
        return try {
            getVideoThumb(url, maxImageSize).bitmapToByteArray()
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    fun getVideoThumb(url: String, maxImageSize: Float): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val bitmap = retriever.apply {
                setDataSource(url)
            }.getFrameAtTime(1000)
            resizeAndCompressImageAsByteArray(bitmap ?: return null, reqSize = maxImageSize.toInt())
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    fun getVideoThumbAsFile(context: Context, url: String, maxImageSize: Float): File? {
        val retriever = MediaMetadataRetriever()
        return try {
            val bitmap = retriever.apply {
                setDataSource(url)
            }.getFrameAtTime(1000)
            resizeAndCompressImageAsFile(context, bitmap
                    ?: return null, reqSize = maxImageSize.toInt())
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    fun getImageThumbAsFile(context: Context, url: String, maxImageSize: Float): File? {
        return try {
            resizeAndCompressImage(context, url, reqSize = maxImageSize.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getOrientationCorrectedBitmap(bitmap: Bitmap, filePath: String): Bitmap {
        val matrix = Matrix()
        val rotationAngle = getFileOrientation(imagePath = filePath)
        return if (rotationAngle != 0) {
            matrix.setRotate(rotationAngle.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
    }

    fun getOrientationCorrectedBitmap(bitmap: Bitmap, byteArray: ByteArray): Bitmap {
        val matrix = Matrix()
        val rotationAngle = getFileOrientation(ByteArrayInputStream(byteArray))
        return if (rotationAngle != 0) {
            matrix.setRotate(rotationAngle.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
    }

    fun createFileFromBitmap(context: Context, bitmap: Bitmap): File? {
        return try {
            val fileDest = "${context.cacheDir}/" + UUID.randomUUID() + ".JPEG"
            val bmpFile = FileOutputStream(fileDest)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bmpFile)
            bmpFile.flush()
            bmpFile.close()
            bitmap.recycle()
            File(fileDest)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(size: Size, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = size.run { height to width }

        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
            val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()

            inSampleSize = min(heightRatio, widthRatio)
        }
        return inSampleSize
    }

    private fun calculateQuality(filePath: String, isSimpleSize: Int): Int {
        return if (isSimpleSize > 1)
            80
        else {
            if (getFileSizeMb(filePath) > 1)
                80 else 100
        }
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

    private fun getFileOrientation(inputStream: ByteArrayInputStream): Int {
        var rotate = 0
        try {
            val exif = ExifInterface(inputStream)
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
}