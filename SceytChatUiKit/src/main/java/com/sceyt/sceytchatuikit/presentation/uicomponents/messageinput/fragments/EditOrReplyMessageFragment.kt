package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.sceytchatuikit.extensions.setBoldSpan
import com.sceyt.sceytchatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.sceytchatuikit.presentation.common.getShowBody
import com.sceyt.sceytchatuikit.presentation.common.isTextMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MessageBodyStyleHelper
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.MessageInputViewStyle
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle
import com.sceyt.sceytchatuikit.shared.utils.ViewUtil

open class EditOrReplyMessageFragment : Fragment() {
    protected var binding: SceytFragmentEditOrReplyMessageBinding? = null
    protected var clickListeners: MessageInputClickListeners.CloseReplyMessageViewClickListener? = null
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
            ViewUtil.expandHeight(root, 1, 200)
            icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_edit)
            layoutImage.isVisible = false
            tvName.text = getString(R.string.sceyt_edit_message)
            tvMessageBody.text = if (message.isTextMessage())
                MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(message)
            else message.getShowBody(root.context)
        }
    }

    open fun replyMessage(message: SceytMessage) {
        with(binding ?: return) {
            root.isVisible = true
            if (!root.isVisible || root.height <= 1)
                ViewUtil.expandHeight(root, 1, 200)
            val name = message.user?.let { userNameBuilder?.invoke(it) }
                    ?: message.user?.getPresentableName() ?: ""
            val text = "${getString(R.string.sceyt_reply)} $name".run {
                setBoldSpan(length - name.length, length)
            }
            tvName.text = text
            icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_input_reply)

            if (!message.attachments.isNullOrEmpty()) {
                layoutImage.isVisible = true
                loadReplyMessageImage(message.attachments?.getOrNull(0))
            } else layoutImage.isVisible = false

            tvMessageBody.text = if (message.isTextMessage())
                MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(message)
            else message.getShowBody(root.context)
        }
    }

    open fun cancelReply(readyCb: (() -> Unit?)? = null) {
        with(binding ?: return) {
            ViewUtil.collapseHeight(root, to = 1, duration = 200) {
                root.isVisible = false
                readyCb?.invoke()
            }
        }
    }

    open fun setClickListener(clickListeners: MessageInputClickListeners.CloseReplyMessageViewClickListener) {
        this.clickListeners = clickListeners
    }

    protected open fun loadReplyMessageImage(attachment: SceytAttachment?) {
        attachment ?: return
        with(binding ?: return) {
            when {
                attachment.type.isEqualsVideoOrImage() -> {
                    val placeHolder = getThumbFromMetadata(attachment.metadata)?.toDrawable(requireContext().resources)?.mutate()
                    Glide.with(requireContext())
                        .load(attachment.filePath)
                        .placeholder(placeHolder)
                        .override(100)
                        .error(placeHolder)
                        .into(imageAttachment)
                }

                attachment.type == AttachmentTypeEnum.Voice.value() || attachment.type == AttachmentTypeEnum.Link.value() -> {
                    layoutImage.isVisible = false
                }

                else -> imageAttachment.setImageResource(MessagesStyle.fileAttachmentIcon)
            }
        }
    }

    private fun SceytFragmentEditOrReplyMessageBinding.setupStyle() {
        icReplyOrEdit.setColorFilter(requireContext().getCompatColorByTheme(SceytKitConfig.sceytColorAccent))
        tvName.setTextColor(requireContext().getCompatColorByTheme(MessageInputViewStyle.userNameTextColor))
    }
}