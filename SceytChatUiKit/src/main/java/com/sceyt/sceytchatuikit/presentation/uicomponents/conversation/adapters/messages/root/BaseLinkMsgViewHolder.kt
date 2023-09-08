package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytMessageLinkPreviewContainerBinding
import com.sceyt.sceytchatuikit.extensions.setTextAndVisibility
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

abstract class BaseLinkMsgViewHolder(private val linkPreview: LinkPreviewHelper,
                                     view: View,
                                     messageListeners: MessageClickListeners.ClickListeners? = null,
                                     displayListItem: ((MessageListItem) -> Unit)? = null,
                                     userNameBuilder: ((User) -> String)? = null)
    : BaseMsgViewHolder(view, messageListeners, displayListItem, userNameBuilder) {

    fun loadLinkPreview(message: MessageListItem.MessageItem, layoutLinkPreview: SceytMessageLinkPreviewContainerBinding, messageBody: TextView) {
        if (message.linkPreviewData == null) {

            layoutLinkPreview.setLinPreview(null, messageBody, message.message)

            linkPreview.getPreview(message.message.id, message.message.body, successListener = {
                message.linkPreviewData = it

                layoutLinkPreview.setLinPreview(message.linkPreviewData, messageBody, message.message)
            })
        } else layoutLinkPreview.setLinPreview(message.linkPreviewData, messageBody, message.message)
    }

    private fun SceytMessageLinkPreviewContainerBinding.setLinPreview(data: LinkPreviewHelper.PreviewMetaData?,
                                                                      messageBody: TextView,
                                                                      message: SceytMessage) {
        when {
            data == null -> {
                root.isVisible = false
            }
            data.messageId != message.id -> {
                return
            }
            data.title.isNullOrBlank() && data.description.isNullOrBlank() && data.imageUrl.isNullOrBlank() -> {
                root.isVisible = false
            }
            else -> {
                if (data.imageUrl.isNullOrBlank().not()) {
                    Glide.with(context)
                        .load(data.imageUrl)
                        .override(previewImage.width)
                        .transition(DrawableTransitionOptions.withCrossFade(100))
                        .into(previewImage)
                    previewImage.isVisible = true
                } else previewImage.isVisible = false

                tvLinkTitle.setTextAndVisibility(data.title)
                tvLinkDesc.setTextAndVisibility(data.description)
                messageBody.text = message.body.trim()
                root.isVisible = true
            }
        }
    }
}