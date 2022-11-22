package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners

sealed interface SelectFileTypePopupClickListeners {
    fun interface GalleryClickListener : SelectFileTypePopupClickListeners {
        fun onGalleryClick()
    }

    fun interface TakePhotoClickListener : SelectFileTypePopupClickListeners {
        fun onTakePhotoClick()
    }

    fun interface FileClickListener : SelectFileTypePopupClickListeners {
        fun onFileClick()
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners :
            GalleryClickListener,
            TakePhotoClickListener,
            FileClickListener
}