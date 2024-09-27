package com.sceyt.chatuikit.presentation.components.channel.input.fragments

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytFragmentMessageActionsBinding
import com.sceyt.chatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.MessageInputClickListeners.CancelReplyMessageViewClickListener
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.shared.utils.ViewUtil
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.input.InputEditMessageStyle
import com.sceyt.chatuikit.styles.input.InputReplyMessageStyle
import com.sceyt.chatuikit.styles.input.MessageInputStyle

@Suppress("MemberVisibilityCanBePrivate")
open class MessageActionsFragment : Fragment() {
    protected var binding: SceytFragmentMessageActionsBinding? = null
    protected var clickListeners: CancelReplyMessageViewClickListener? = null
    protected var userNameFormatter = SceytChatUIKit.formatters.userNameFormatterNew
    protected lateinit var inputStyle: MessageInputStyle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentMessageActionsBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    protected fun initViews() {
        binding?.icCancel?.setOnClickListener {
            clickListeners?.onCancelReplyMessageViewClick(it)
        }
    }

    open fun editMessage(message: SceytMessage) {
        val style = inputStyle.editMessageStyle
        with(binding ?: return) {
            applyEditStyle(style)
            setCancelIcon(inputStyle.closeIcon)

            if (!root.isVisible || root.height != root.measuredHeight || root.measuredHeight == 0) {
                root.isVisible = true
                ViewUtil.expandHeight(root, 0, 200)
            }

            val replyTitle = SpannableStringBuilder("${getString(R.string.sceyt_edit_message)}:")
            style.titleTextStyle.apply(requireContext(), replyTitle)
            tvName.text = replyTitle

            loadAttachmentImage(message.attachments, style.attachmentIconProvider)

            tvMessageBody.text = message.getFormattedBody(root.context, TextStyle(
                style = Typeface.BOLD,
            ), SceytChatUIKit.formatters.attachmentNameFormatter)
        }
    }

    open fun replyMessage(message: SceytMessage) {
        val style = inputStyle.replyMessageStyle
        with(binding ?: return) {
            applyRelyStyle(style)
            setCancelIcon(inputStyle.closeIcon)

            if (!root.isVisible || root.height != root.measuredHeight || root.measuredHeight == 0) {
                root.isVisible = true
                ViewUtil.expandHeight(root, 0, 200)
            }

            val replyTitle = SpannableStringBuilder("${getString(R.string.sceyt_reply)}:")
            style.titleTextStyle.apply(requireContext(), replyTitle)

            val senderName = message.user?.let {
                val name = SpannableStringBuilder(userNameFormatter.format(requireContext(), it))
                style.senderNameTextStyle.apply(requireContext(), name)
                name
            } ?: ""

            tvName.text = buildSpannedString {
                append(replyTitle)
                append(" ")
                append(senderName)
            }

            loadAttachmentImage(message.attachments, style.attachmentIconProvider)

            tvMessageBody.text = message.getFormattedBody(
                context = requireContext(),
                mentionTextStyle = style.mentionTextStyle,
                attachmentNameFormatter = SceytChatUIKit.formatters.attachmentNameFormatter
            )
        }
    }

    protected open fun SceytFragmentMessageActionsBinding.applyRelyStyle(
            style: InputReplyMessageStyle
    ) {
        root.setBackgroundColor(style.backgroundColor)
        setReplyOrEditIcon(style.replyIcon)
        style.titleTextStyle.apply(tvName)
        style.bodyTextStyle.apply(tvMessageBody)
    }

    protected open fun SceytFragmentMessageActionsBinding.applyEditStyle(
            style: InputEditMessageStyle
    ) {
        root.setBackgroundColor(style.backgroundColor)
        setReplyOrEditIcon(style.editIcon)
        style.titleTextStyle.apply(tvName)
        style.bodyTextStyle.apply(tvMessageBody)
    }

    protected open fun setReplyOrEditIcon(icon: Drawable?) {
        binding?.icReplyOrEdit?.apply {
            setImageDrawable(icon)
            isVisible = icon != null
        }
    }

    protected open fun setCancelIcon(icon: Drawable?) {
        binding?.icCancel?.setImageDrawable(icon)
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

    protected open fun loadAttachmentImage(
            attachments: List<SceytAttachment>?,
            attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
    ) {
        with(binding ?: return) {
            if (attachments.isNullOrEmpty()) {
                layoutImage.isVisible = false
                return
            }
            layoutImage.isVisible = true
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
                        val icon = attachmentIconProvider.provide(requireContext(), attachment)
                        fileAttachment.setImageDrawable(icon)
                        fileAttachment.isVisible = true
                        imageAttachment.isVisible = false
                    }
                }
            } else {
                val attachment = links[0]
                val icon = attachmentIconProvider.provide(requireContext(), attachment)
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
        val placeHolder = getThumbFromMetadata(metadata)?.toDrawable(requireContext().resources)
            ?.mutate() ?: defaultPlaceHolder
        Glide.with(requireContext())
            .load(path)
            .placeholder(placeHolder)
            .override(100)
            .error(error)
            .into(imageAttachment)
    }

    internal fun setStyle(inputStyle: MessageInputStyle) {
        this.inputStyle = inputStyle
        binding?.applyStyle(inputStyle)
    }

    private fun SceytFragmentMessageActionsBinding.applyStyle(inputStyle: MessageInputStyle) {
        fileAttachment.setBackgroundTintColorRes(SceytChatUIKit.theme.accentColor)
        icCancel.setImageDrawable(inputStyle.closeIcon)
    }
}