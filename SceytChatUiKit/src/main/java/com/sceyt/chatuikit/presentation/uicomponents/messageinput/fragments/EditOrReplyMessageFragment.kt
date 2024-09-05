package com.sceyt.chatuikit.presentation.uicomponents.messageinput.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytFragmentEditOrReplyMessageBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setBoldSpan
import com.sceyt.chatuikit.extensions.setTint
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody
import com.sceyt.chatuikit.presentation.extensions.isTextMessage
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.CancelReplyMessageViewClickListener
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.MessageBodyStyleHelper
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.sceytstyles.MessageInputStyle
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle
import com.sceyt.chatuikit.shared.utils.ViewUtil

open class EditOrReplyMessageFragment : Fragment() {
    protected var binding: SceytFragmentEditOrReplyMessageBinding? = null
    protected var clickListeners: CancelReplyMessageViewClickListener? = null
    protected var userNameFormatter: UserNameFormatter? = SceytChatUIKit.formatters.userNameFormatter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentEditOrReplyMessageBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.applyStyle()
        initViews()
    }

    protected fun initViews() {
        binding?.icCancelReply?.setOnClickListener {
            clickListeners?.onCancelReplyMessageViewClick(it)
        }
    }

    open fun editMessage(message: SceytMessage) {
        with(binding ?: return) {
            if (!root.isVisible || root.height != root.measuredHeight || root.measuredHeight == 0) {
                root.isVisible = true
                ViewUtil.expandHeight(root, 0, 200)
            }
            icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_edit)
            layoutImage.isVisible = false
            tvName.text = getString(R.string.sceyt_edit_message)
            tvMessageBody.text = if (message.isTextMessage())
                MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(message)
            else message.getFormattedBody(root.context)
        }
    }

    open fun replyMessage(message: SceytMessage, inputStyle: MessageInputStyle, style: MessagesListViewStyle?) {
        val messageItemStyle = style?.messageItemStyle
                ?: MessageItemStyle.Builder(requireContext(), null).build()
        with(binding ?: return) {
            if (!root.isVisible || root.height != root.measuredHeight || root.measuredHeight == 0) {
                root.isVisible = true
                ViewUtil.expandHeight(root, 0, 200)
            }
            val name = message.user?.let { userNameFormatter?.format(it) }
                    ?: message.user?.getPresentableName() ?: ""
            val text = "${getString(R.string.sceyt_reply)} $name".run {
                setBoldSpan(length - name.length, length)
            }
            tvName.text = text
            icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_input_reply)

            if (!message.attachments.isNullOrEmpty()) {
                layoutImage.isVisible = true
                loadReplyMessageImage(message.attachments, messageItemStyle)
            } else layoutImage.isVisible = false

            tvMessageBody.text = inputStyle.replyMessageBodyFormatter.format(requireContext(), message)
        }
    }

    open fun close(readyCb: (() -> Unit?)? = null) {
        with(binding ?: return) {
            ViewUtil.collapseHeight(root, to = 0, duration = 200) {
                root.isVisible = false
                readyCb?.invoke()
            }
        }
    }

    open fun setClickListener(clickListeners: CancelReplyMessageViewClickListener) {
        this.clickListeners = clickListeners
    }

    protected open fun loadReplyMessageImage(attachments: List<SceytAttachment>?, style: MessageItemStyle) {
        if (attachments.isNullOrEmpty()) {
            binding?.layoutImage?.isVisible = false
            return
        }
        with(binding ?: return) {
            tvName.setTextColor(style.senderNameTextColor)
            tvMessageBody.setTextColor(style.bodyTextColor)
            imageAttachment.isVisible = true
            fileAttachment.isVisible = false
            val (links, others) = attachments.partition { it.type == AttachmentTypeEnum.Link.value() }
            if (others.isNotEmpty()) {
                val attachment = others[0]
                when {
                    attachment.type.isEqualsVideoOrImage() -> {
                        loadImage(imageAttachment, attachment.metadata, attachment.filePath)
                    }

                    attachment.type == AttachmentTypeEnum.Voice.value() -> {
                        layoutImage.isVisible = false
                    }

                    else -> {
                        fileAttachment.setImageDrawable(style.fileAttachmentIcon)
                        fileAttachment.isVisible = true
                        imageAttachment.isVisible = false
                    }
                }
            } else {
                val attachment = links[0]
                attachment.linkPreviewDetails?.imageUrl?.let {
                    loadImage(imageAttachment, attachment.metadata, it, style.linkAttachmentIcon)
                } ?: imageAttachment.setImageDrawable(style.linkAttachmentIcon)
            }
        }
    }

    private fun loadImage(imageAttachment: ImageView,
                          metadata: String?,
                          path: String?,
                          defaultPlaceHolder: Drawable? = null,
                          error: Drawable? = null
    ) {
        val placeHolder = getThumbFromMetadata(metadata)?.toDrawable(requireContext().resources)
            ?.mutate() ?: defaultPlaceHolder
        Glide.with(requireContext())
            .load(path)
            .placeholder(placeHolder)
            .override(100)
            .error(error)
            .into(imageAttachment)
    }

    private fun SceytFragmentEditOrReplyMessageBinding.applyStyle() {
        val accentColor = requireContext().getCompatColor(SceytChatUIKit.theme.accentColor)
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.surface1Color))
        tvName.setTextColor(accentColor)
        icReplyOrEdit.setTint(accentColor)
        fileAttachment.setBackgroundTint(accentColor)
        icCancelReply.setTintColorRes(SceytChatUIKit.theme.iconSecondaryColor)
    }
}