package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.fragments

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
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytFragmentEditOrReplyMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.sceytchatuikit.extensions.setBoldSpan
import com.sceyt.sceytchatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.sceytchatuikit.presentation.common.getFormattedBody
import com.sceyt.sceytchatuikit.presentation.common.isTextMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.CancelReplyMessageViewClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MessageBodyStyleHelper
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.MessageInputViewStyle
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle
import com.sceyt.sceytchatuikit.shared.utils.ViewUtil

open class EditOrReplyMessageFragment : Fragment() {
    protected var binding: SceytFragmentEditOrReplyMessageBinding? = null
    protected var clickListeners: CancelReplyMessageViewClickListener? = null
    protected var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentEditOrReplyMessageBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.setupStyle()
        initViews()
    }

    protected fun initViews() {
        binding?.icCancelReply?.setOnClickListener {
            clickListeners?.onCancelReplyMessageViewClick(it)
        }
    }

    open fun editMessage(message: SceytMessage) {
        with(binding ?: return) {
            root.isVisible = true
            if (!root.isVisible || root.height != root.measuredHeight)
                ViewUtil.expandHeight(root, 0, 200)
            icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_edit)
            layoutImage.isVisible = false
            tvName.text = getString(R.string.sceyt_edit_message)
            tvMessageBody.text = if (message.isTextMessage())
                MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(message)
            else message.getFormattedBody(root.context)
        }
    }

    open fun replyMessage(message: SceytMessage) {
        with(binding ?: return) {
            if (!root.isVisible || root.height != root.measuredHeight) {
                root.isVisible = true
                ViewUtil.expandHeight(root, 0, 200)
            }
            val name = message.user?.let { userNameBuilder?.invoke(it) }
                    ?: message.user?.getPresentableName() ?: ""
            val text = "${getString(R.string.sceyt_reply)} $name".run {
                setBoldSpan(length - name.length, length)
            }
            tvName.text = text
            icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_input_reply)

            if (!message.attachments.isNullOrEmpty()) {
                layoutImage.isVisible = true
                loadReplyMessageImage(message.attachments)
            } else layoutImage.isVisible = false

            tvMessageBody.text = if (message.isTextMessage())
                MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(message)
            else message.getFormattedBody(root.context)
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

    protected open fun loadReplyMessageImage(attachments: Array<SceytAttachment>?) {
        if (attachments.isNullOrEmpty()) {
            binding?.layoutImage?.isVisible = false
            return
        }
        with(binding ?: return) {
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

                    else -> imageAttachment.setImageResource(MessagesStyle.fileAttachmentIcon)
                }
            } else {
                val attachment = links[0]
                if (attachment.linkPreviewDetails != null && attachment.linkPreviewDetails?.imageUrl != null) {
                    loadImage(imageAttachment, attachment.metadata,
                        attachment.linkPreviewDetails?.imageUrl, getCompatDrawable(MessagesStyle.linkAttachmentIcon))
                } else
                    imageAttachment.setImageResource(MessagesStyle.linkAttachmentIcon)
            }
        }
    }


    private fun loadImage(imageAttachment: ImageView, metadata: String?,
                          path: String?, defaultPlaceHolder: Drawable? = null) {
        val placeHolder = getThumbFromMetadata(metadata)?.toDrawable(requireContext().resources)
            ?.mutate() ?: defaultPlaceHolder
        Glide.with(requireContext())
            .load(path)
            .placeholder(placeHolder)
            .override(100)
            .error(MessagesStyle.linkAttachmentIcon)
            .into(imageAttachment)
    }

    private fun SceytFragmentEditOrReplyMessageBinding.setupStyle() {
        icReplyOrEdit.setColorFilter(requireContext().getCompatColor(SceytKitConfig.sceytColorAccent))
        tvName.setTextColor(requireContext().getCompatColor(MessageInputViewStyle.userNameTextColor))
    }
}