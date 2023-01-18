package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

data class AttachmentPayLoadDiff(
        var filePathChanged: Boolean,
        var urlChanged: Boolean,
        val progressPercentChanged: Boolean) {

    fun hasDifference(): Boolean {
        return filePathChanged || urlChanged || progressPercentChanged
    }

    companion object {
        val DEFAULT = AttachmentPayLoadDiff(
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
