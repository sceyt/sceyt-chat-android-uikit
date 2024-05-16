package com.sceyt.chatuikit.media.audio

import java.io.File
import java.util.UUID

object FileManager {

    fun createFile(extension: String, directory: String): File {
        val mediaDir = File(directory)
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        return File(mediaDir, "Audio_" + UUID.randomUUID() + "." + extension)
    }
}