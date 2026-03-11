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
import com.sceyt.chatuikit.extensions.setTextAndVisibility
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

private const val SMALL_IMAGE_THRESHOLD_PX = 200

abstract class BaseLinkMessageViewHolder(
    view: View,
    private val style: MessageItemStyle,
    private val messageListeners: MessageClickListeners.ClickListeners? = null,
    displayedListener: ((MessageListItem) -> Unit)? = null,
) : BaseMessageViewHolder(view, style, messageListeners, displayedListener) {
    protected var linkPreviewContainerBinding: SceytMessageLinkPreviewContainerBinding? = null
    protected open val maxSize by lazy {
        bubbleMaxWidth - dpToPx(16f) //(2*8 preview container) is margins
    }
    protected open val minSize get() = maxSize / 3


    fun loadLinkPreview(message: SceytMessage, attachment: SceytAttachment?, viewStub: ViewStub) {
        attachment ?: return
        val previewDetails = attachment.linkPreviewDetails
        setLinkInfo(previewDetails, message, attachment, viewStub)
    }

    protected open fun setLinkInfo(
        data: LinkPreviewDetails?,
        message: SceytMessage,
        attachment: SceytAttachment,
        viewStub: ViewStub,
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
                it.applyStyle()
                it.root.isVisible = true
            }

        with(linkPreviewContainerBinding ?: return) {
            if (!data.imageUrl.isNullOrBlank()) {
                val isSmallImage = (data.imageWidth ?: 0) in 1 until SMALL_IMAGE_THRESHOLD_PX
                val targetImage = if (isSmallImage) smallPreviewImage else previewImage
                previewImage.isVisible = !isSmallImage
                smallPreviewImage.isVisible = isSmallImage
                setImageSize(targetImage, data, isSmallImage)

                val thumb = message.files?.firstOrNull {
                    it.attachment.type == AttachmentTypeEnum.Link.value
                }?.blurredThumb?.toDrawable(context.resources) ?: style.linkPreviewStyle.placeHolder

                val overrideSize = if (!isSmallImage) {
                    calculateScaleWidthHeight(
                        defaultSize = maxSize,
                        minSize = minSize,
                        imageWidth = data.imageWidth ?: 0,
                        imageHeight = data.imageHeight ?: 0
                    )
                } else null
                var builder = Glide.with(context.applicationContext)
                    .load(data.imageUrl)
                    .placeholder(thumb)
                    .listener(glideRequestListener { success ->
                        targetImage.isVisible = success || thumb != null
                    })
                    .transition(DrawableTransitionOptions.withCrossFade(100))

                if (overrideSize != null) {
                    builder = builder.override(overrideSize.width, overrideSize.height)
                }
                builder.into(targetImage)
            } else {
                previewImage.isVisible = false
                smallPreviewImage.isVisible = false
            }

            tvLinkTitle.setTextAndVisibility(data.title)
            tvLinkDesc.setTextAndVisibility(data.description)
            root.isVisible = true

            root.setOnClickListener {
                messageListeners?.onLinkDetailsClick(it, requireMessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, requireMessageItem)
                return@setOnLongClickListener true
            }
        }
    }

    protected open fun setImageSize(
        image: View,
        details: LinkPreviewDetails?,
        isSmallImage: Boolean = false
    ) {
        if (details?.imageWidth == null || details.imageHeight == null
            || details.imageWidth == 0 || details.imageHeight == 0
        ) {
            image.isVisible = false
            return
        }
        if (!isSmallImage) {
            val size = calculateScaleWidthHeight(
                maxSize, minSize,
                imageWidth = details.imageWidth,
                imageHeight = details.imageHeight
            )
            image.updateLayoutParams<ViewGroup.LayoutParams> {
                width = maxSize
                height = size.height
            }
        }
        image.isVisible = true
    }

    protected open fun SceytMessageLinkPreviewContainerBinding.applyStyle() {
        val linkStyle = style.linkPreviewStyle
        val backgroundStyle = if (requireMessage.incoming)
            style.incomingLinkPreviewBackgroundStyle
        else style.outgoingLinkPreviewBackgroundStyle
        backgroundStyle.apply(root)
        linkStyle.titleStyle.apply(tvLinkTitle)
        linkStyle.descriptionStyle.apply(tvLinkDesc)
    }
}