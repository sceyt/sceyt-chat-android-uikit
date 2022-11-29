package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners

import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView

open class SelectFileTypePopupClickListenersImpl(view: MessageInputView) : SelectFileTypePopupClickListeners.ClickListeners {
    private var defaultListeners: SelectFileTypePopupClickListeners.ClickListeners = view
    private var galleryClickListener: SelectFileTypePopupClickListeners.GalleryClickListener? = null
    private var takePhotoClickListener: SelectFileTypePopupClickListeners.TakePhotoClickListener? = null
    private var fileClickListener: SelectFileTypePopupClickListeners.FileClickListener? = null


    override fun onGalleryClick() {
        defaultListeners.onGalleryClick()
        galleryClickListener?.onGalleryClick()
    }

    override fun onTakePhotoClick() {
        defaultListeners.onTakePhotoClick()
        takePhotoClickListener?.onTakePhotoClick()
    }

    override fun onFileClick() {
        defaultListeners.onFileClick()
        fileClickListener?.onFileClick()
    }

    fun setListener(listener: SelectFileTypePopupClickListeners) {
        when (listener) {
            is SelectFileTypePopupClickListeners.ClickListeners -> {
                galleryClickListener = listener
                takePhotoClickListener = listener
                fileClickListener = listener
            }
            is SelectFileTypePopupClickListeners.GalleryClickListener -> {
                galleryClickListener = listener
            }
            is SelectFileTypePopupClickListeners.TakePhotoClickListener -> {
                takePhotoClickListener = listener
            }
            is SelectFileTypePopupClickListeners.FileClickListener -> {
                fileClickListener = listener
            }
        }
    }
}