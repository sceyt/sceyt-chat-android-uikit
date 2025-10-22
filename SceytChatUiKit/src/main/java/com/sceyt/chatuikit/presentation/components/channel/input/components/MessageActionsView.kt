package com.sceyt.chatuikit.presentation.components.channel.input.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytFragmentMessageActionsBinding
import com.sceyt.chatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.MessageInputClickListeners.CancelReplyMessageViewClickListener
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.shared.utils.ViewUtil
import com.sceyt.chatuikit.styles.input.InputEditMessageStyle
import com.sceyt.chatuikit.styles.input.InputReplyMessageStyle
import com.sceyt.chatuikit.styles.input.MessageInputStyle

@Suppress("MemberVisibilityCanBePrivate", "JoinDeclarationAndAssignment")
class MessageActionsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: SceytFragmentMessageActionsBinding
    private var clickListeners: CancelReplyMessageViewClickListener? = null
    private lateinit var inputStyle: MessageInputStyle

    init {
        binding = SceytFragmentMessageActionsBinding.inflate(LayoutInflater.from(context), this)
        initViews()
    }

    private fun initViews() {
        binding.icCancel.setOnClickListener {
            clickListeners?.onCancelReplyMessageViewClick(it)
        }
    }

    fun editMessage(message: SceytMessage) {
        val style = inputStyle.editMessageStyle
        with(binding) {
            applyEditStyle(style)
            setCancelIcon(inputStyle.closeIcon)

            if (!root.isVisible || root.height != root.measuredHeight || root.measuredHeight in (0..1)) {
                root.isVisible = true
                ViewUtil.expandHeight(root, 0, 200)
            }

            val replyTitle = SpannableStringBuilder("${context.getString(R.string.sceyt_edit_message)}:")
            style.titleTextStyle.apply(context, replyTitle)
            tvName.text = replyTitle

            loadAttachmentImage(message.attachments, style.attachmentIconProvider)

            tvMessageBody.text = style.messageBodyFormatter.format(
                context = context,
                from = MessageBodyFormatterAttributes(
                    message = message,
                    mentionTextStyle = style.mentionTextStyle
                )
            )
        }
    }

    fun replyMessage(message: SceytMessage) {
        val style = inputStyle.replyMessageStyle
        with(binding) {
            applyRelyStyle(style)
            setCancelIcon(inputStyle.closeIcon)

            if (!root.isVisible || root.height != root.measuredHeight || root.measuredHeight in (0..1)) {
                root.isVisible = true
                ViewUtil.expandHeight(root, 1, 200)
            }

            val replyTitle = SpannableStringBuilder("${context.getString(R.string.sceyt_reply)}:")
            style.titleTextStyle.apply(context, replyTitle)

            val senderName = message.user?.let {
                val name = SpannableStringBuilder(style.senderNameFormatter.format(context, it))
                style.senderNameTextStyle.apply(context, name)
                name
            } ?: ""

            tvName.text = buildSpannedString {
                append(replyTitle)
                append(" ")
                append(senderName)
            }

            loadAttachmentImage(message.attachments, style.attachmentIconProvider)

            tvMessageBody.text = if ((MessageTypeEnum.fromValue(message.type)) == null) {
                context.getString(R.string.unsupported_message_text)
            } else {
                style.messageBodyFormatter.format(
                    context = context,
                    from = MessageBodyFormatterAttributes(
                        message = message,
                        mentionTextStyle = style.mentionTextStyle
                    )
                )
            }
        }
    }

    private fun SceytFragmentMessageActionsBinding.applyRelyStyle(style: InputReplyMessageStyle) {
        root.setBackgroundColor(style.backgroundColor)
        setReplyOrEditIcon(style.replyIcon)
        style.titleTextStyle.apply(tvName)
        style.bodyTextStyle.apply(tvMessageBody)
    }

    private fun SceytFragmentMessageActionsBinding.applyEditStyle(style: InputEditMessageStyle) {
        root.setBackgroundColor(style.backgroundColor)
        setReplyOrEditIcon(style.editIcon)
        style.titleTextStyle.apply(tvName)
        style.bodyTextStyle.apply(tvMessageBody)
    }

    private fun setReplyOrEditIcon(icon: Drawable?) {
        binding.icReplyOrEdit.apply {
            setImageDrawable(icon)
            isVisible = icon != null
        }
    }

    private fun setCancelIcon(icon: Drawable?) {
        binding.icCancel.setImageDrawable(icon)
    }

    fun close(readyCb: (() -> Unit?)? = null) {
        with(binding) {
            ViewUtil.collapseHeight(root, to = 1, duration = 200) {
                root.isVisible = false
                readyCb?.invoke()
            }
        }
    }

    fun setClickListener(clickListeners: CancelReplyMessageViewClickListener) {
        this.clickListeners = clickListeners
    }

    private fun loadAttachmentImage(
            attachments: List<SceytAttachment>?,
            attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
    ) {
        with(binding) {
            if (attachments.isNullOrEmpty()) {
                layoutImage.isVisible = false
                return
            }
            layoutImage.isVisible = true
            imageAttachment.isVisible = true
            fileAttachment.isVisible = false
            val (links, others) = attachments.partition { it.type == AttachmentTypeEnum.Link.value }
            if (others.isNotEmpty()) {
                val attachment = others[0]
                when {
                    attachment.type.isEqualsVideoOrImage() -> {
                        loadImage(imageAttachment, attachment.metadata, attachment.filePath)
                    }

                    attachment.type == AttachmentTypeEnum.Voice.value -> {
                        layoutImage.isVisible = false
                    }

                    else -> {
                        val icon = attachmentIconProvider.provide(context, attachment)
                        fileAttachment.setImageDrawable(icon)
                        fileAttachment.isVisible = true
                        imageAttachment.isVisible = false
                    }
                }
            } else {
                val attachment = links[0]
                val icon = attachmentIconProvider.provide(context, attachment)
                attachment.linkPreviewDetails?.imageUrl?.let {
                    loadImage(imageAttachment, attachment.metadata, it, icon)
                } ?: imageAttachment.setImageDrawable(icon)
            }
        }
    }

    private fun loadImage(
            imageAttachment: ImageView,
            metadata: String?,
            path: String?,
            defaultPlaceHolder: Drawable? = null,
            error: Drawable? = null
    ) {
        val placeHolder = getThumbFromMetadata(metadata)?.toDrawable(context.resources)
            ?.mutate() ?: defaultPlaceHolder
        Glide.with(context)
            .load(path)
            .placeholder(placeHolder)
            .override(100)
            .error(error)
            .into(imageAttachment)
    }

    internal fun setStyle(inputStyle: MessageInputStyle) {
        this.inputStyle = inputStyle
        binding.applyStyle(inputStyle)
    }

    private fun SceytFragmentMessageActionsBinding.applyStyle(inputStyle: MessageInputStyle) {
        fileAttachment.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
        icCancel.setImageDrawable(inputStyle.closeIcon)
    }
}