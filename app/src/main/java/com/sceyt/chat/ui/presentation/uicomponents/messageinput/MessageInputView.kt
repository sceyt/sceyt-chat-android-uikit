package com.sceyt.chat.ui.presentation.uicomponents.messageinput

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import com.sceyt.chat.ui.data.toPublicChannel
import com.sceyt.chat.ui.databinding.SceytMessageInputViewBinding
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.presentation.common.chooseAttachment.AttachmentChooseType
import com.sceyt.chat.ui.presentation.common.chooseAttachment.ChooseAttachmentHelper
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.isTextMessage
import com.sceyt.chat.ui.presentation.uicomponents.conversation.dialogs.ChooseFileTypeDialog
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.adapter.AttachmentItem
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.adapter.AttachmentsAdapter
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.listeners.MessageInputClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.listeners.MessageInputClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessageInputViewStyle
import com.sceyt.chat.ui.utils.ViewUtil
import kotlinx.coroutines.*
import java.io.File

class MessageInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageInputClickListeners.ClickListeners {
    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private var allAttachments = mutableListOf<Attachment>()
    private val binding: SceytMessageInputViewBinding
    private var clickListeners = MessageInputClickListenersImpl(this)
    private var chooseAttachmentHelper: ChooseAttachmentHelper? = null
    private var typingJob: Job? = null

    init {
        if (!isInEditMode)
            chooseAttachmentHelper = ChooseAttachmentHelper(context.asAppCompatActivity())
    }

    var messageInputActionCallback: MessageInputActionCallback? = null
    var message: Message? = null
        set(value) {
            field = value
            if (value != null) {
                binding.messageInput.setText(message?.body)
                binding.messageInput.text?.let { text -> binding.messageInput.setSelection(text.length) }
                context.showSoftInput(binding.messageInput)
            }
        }

    private var replayMessage: Message? = null
    private var replayThreadMessageId: Long? = null

