package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.databinding.SceytMessageLinkPreviewContainerBinding
import com.sceyt.sceytchatuikit.extensions.setTextAndVisibility
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

abstract class BaseLinkMsgViewHolder(
        private val linkPreview: LinkPreviewHelper,
        view: View,
        messageListeners: MessageClickListeners.ClickListeners? = null,
        displayedListener: ((MessageListItem) -> Unit)? = null,
        userNameBuilder: ((User) -> String)? = null,
) : BaseMsgViewHolder(view, messageListeners, displayedListener, userNameBuilder) {

    fun loadLinkPreview(message: SceytAttachment, layoutLinkPreview: SceytMessageLinkPreviewContainerBinding, messageBody: TextView) {
        if (message.linkPreviewDetails == null) {

            layoutLinkPreview.setLinPreview(null, messageBody, message)

            linkPreview.getPreview(message, successListener = {
                message.linkPreviewDetails = it
                layoutLinkPreview.setLinPreview(it, messageBody, message)
            })
        } else layoutLinkPreview.setLinPreview(message.linkPreviewDetails, messageBody, message)
    }

    private fun SceytMessageLinkPreviewContainerBinding.setLinPreview(data: LinkPreviewDetails?,
                                                                      messageBody: TextView,
                                                                      message: SceytAttachment) {
        when {
            data == null -> {
                root.isVisible = false
            }

            data.link != message.url -> {
                return
            }

            data.description.isNullOrBlank() && data.description.isNullOrBlank() && data.imageUrl.isNullOrBlank() -> {
                root.isVisible = false
            }

            else -> {
                if (data.imageUrl.isNullOrBlank().not()) {
                    Glide.with(context)
                        .load(data.imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade(100))
                        .into(previewImage)
                    previewImage.isVisible = true
                } else previewImage.isVisible = false

                tvLinkTitle.setTextAndVisibility(data.description)
                tvLinkDesc.setTextAndVisibility(data.description)
                // messageBody.text = message.body.trim()
                root.isVisible = true
            }
        }
    }
}