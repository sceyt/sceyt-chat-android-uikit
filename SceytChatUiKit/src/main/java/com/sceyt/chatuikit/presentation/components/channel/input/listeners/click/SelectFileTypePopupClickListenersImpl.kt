package com.sceyt.chatuikit.presentation.components.channel.input.listeners.click

import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.SelectFileTypePopupClickListeners.ClickListeners
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.SelectFileTypePopupClickListeners.FileClickListener
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.SelectFileTypePopupClickListeners.GalleryClickListener
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.SelectFileTypePopupClickListeners.TakePhotoClickListener
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.SelectFileTypePopupClickListeners.TakeVideoClickListener

open class SelectFileTypePopupClickListenersImpl : ClickListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessageInputView) {
        defaultListeners = view
    }

    private var defaultListeners: ClickListeners? = null
    private var galleryClickListener: GalleryClickListener? = null
    private var takePhotoClickListener: TakePhotoClickListener? = null
    private var takeVideoClickListener: TakeVideoClickListener? = null
    private var fileClickListener: FileClickListener? = null


    override fun onGalleryClick() {
        defaultListeners?.onGalleryClick()
        galleryClickListener?.onGalleryClick()
    }

    override fun onTakePhotoClick() {
        defaultListeners?.onTakePhotoClick()
        takePhotoClickListener?.onTakePhotoClick()
    }

    override fun onTakeVideoClick() {
        defaultListeners?.onTakeVideoClick()
        takeVideoClickListener?.onTakeVideoClick()
    }

    override fun onFileClick() {
        defaultListeners?.onFileClick()
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

    internal fun withDefaultListeners(
            listener: ClickListeners
    ): SelectFileTypePopupClickListenersImpl {
        defaultListeners = listener
        return this
    }
}