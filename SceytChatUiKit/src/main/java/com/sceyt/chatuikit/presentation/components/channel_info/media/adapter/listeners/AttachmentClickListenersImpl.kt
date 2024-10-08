package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem

class AttachmentClickListenersImpl : AttachmentClickListeners.ClickListeners {
    private var attachmentClickListener: AttachmentClickListeners.AttachmentClickListener? = null
    private var attachmentLoaderClickListener: AttachmentClickListeners.AttachmentLoaderClickListener? = null

    override fun onAttachmentClick(view: View, item: ChannelFileItem) {
        attachmentClickListener?.onAttachmentClick(view, item)
    }

    override fun onAttachmentLoaderClick(view: View, item: ChannelFileItem) {
        attachmentLoaderClickListener?.onAttachmentLoaderClick(view, item)
    }

    fun setListener(listener: AttachmentClickListeners) {
        when (listener) {
            is AttachmentClickListeners.ClickListeners -> {
                attachmentClickListener = listener
                attachmentLoaderClickListener = listener
            }

            is AttachmentClickListeners.AttachmentClickListener -> {
                attachmentClickListener = listener
            }

            is AttachmentClickListeners.AttachmentLoaderClickListener -> {
                attachmentLoaderClickListener = listener
            }
        }
    }
}