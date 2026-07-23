package com.sceyt.chatuikit.shared.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.Size
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.sceyt.chatuikit.extensions.bitmapToByteArray
import com.sceyt.chatuikit.extensions.getFileSizeMb
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

object FileResizeUtil {

    fun resizeAndCompressImage(
        filePath: String,
        parentDir: File,
        reqSize: Int = 800,
        reqWith: Int = reqSize,
        reqHeight: Int = reqSize,
        preferQuality: Int = 80
    ): Result<File> {
        if (filePath.isBlank()) return Result.failure(IllegalArgumentException("File path is blank. Can't resize"))
        return runCatching {
            val initialSize = getImageDimensionsSize(filePath)
            if (initialSize.width == -1 || initialSize.height == -1) return Result.failure(
                IllegalArgumentException("Invalid image dimensions for resizing $filePath")
            )

            val inSimpleSize = calculateInSampleSize(initialSize, reqWith, reqHeight)
            val quality = calculateQuality(filePath, inSimpleSize, preferQuality)

            // No need to resize
            if (inSimpleSize == 1 && quality == 100)
                return@runCatching File(filePath)

            var bmpPic = BitmapFactory.decodeFile(filePath, BitmapFactory.Options().apply {
                inSampleSize = inSimpleSize
            })
            val dest = File(parentDir, "${UUID.randomUUID()}.JPEG")
            bmpPic = getOrientationCorrectedBitmap(bitmap = bmpPic, filePath)
            val bmpFile = FileOutputStream(dest)
            bmpPic.compress(Bitmap.CompressFormat.JPEG, quality, bmpFile)
            bmpFile.flush()
            bmpFile.close()
            dest
        }
    }

