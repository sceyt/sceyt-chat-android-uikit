package com.sceyt.chatuikit.presentation.components.camera

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaFileFactory(private val context: Context) {

    fun createPhotoFile(): File = create(prefix = "Photo_", suffix = ".jpg", dirName = "Photos")

    fun createVideoFile(): File = create(prefix = "Video_", suffix = ".mp4", dirName = "Videos")

    private fun create(prefix: String, suffix: String, dirName: String): File {
        val directory = File(context.filesDir, dirName)
        if (!directory.exists()) directory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File.createTempFile("${prefix}${timestamp}_", suffix, directory)
    }
}