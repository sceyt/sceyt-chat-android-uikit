package com.sceyt.sceytchatuikit.persistence.differs

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment

data class AttachmentDiff(
        var filePathChanged: Boolean,
        var urlChanged: Boolean,
        val progressPercentChanged: Boolean) {

    fun hasDifference(): Boolean {
        return filePathChanged || urlChanged || progressPercentChanged
    }

    companion object {
        val DEFAULT = AttachmentDiff(
            filePathChanged = true,
            urlChanged = true,
            progressPercentChanged = true
        )
    }

    override fun toString(): String {
        return "filePathChanged: $filePathChanged, urlChanged: $urlChanged," +
                "progressPercentChanged: $progressPercentChanged"
    }
}

fun SceytAttachment.diff(other: SceytAttachment): AttachmentDiff {
    return AttachmentDiff(
        filePathChanged = filePath != other.filePath,
        urlChanged = url != other.url,
        progressPercentChanged = progressPercent != other.progressPercent,
    )
}

fun SceytAttachment.diffBetweenServerData(other: SceytAttachment): AttachmentDiff {
    return AttachmentDiff(
        filePathChanged = false,
        urlChanged = url != other.url,
        progressPercentChanged = false
    )
}