    private fun resizeAndCompressImageAsFile(
        bitmap: Bitmap,
        parentDir: File,
        reqSize: Int = 800,
        reqWith: Int = reqSize,
        reqHeight: Int = reqSize
    ): Result<File> = runCatching {
        val initialSize = Size(bitmap.width, bitmap.height)
        val byteArray = bitmap.bitmapToByteArray() ?: return Result.failure(
            IllegalArgumentException("Invalid byte array for resizing")
        )

        val inSimpleSize = calculateInSampleSize(initialSize, reqWith, reqHeight)

        val bmpPic = if (inSimpleSize == 1) bitmap
        else {
            BitmapFactory.decodeByteArray(
                /* data = */ byteArray,
                /* offset = */ 0,
                /* length = */ byteArray.size,
                /* opts = */ BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(initialSize, reqWith, reqHeight)
                })
        }
        val dest = File(parentDir, "${UUID.randomUUID()}.JPEG")
        val bmpFile = FileOutputStream(dest)
        bmpPic.compress(Bitmap.CompressFormat.JPEG, 80, bmpFile)
        bmpFile.flush()
        bmpFile.close()
        dest
    }

    fun resizeAndCompressBitmapWithFilePath(
        filePath: String,
        reqSize: Int = 800
    ): Result<Bitmap> = runCatching {
        if (filePath.isBlank())
            return Result.failure(IllegalArgumentException("File path is blank. Can't resize"))

        val initialSize = getImageDimensionsSize(filePath)
        if (initialSize.width == -1 || initialSize.height == -1) return Result.failure(
            IllegalArgumentException("Invalid image dimensions for resizing $filePath")
        )

        val size = Size(initialSize.width, initialSize.height)
        val w = (reqSize * size.width / max(size.width, size.height)).toDouble().roundToInt()
        val h = (reqSize * size.height / max(size.width, size.height)).toDouble().roundToInt()

        val inSimpleSize = calculateInSampleSize(initialSize, w, h)

        val bmpPic = BitmapFactory.decodeFile(filePath, BitmapFactory.Options().apply {
            inSampleSize = inSimpleSize
        })

        getOrientationCorrectedBitmap(bitmap = bmpPic, filePath).scale(w, h, false)
    }


    fun resizeAndCompressImageAsByteArray(
        bitmap: Bitmap,
        reqSize: Int = 800
    ): Result<Bitmap> = runCatching {
        val initialSize = Size(bitmap.width, bitmap.height)
        val byteArray = bitmap.bitmapToByteArray() ?: return Result.failure(
            IllegalArgumentException("Invalid byte array for resizing")
        )

        val size = Size(initialSize.width, initialSize.height)
        val w = (reqSize * size.width / max(size.width, size.height)).toDouble().roundToInt()
        val h = (reqSize * size.height / max(size.width, size.height)).toDouble().roundToInt()

        BitmapFactory.decodeByteArray(
            byteArray, 0, byteArray.size, BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(initialSize, w, h)
            }).scale(w, h, false)
    }

    fun getImageDimensionsSize(path: String): Size {
        val input = FileInputStream(path)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(input, null, options)
        return Size(options.outWidth, options.outHeight)
    }

    fun getImageSizeOriented(path: String): Size {
        var size = Size(0, 0)
        try {
            size = getImageDimensionsSize(path)
            val exif = ExifInterface(path)
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_270,
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_TRANSVERSE ->
                    size = Size(size.height, size.width)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    fun getVideoSize(path: String): Size? {
        val metaRetriever = MediaMetadataRetriever()
        return try {
            metaRetriever.setDataSource(path)
            val height =
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()
                    ?: 0
            val width =
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()
                    ?: 0
            Size(width, height)
        } catch (e: Throwable) {
            print("Error getting video size: ${e.message}")
            null
        } finally {
            metaRetriever.release()
        }
    }

    fun getVideoSizeOriented(path: String): Size? {
        val metaRetriever = MediaMetadataRetriever()
        return try {
            metaRetriever.setDataSource(path)
            val rotation =
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull()
                    ?: 0
            val height =
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()
                    ?: 0
            val width =
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()
                    ?: 0
            if (rotation == 90 || rotation == 270)
                return Size(height, width)

            return Size(width, height)
        } catch (e: Throwable) {
            print("Error getting video size: ${e.message}")
            null
        } finally {
            metaRetriever.release()
        }
    }

    fun getVideoDuration(context: Context, path: String): Long? {
        val retriever = MediaMetadataRetriever()
        val timeInMilliSec: Long? = try {
            retriever.setDataSource(context, path.toUri())
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLongOrNull()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
        return timeInMilliSec
    }

    fun getVideoThumbByUrlAsByteArray(url: String, maxImageSize: Float): Result<Bitmap> {
        return getVideoThumb(url, maxImageSize)
    }

    fun getVideoThumb(
        path: String,
        maxImageSize: Float
    ): Result<Bitmap> {
        val retriever = MediaMetadataRetriever()
        return try {
            val bitmap = retriever.apply {
                setDataSource(path)
            }.getFrameAtTime(1000)
                ?: return Result.failure(IllegalArgumentException("Failed to retrieve video frame from $path"))
            resizeAndCompressImageAsByteArray(bitmap, reqSize = maxImageSize.toInt())
        } catch (ex: Exception) {
            ex.printStackTrace()
            Result.failure(ex)
        } finally {
            retriever.release()
        }
    }

    fun getVideoThumbAsFile(
        context: Context,
        path: String,
        maxImageSize: Float
    ): Result<File> {
        val retriever = MediaMetadataRetriever()
        return try {
            val bitmap = retriever.apply {
                setDataSource(path)
            }.getFrameAtTime(1000)
            resizeAndCompressImageAsFile(
                bitmap = bitmap
                    ?: return Result.failure(IllegalArgumentException("Failed to retrieve video frame from $path")),
                parentDir = context.cacheDir,
                reqSize = maxImageSize.toInt()
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            Result.failure(ex)
        } finally {
            retriever.release()
        }
    }

    fun getImageThumbAsFile(context: Context, url: String, maxImageSize: Float): Result<File> {
        return resizeAndCompressImage(
            filePath = url,
            parentDir = context.cacheDir,
            reqSize = maxImageSize.toInt()
        )
    }

    fun getOrientationCorrectedBitmap(bitmap: Bitmap, filePath: String): Bitmap {
        val orientation = getFileOrientation(imagePath = filePath)
        val matrix = createOrientationMatrix(orientation) ?: return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun getOrientationCorrectedBitmap(bitmap: Bitmap, byteArray: ByteArray): Bitmap {
        val orientation = getFileOrientation(ByteArrayInputStream(byteArray))
        val matrix = createOrientationMatrix(orientation) ?: return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun createFileFromBitmap(context: Context, bitmap: Bitmap): Result<File> {
        return runCatching {
            val fileDest = "${context.cacheDir}/" + UUID.randomUUID() + ".JPEG"
            val bmpFile = FileOutputStream(fileDest)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bmpFile)
            bmpFile.flush()
            bmpFile.close()
            bitmap.recycle()
            File(fileDest)
        }
    }

    fun calculateInSampleSize(size: Size, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = size.run { height to width }

        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
            val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()

            // If the req size is greater than 500, then we can use max simple size, if lower than 500,
            // then we don't use max simple size, because it will be bad quality.
            // Note: 500 is a conditional value, you can change it to any value you want.
            val useMaxSimpleSize = max(reqWidth, reqHeight) > 500
            inSampleSize = if (useMaxSimpleSize)
                max(heightRatio, widthRatio) else (heightRatio + widthRatio) / 2
        }
        return inSampleSize
    }

    private fun calculateQuality(filePath: String, isSimpleSize: Int, preferQuality: Int): Int {
        return if (isSimpleSize > 1)
            preferQuality
        else {
            // If the file size is greater than 1 MB, then we can use prefer quality,
            // otherwise we don't use prefer quality, because it will be bad quality.
            if (getFileSizeMb(filePath) > 1)
                preferQuality else 100
        }
    }

    private fun getFileOrientation(imagePath: String): Int {
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            val exif = ExifInterface(imagePath)
            orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return orientation
    }

    private fun getFileOrientation(inputStream: ByteArrayInputStream): Int {
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            val exif = ExifInterface(inputStream)
            orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return orientation
    }

    private fun createOrientationMatrix(orientation: Int): Matrix? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return null
        }
        return matrix
    }
}