    init {
        binding = SceytMessageInputViewBinding.inflate(LayoutInflater.from(context), this, true)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.MessageInputView)
            MessageInputViewStyle.updateWithAttributes(context, a)
            a.recycle()
        }
        init()
        setupAttachmentsList()
    }


    private fun init() {
        with(binding) {
            setUpStyle()
            determineState()

            messageInput.doOnTextChanged { text, _, _, _ ->
                determineState()
                messageInputActionCallback?.typing(text.isNullOrBlank().not())
                typingJob?.cancel()
                typingJob = CoroutineScope(Dispatchers.Main + Job()).launch {
                    messageInputActionCallback?.typing(text.isNullOrBlank().not())
                    delay(2000)
                    messageInputActionCallback?.typing(false)
                }
            }

            icSendMessage.setOnClickListener {
                clickListeners.onSendMsgClick(it)
            }

            icAddAttachments.setOnClickListener {
                clickListeners.onSendAttachmentClick(it)
            }

            layoutReplayMessage.icCancelReplay.setOnClickListener {
                clickListeners.onCancelReplayMessageViewClick(it)
            }

            btnJoin.setOnClickListener {
                clickListeners.onJoinClick()
            }
        }
    }

    private fun sendMessage() {
        val messageBody = binding.messageInput.text.toString().trim()
        if (messageBody != "" || allAttachments.isNotEmpty()) {
            if (message != null) {
                message?.body = messageBody
                message?.let {
                    messageInputActionCallback?.sendEditMessage(it)
                }
            } else {
                val messageToSend: Message? = Message.MessageBuilder()
                    .setAttachments(allAttachments.toTypedArray())
                    .setType(getMessageType(allAttachments))
                    .setBody(binding.messageInput.text.toString())
                    .apply {
                        replayMessage?.let {
                            setParentMessageId(it.id)
                            setReplyInThread(replayThreadMessageId != null)
                        } ?: replayThreadMessageId?.let {
                            setParentMessageId(it)
                            setReplyInThread(true)
                        }
                    }.build()

                if (replayMessage != null)
                    messageToSend?.let { msg -> messageInputActionCallback?.sendReplayMessage(msg, replayMessage) }
                else
                    messageToSend?.let { msg -> messageInputActionCallback?.sendMessage(msg) }
            }
            reset()
        }
    }

    private fun handleAttachmentClick() {
        ChooseFileTypeDialog(context) { chooseType ->
            when (chooseType) {
                AttachmentChooseType.Gallery -> {
                    chooseAttachmentHelper?.chooseFromGallery(allowMultiple = true, onlyImages = false) {
                        addAttachmentFile(*it.toTypedArray())
                    }
                }
                AttachmentChooseType.Camera -> {
                    chooseAttachmentHelper?.takePicture {
                        addAttachmentFile(it)
                    }
                }
                AttachmentChooseType.File -> {
                    chooseAttachmentHelper?.chooseMultipleFiles(allowMultiple = true) {
                        addAttachmentFile(*it.toTypedArray())
                    }
                }
            }
        }.show()
    }

    private fun SceytMessageInputViewBinding.setUpStyle() {
        icAddAttachments.setImageResource(MessageInputViewStyle.attachmentIcon)
        icSendMessage.setImageResource(MessageInputViewStyle.sendMessageIcon)
        messageInput.setTextColor(context.getCompatColor(MessageInputViewStyle.inputTextColor))
        messageInput.hint = MessageInputViewStyle.inputHintText
        messageInput.setHintTextColor(context.getCompatColor(MessageInputViewStyle.inputHintTextColor))
    }

    private fun getMessageType(attachments: List<Attachment>): String {
        if (attachments.isNotEmpty() && attachments.size == 1) {
            return if (attachments[0].type.isEqualsVideoOrImage())
                "media"
            else "file"
        }
        return "text"
    }

    private fun reset() {
        message = null
        replayMessage = null
        allAttachments.clear()
        attachmentsAdapter.clear()
        binding.messageInput.text = null
        determineState()
    }

    private fun determineState() {
        if (binding.messageInput.text?.trim().isNullOrEmpty() && allAttachments.isEmpty()) {
            binding.icSendMessage.alpha = 0.5f
        } else
            binding.icSendMessage.alpha = 1f
    }

    private fun addAttachments(attachments: List<Attachment>) {
        allAttachments.addAll(attachments)
        attachmentsAdapter.addItems(attachments.map { AttachmentItem(it) })
        determineState()
    }

    private fun setupAttachmentsList() {
        attachmentsAdapter = AttachmentsAdapter(allAttachments.map { AttachmentItem(it) } as ArrayList<AttachmentItem>) {
            clickListeners.onRemoveAttachmentClick(it)
        }

        binding.rvAttachments.adapter = attachmentsAdapter
    }

    private fun addAttachmentFile(vararg filePath: String) {
        val attachments = mutableListOf<Attachment>()

        filePath.forEach { item ->
            val attachment = Attachment.Builder(item, getAttachmentType(item))
                .setName(File(item).name)
                .setMetadata(Gson().toJson(AttachmentMetadata(item)))
                .setUpload(true)
                .build()

            attachments.add(attachment)
        }
        addAttachments(attachments)
    }

    private fun getAttachmentType(path: String?): String {
        return when (val type = getMimeTypeTakeFirstPart(path)) {
            "image", "video" -> type
            else -> "file"
        }
    }

    internal fun replayMessage(message: Message) {
        replayMessage = message
        with(binding.layoutReplayMessage) {
            isVisible = true
            ViewUtil.expandHeight(root, 1, 200)
            tvName.text = message.from.fullName.trim()
            tvMessageBody.text = if (message.isTextMessage())
                message.body.trim() else context.getString(R.string.attachment)
        }
    }

    internal fun cancelReplay(readyCb: (() -> Unit?)? = null) {
        if (replayMessage == null)
            readyCb?.invoke()
        else {
            replayMessage = null
            ViewUtil.collapseHeight(binding.layoutReplayMessage.root, to = 1, duration = 200) {
                binding.layoutReplayMessage.root.isVisible = false
                context.asAppCompatActivity().lifecycleScope.launchWhenResumed { readyCb?.invoke() }
            }
        }
    }

    internal fun setReplayInThreadMessageId(messageId: Long?) {
        replayThreadMessageId = messageId
    }

    internal fun checkIsParticipant(channel: SceytChannel) {
        if (channel.channelType == ChannelTypeEnum.Public) {
            if (channel.toPublicChannel().myRole() == Member.MemberType.MemberTypeNone) {
                binding.btnJoin.isVisible = true
                binding.layoutInput.isVisible = false
            }
        }
    }

    internal fun joinSuccess() {
        binding.btnJoin.isVisible = false
        binding.layoutInput.isVisible = true
    }

    internal fun onChannelLeft(){
        binding.btnJoin.isVisible = true
        binding.layoutInput.isVisible = false
    }

    interface MessageInputActionCallback {
        fun sendMessage(message: Message)
        fun sendReplayMessage(message: Message, parent: Message?)
        fun sendEditMessage(message: Message)
        fun typing(typing: Boolean)
        fun join()
    }

    fun setClickListener(listener: MessageInputClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setCustomClickListener(listener: MessageInputClickListenersImpl) {
        clickListeners = listener
    }

    override fun onSendMsgClick(view: View) {
        sendMessage()
    }

    override fun onSendAttachmentClick(view: View) {
        handleAttachmentClick()
    }

    override fun onCancelReplayMessageViewClick(view: View) {
        cancelReplay()
    }

    override fun onRemoveAttachmentClick(item: AttachmentItem) {
        attachmentsAdapter.removeItem(item)
        allAttachments.remove(item.attachment)
        determineState()
    }

    override fun onJoinClick() {
        messageInputActionCallback?.join()
    }
}