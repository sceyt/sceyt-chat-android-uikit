package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.databinding.SceytMessageInputViewBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.imagepicker.GalleryMediaPicker
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.presentation.common.getShowBody
import com.sceyt.sceytchatuikit.presentation.common.isTextMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.dialogs.ChooseFileTypeDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.AttachmentItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.AttachmentsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.MessageInputClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.MessageInputClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.SelectFileTypePopupClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.SelectFileTypePopupClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessageInputViewStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.AttachmentChooseType
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.sceyt.sceytchatuikit.shared.utils.ViewUtil
import kotlinx.coroutines.*
import org.koin.core.component.inject
import java.io.File

class MessageInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageInputClickListeners.ClickListeners,
        SelectFileTypePopupClickListeners.ClickListeners, SceytKoinComponent {

    private val preferences by inject<SceytSharedPreference>()
    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private var allAttachments = mutableListOf<Attachment>()
    private val binding: SceytMessageInputViewBinding
    private var clickListeners = MessageInputClickListenersImpl(this)
    private var selectFileTypePopupClickListeners = SelectFileTypePopupClickListenersImpl(this)
    private var chooseAttachmentHelper: ChooseAttachmentHelper? = null
    private var typingJob: Job? = null
    private var userNameBuilder: ((User) -> String)? = null

    var messageInputActionCallback: MessageInputActionCallback? = null
    private var editMessage: Message? = null
        set(value) {
            field = value
            if (value != null) {
                binding.messageInput.setText(editMessage?.body)
                binding.messageInput.text?.let { text -> binding.messageInput.setSelection(text.length) }
                context.showSoftInput(binding.messageInput)
            }
        }

    private var replyMessage: Message? = null
    private var replyThreadMessageId: Long? = null

    init {
        if (!isInEditMode)
            chooseAttachmentHelper = ChooseAttachmentHelper(context.asComponentActivity())

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

            layoutReplyOrEditMessage.icCancelReply.setOnClickListener {
                clickListeners.onCancelReplyMessageViewClick(it)
            }

            btnJoin.setOnClickListener {
                clickListeners.onJoinClick()
            }
        }
    }

    private fun sendMessage() {
        val messageBody = binding.messageInput.text.toString().trim()

        if (messageBody != "" || allAttachments.isNotEmpty()) {
            if (editMessage != null) {
                if (editMessage?.body?.trim() == messageBody.trim()) {
                    cancelReply()
                    reset()
                    return
                }
                editMessage?.body = messageBody
                editMessage?.let {
                    cancelReply {
                        messageInputActionCallback?.sendEditMessage(it.toSceytUiMessage())
                        reset()
                    }
                }
            } else {
                cancelReply {
                    if (allAttachments.isNotEmpty()) {
                        allAttachments.forEachIndexed { index, attachment ->
                            val messageToSend: Message? = Message.MessageBuilder()
                                .setAttachments(arrayOf(attachment))
                                .setType(getMessageType(if (index == 0) messageBody else null, attachment))
                                .apply {
                                    if (index == 0) {
                                        setBody(messageBody)
                                        replyMessage?.let {
                                            setParentMessageId(it.id)
                                            setReplyInThread(replyThreadMessageId != null)
                                        } ?: replyThreadMessageId?.let {
                                            setParentMessageId(it)
                                            setReplyInThread(true)
                                        }
                                    }
                                }.build()

                            if (index == 0 && replyMessage != null) {
                                messageToSend?.let { msg -> messageInputActionCallback?.sendReplyMessage(msg, replyMessage) }
                            } else {
                                messageToSend?.let { msg -> messageInputActionCallback?.sendMessage(msg) }
                            }
                        }
                    } else {
                        val messageToSend: Message? = Message.MessageBuilder()
                            .setAttachments(allAttachments.toTypedArray())
                            .setType(getMessageType(messageBody))
                            .setBody(messageBody)
                            .apply {
                                replyMessage?.let {
                                    setParentMessageId(it.id)
                                    setReplyInThread(replyThreadMessageId != null)
                                } ?: replyThreadMessageId?.let {
                                    setParentMessageId(it)
                                    setReplyInThread(true)
                                }
                            }.build()

                        if (replyMessage != null)
                            messageToSend?.let { msg -> messageInputActionCallback?.sendReplyMessage(msg, replyMessage) }
                        else
                            messageToSend?.let { msg -> messageInputActionCallback?.sendMessage(msg) }
                    }
                    reset()
                }
            }
        }
    }

    private fun handleAttachmentClick() {
        ChooseFileTypeDialog(context) { chooseType ->
            when (chooseType) {
                AttachmentChooseType.Gallery -> {
                    selectFileTypePopupClickListeners.onGalleryClick()
                }
                AttachmentChooseType.Camera -> {
                    selectFileTypePopupClickListeners.onTakePhotoClick()
                }
                AttachmentChooseType.File -> {
                    selectFileTypePopupClickListeners.onFileClick()
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
        with(layoutReplyOrEditMessage) {
            icReplyOrEdit.setColorFilter(context.getCompatColorByTheme(SceytKitConfig.sceytColorAccent))
            tvName.setTextColor(context.getCompatColorByTheme(MessageInputViewStyle.userNameTextColor))
        }
    }

    private fun getMessageType(body: String?, vararg attachments: Attachment): String {
        if (attachments.isNotEmpty() && attachments.size == 1) {
            return if (attachments[0].type.isEqualsVideoOrImage())
                MessageTypeEnum.Media.value()
            else MessageTypeEnum.File.value()
        }
        return if (body.isLink()) MessageTypeEnum.Link.value() else MessageTypeEnum.Text.value()
    }

    private fun reset(clearInput: Boolean = true) {
        if (clearInput)
            binding.messageInput.text = null
        editMessage = null
        replyMessage = null
        allAttachments.clear()
        attachmentsAdapter.clear()
        determineState()
    }

    private fun determineState() {
        if (binding.messageInput.text?.trim().isNullOrEmpty() && allAttachments.isEmpty()) {
            binding.icSendMessage.alpha = 0.5f
        } else
            binding.icSendMessage.alpha = 1f
    }

    private fun addAttachments(attachments: List<Attachment>) {
        binding.rvAttachments.isVisible = true
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

    private fun getAttachmentType(path: String?): String {
        return when (val type = getMimeTypeTakeFirstPart(path)) {
            AttachmentTypeEnum.Image.value(), AttachmentTypeEnum.Video.value() -> type
            else -> AttachmentTypeEnum.File.value()
        }
    }

    private fun showHideJoinButton(show: Boolean) {
        binding.btnJoin.isVisible = show
        binding.layoutInput.isVisible = show.not()
    }

    private fun checkIsExistAttachment(path: String?): Boolean {
        return allAttachments.map { it.url }.contains(path)
    }

    private fun cancelReply(readyCb: (() -> Unit?)? = null) {
        if (replyMessage == null && editMessage == null)
            readyCb?.invoke()
        else {
            ViewUtil.collapseHeight(binding.layoutReplyOrEditMessage.root, to = 1, duration = 200) {
                binding.layoutReplyOrEditMessage.root.isVisible = false
                context.asComponentActivity().lifecycleScope.launchWhenResumed { readyCb?.invoke() }
            }
        }
    }

    internal fun replyMessage(message: Message) {
        replyMessage = message
        with(binding.layoutReplyOrEditMessage) {
            isVisible = true
            ViewUtil.expandHeight(root, 1, 200)
            val name = userNameBuilder?.invoke(message.from) ?: message.from.getPresentableName()
            val text = "${getString(R.string.sceyt_reply)} $name".run {
                setBoldSpan(length - name.length, length)
            }
            tvName.text = text
            icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_input_reply)
            tvMessageBody.text = if (message.isTextMessage())
                message.body.trim()
            else message.toSceytUiMessage().getShowBody(context)
        }
    }


    internal fun editMessage(message: Message) {
        editMessage = message
        with(binding.layoutReplyOrEditMessage) {
            isVisible = true
            ViewUtil.expandHeight(root, 1, 200)
            icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_edit)
            tvName.text = getString(R.string.sceyt_edit_message)
            tvMessageBody.text = if (message.isTextMessage())
                message.body.trim()
            else message.toSceytUiMessage().getShowBody(context)
        }
    }

    internal fun setReplyInThreadMessageId(messageId: Long?) {
        replyThreadMessageId = messageId
    }

    internal fun checkIsParticipant(channel: SceytChannel) {
        when (channel.channelType) {
            ChannelTypeEnum.Public -> {
                if (channel.toGroupChannel().members.find { it.id == preferences.getUserId() } == null) {
                    showHideJoinButton(true)
                } else showHideJoinButton(false)
            }
            ChannelTypeEnum.Direct -> {
                val isBlockedPeer = (channel as? SceytDirectChannel)?.peer?.user?.blocked == true
                with(binding) {
                    if (isBlockedPeer) {
                        rvAttachments.isVisible = false
                        layoutReplyOrEditMessage.root.isVisible = false
                    }
                    layoutInput.isVisible = !isBlockedPeer
                    layoutUserBlocked.root.isVisible = (channel as? SceytDirectChannel)?.peer?.user?.blocked == true
                }
            }
            else -> {}
        }
    }

    internal fun joinSuccess() {
        showHideJoinButton(false)
    }

    internal fun onChannelLeft() {
        showHideJoinButton(true)
    }

    fun addAttachmentFile(vararg filePath: String) {
        val attachments = mutableListOf<Attachment>()
        for (path in filePath) {
            if (checkIsExistAttachment(path))
                continue

            val file = File(path)
            if (file.exists()) {
                val attachment = Attachment.Builder(path, null, getAttachmentType(path))
                    .setName(File(path).name)
                    .setFileSize(getFileSize(path))
                    .setUpload(false)
                    .build()

                attachments.add(attachment)
            } else
                Toast.makeText(context, "\"${file.name}\" ${getString(R.string.sceyt_unsupported_file_format)}", Toast.LENGTH_SHORT).show()
        }
        addAttachments(attachments)
    }

    interface MessageInputActionCallback {
        fun sendMessage(message: Message)
        fun sendReplyMessage(message: Message, parent: Message?)
        fun sendEditMessage(message: SceytMessage)
        fun typing(typing: Boolean)
        fun join()
    }

    fun setClickListener(listener: MessageInputClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    fun setCustomClickListener(listener: MessageInputClickListenersImpl) {
        clickListeners = listener
    }

    fun setCustomSelectFileTypePopupClickListener(listener: SelectFileTypePopupClickListenersImpl) {
        selectFileTypePopupClickListeners = listener
    }

    override fun onSendMsgClick(view: View) {
        sendMessage()
    }

    override fun onSendAttachmentClick(view: View) {
        handleAttachmentClick()
    }

    override fun onCancelReplyMessageViewClick(view: View) {
        cancelReply()
        reset(replyMessage == null)
    }

    override fun onRemoveAttachmentClick(item: AttachmentItem) {
        attachmentsAdapter.removeItem(item)
        allAttachments.remove(item.attachment)
        determineState()
    }

    override fun onJoinClick() {
        messageInputActionCallback?.join()
    }


    fun send() {
        GlobalScope.launch {
            for (i in 1..30)
                send(i)
        }
    }

    suspend fun send(i: Int) {
        delay(200)
        withContext(Dispatchers.Main) {
            binding.messageInput.setText(i.toString())
            sendMessage()
        }
    }

    private fun getPickerListener(): GalleryMediaPicker.PickerListener {
        return GalleryMediaPicker.PickerListener {
            addAttachmentFile(*it.map { mediaData -> mediaData.realPath }.toTypedArray())
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        GalleryMediaPicker.pickerListener?.let {
            GalleryMediaPicker.pickerListener = getPickerListener()
        }
    }

    // Choose file type popup listeners
    override fun onGalleryClick() {
        GalleryMediaPicker.instance(*allAttachments.map { it.url }.toTypedArray()).apply {
            GalleryMediaPicker.pickerListener = getPickerListener()
        }.show(context.asFragmentActivity().supportFragmentManager, GalleryMediaPicker.TAG)
    }

    override fun onTakePhotoClick() {
        chooseAttachmentHelper?.takePicture {
            addAttachmentFile(it)
        }
    }

    override fun onFileClick() {
        chooseAttachmentHelper?.chooseMultipleFiles(allowMultiple = true) {
            addAttachmentFile(*it.toTypedArray())
        }
    }
}