package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytMessageLinkPreviewContainerBinding
import com.sceyt.sceytchatuikit.extensions.calculateScaleWidthHeight
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.glideRequestListener
import com.sceyt.sceytchatuikit.extensions.setTextAndVisibility
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.OutLinkMsgViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

abstract class BaseLinkMsgViewHolder(
        private val linkPreview: LinkPreviewHelper,
        view: View,
        messageListeners: MessageClickListeners.ClickListeners? = null,
        displayedListener: ((MessageListItem) -> Unit)? = null,
        userNameBuilder: ((User) -> String)? = null,
) : BaseMsgViewHolder(view, messageListeners, displayedListener, userNameBuilder) {
    private var linkPreviewContainerBinding: SceytMessageLinkPreviewContainerBinding? = null
    private val maxSize by lazy {
        bubbleMaxWidth - dpToPx(16f) //4f is margins
    }
    private val minSize = maxSize / 3


    fun loadLinkPreview(message: SceytMessage, attachment: SceytAttachment?, viewStub: ViewStub) {
        attachment ?: return
        val previewDetails = attachment.linkPreviewDetails

        if (previewDetails == null) {
            setLinkInfo(null, message, attachment, viewStub)
            linkPreview.getPreview(attachment, true, successListener = {
                attachment.linkPreviewDetails = it
                setLinkInfo(it, message, attachment, viewStub)
            })
        } else {
            setLinkInfo(previewDetails, message, attachment, viewStub)
            linkPreview.checkMissedData(previewDetails){
                attachment.linkPreviewDetails = it
                setLinkInfo(it, message, attachment, viewStub)
            }
        }
    }

    private fun setLinkInfo(data: LinkPreviewDetails?, message: SceytMessage, attachment: SceytAttachment, viewStub: ViewStub) {
        if (data == null || data.link != attachment.url) {
            viewStub.isVisible = false
            return
        }
        val hasImageUrl = !data.imageUrl.isNullOrBlank()

        if (!hasImageUrl && data.title.isNullOrBlank() && data.description.isNullOrBlank()) {
            viewStub.isVisible = false
            return
        }

        if (viewStub.parent != null)
            SceytMessageLinkPreviewContainerBinding.bind(viewStub.inflate()).also {
                linkPreviewContainerBinding = it
                it.root.isVisible = true
            }

        with(linkPreviewContainerBinding ?: return) {
            setupStyle()
            if (!data.imageUrl.isNullOrBlank()) {
                val thumb = message.files?.firstOrNull { it.file.type == AttachmentTypeEnum.Link.value() }?.blurredThumb
                setImageSize(previewImage, data)
                Glide.with(context)
                    .load(data.imageUrl)
                    .override(data.imageWidth ?: maxSize, data.imageHeight ?: maxSize)
                    .placeholder(thumb?.toDrawable(context.resources))
                    .listener(glideRequestListener(onResourceReady = {
                        previewImage.isVisible = true
                    }, onLoadFailed = {
                        previewImage.isVisible = false
                    }))
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .into(previewImage)
            }

            tvLinkTitle.setTextAndVisibility(data.title)
            tvLinkDesc.setTextAndVisibility(data.description)
            root.isVisible = true
        }
    }

    private fun setImageSize(image: View, details: LinkPreviewDetails?) {
        if (details?.imageWidth == null || details.imageHeight == null) {
            image.isVisible = false
            return
        }
        val size = calculateScaleWidthHeight(maxSize, minSize, imageWidth = details.imageWidth
                ?: maxSize,
            imageHeight = details.imageHeight ?: maxSize)

        image.updateLayoutParams<ViewGroup.LayoutParams> {
            width = size.width
            height = size.height
        }
        image.isVisible = true
    }

    private fun SceytMessageLinkPreviewContainerBinding.setupStyle() {
        val color = if (this@BaseLinkMsgViewHolder is OutLinkMsgViewHolder)
            ColorStateList.valueOf(context.getCompatColor(MessagesStyle.outLinkPreviewBackgroundColor))
        else ColorStateList.valueOf(context.getCompatColor(MessagesStyle.incLinkPreviewBackgroundColor))
        root.backgroundTintList = color
    }
}