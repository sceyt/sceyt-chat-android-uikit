package com.sceyt.chatuikit.presentation.components.channel.input.listeners.click

sealed interface SelectFileTypePopupClickListeners {
    fun interface GalleryClickListener : SelectFileTypePopupClickListeners {
        fun onGalleryClick()
    }

    fun interface TakePhotoClickListener : SelectFileTypePopupClickListeners {
        fun onTakePhotoClick()
    }

    fun interface TakeVideoClickListener : SelectFileTypePopupClickListeners {
        fun onTakeVideoClick()
    }

    fun interface FileClickListener : SelectFileTypePopupClickListeners {
        fun onFileClick(mimeTypes: Array<String>?)
    }

    fun interface PollClickListener : SelectFileTypePopupClickListeners {
        fun onPollClick()
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners :
            GalleryClickListener,
            TakePhotoClickListener,
            TakeVideoClickListener,
            FileClickListener,
            PollClickListener
}