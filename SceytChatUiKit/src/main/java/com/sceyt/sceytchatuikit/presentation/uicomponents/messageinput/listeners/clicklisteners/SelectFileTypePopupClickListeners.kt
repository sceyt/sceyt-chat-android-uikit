package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners

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
        fun onFileClick()
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners :
            GalleryClickListener,
            TakePhotoClickListener,
            TakeVideoClickListener,
            FileClickListener
}