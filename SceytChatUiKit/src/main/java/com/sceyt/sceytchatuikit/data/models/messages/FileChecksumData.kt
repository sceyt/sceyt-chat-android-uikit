package com.sceyt.sceytchatuikit.data.models.messages

data class FileChecksumData(
        val checksum: Long,
        val resizedFilePath: String?,
        val url: String?,
        val metadata: String?,
        val fileSize: Long?
)
