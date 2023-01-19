package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners

import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.SelectFileTypePopupClickListeners.ClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.SelectFileTypePopupClickListeners.FileClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.SelectFileTypePopupClickListeners.GalleryClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.SelectFileTypePopupClickListeners.TakePhotoClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.SelectFileTypePopupClickListeners.TakeVideoClickListener

open class SelectFileTypePopupClickListenersImpl(view: MessageInputView) : ClickListeners {
    private var defaultListeners: ClickListeners = view
    private var galleryClickListener: GalleryClickListener? = null
    private var takePhotoClickListener: TakePhotoClickListener? = null
    private var takeVideoClickListener: TakeVideoClickListener? = null
    private var fileClickListener: FileClickListener? = null


    override fun onGalleryClick() {
        defaultListeners.onGalleryClick()
        galleryClickListener?.onGalleryClick()
    }

    override fun onTakePhotoClick() {
        defaultListeners.onTakePhotoClick()
        takePhotoClickListener?.onTakePhotoClick()
    }

    override fun onTakeVideoClick() {
        defaultListeners.onTakeVideoClick()
        takeVideoClickListener?.onTakeVideoClick()
    }

    override fun onFileClick() {
        defaultListeners.onFileClick()
        fileClickListener?.onFileClick()
    }

    fun setListener(listener: SelectFileTypePopupClickListeners) {
        when (listener) {
            is ClickListeners -> {
                galleryClickListener = listener
                takePhotoClickListener = listener
                fileClickListener = listener
            }
            is GalleryClickListener -> {
                galleryClickListener = listener
            }
            is TakePhotoClickListener -> {
                takePhotoClickListener = listener
            }
            is TakeVideoClickListener -> {
                takeVideoClickListener = listener
            }
            is FileClickListener -> {
                fileClickListener = listener
            }
        }
    }
}