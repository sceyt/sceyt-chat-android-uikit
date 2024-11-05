package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytMessageLinkPreviewContainerBinding
import com.sceyt.chatuikit.extensions.calculateScaleWidthHeight
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setTextAndVisibility
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

abstract class BaseLinkMessageViewHolder(
        view: View,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners? = null,
        displayedListener: ((MessageListItem) -> Unit)? = null,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMessageViewHolder(view, style, messageListeners, displayedListener) {
    protected var linkPreviewContainerBinding: SceytMessageLinkPreviewContainerBinding? = null
    protected open val maxSize by lazy {
        bubbleMaxWidth - dpToPx(28f) //(2*8 preview container + 2*6 root paddings ) is margins
    }
    protected open val minSize get() = maxSize / 3


    fun loadLinkPreview(message: SceytMessage, attachment: SceytAttachment?, viewStub: ViewStub) {
        attachment ?: return
        val previewDetails = attachment.linkPreviewDetails

        if (previewDetails == null) {
            setLinkInfo(null, message, attachment, viewStub)
            needMediaDataCallback(NeedMediaInfoData.NeedLinkPreview(attachment, false))
        } else {
            setLinkInfo(previewDetails, message, attachment, viewStub)
            if (previewDetails.imageUrl != null && previewDetails.imageWidth == null)
                needMediaDataCallback(NeedMediaInfoData.NeedLinkPreview(attachment, true))
        }
    }

    protected open fun setLinkInfo(
            data: LinkPreviewDetails?,
            message: SceytMessage,
            attachment: SceytAttachment,
            viewStub: ViewStub
    ) {
        if (data == null || data.link != attachment.url) {
            viewStub.isVisible = false
            return
        }
        val hasImageUrl = !data.imageUrl.isNullOrBlank()

        if (data.hideDetails || (!hasImageUrl && data.title.isNullOrBlank() && data.description.isNullOrBlank())) {
            viewStub.isVisible = false
            return
        }

        if (viewStub.parent != null)
            SceytMessageLinkPreviewContainerBinding.bind(viewStub.inflate()).also {
                linkPreviewContainerBinding = it
                it.root.isVisible = true
            }

        with(linkPreviewContainerBinding ?: return) {
            applyStyle()
            if (!data.imageUrl.isNullOrBlank()) {
                setImageSize(previewImage, data)
                val thumb = message.files?.firstOrNull {
                    it.attachment.type == AttachmentTypeEnum.Link.value
                }?.blurredThumb?.toDrawable(context.resources) ?: style.linkPreviewStyle.placeHolder

                Glide.with(context.applicationContext)
                    .load(data.imageUrl)
                    .override(data.imageWidth ?: maxSize, data.imageHeight ?: maxSize)
                    .placeholder(thumb)
                    .listener(glideRequestListener(onResourceReady = {
                        previewImage.isVisible = true
                    }, onLoadFailed = {
                        previewImage.isVisible = false
                    }))
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .into(previewImage)
            } else previewImage.isVisible = false

            tvLinkTitle.setTextAndVisibility(data.title)
            tvLinkDesc.setTextAndVisibility(data.description)
            root.isVisible = true

            root.setOnClickListener {
                messageListeners?.onLinkDetailsClick(it, messageListItem as MessageListItem.MessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
                return@setOnLongClickListener true
            }
        }
    }

    protected open fun setImageSize(image: View, details: LinkPreviewDetails?) {
        if (details?.imageWidth == null || details.imageHeight == null
                || details.imageWidth == 0 || details.imageHeight == 0) {
            image.isVisible = false
            return
        }
        val size = calculateScaleWidthHeight(maxSize, minSize,
            imageWidth = details.imageWidth,
            imageHeight = details.imageHeight)

        image.updateLayoutParams<ViewGroup.LayoutParams> {
            width = maxSize
            height = size.height
        }
        image.isVisible = true
    }

    protected open fun SceytMessageLinkPreviewContainerBinding.applyStyle() {
        val linkStyle = style.linkPreviewStyle
        val color = if ((messageListItem as MessageListItem.MessageItem).message.incoming)
            style.incomingLinkPreviewBackgroundColor
        else style.outgoingLinkPreviewBackgroundColor
        root.setBackgroundTint(color)
        linkStyle.titleStyle.apply(tvLinkTitle)
        linkStyle.descriptionStyle.apply(tvLinkDesc)
    }
}