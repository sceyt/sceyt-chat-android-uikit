package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.databinding.SceytMessageLinkPreviewContainerBinding
import com.sceyt.chat.ui.extensions.setTextAndVisibility
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.shared.helpers.LinkPreviewHelper

abstract class BaseLinkMsgViewHolder(private val linkPreview: LinkPreviewHelper,
                                     view: View,
                                     messageListeners: MessageClickListenersImpl? = null)
    : BaseMsgViewHolder(view, messageListeners) {

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
            data.loadId != message.id -> {
                return
            }
            data.title.isNullOrBlank() && data.description.isNullOrBlank() && data.imageUrl.isNullOrBlank() -> {
                root.isVisible = false
            }
            else -> {
                if (data.imageUrl.isNullOrBlank().not()) {
                    Glide.with(itemView.context)
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