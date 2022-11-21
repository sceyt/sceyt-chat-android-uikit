package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentMetadata
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.databinding.SceytMessageInputViewBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.imagepicker.GalleryMediaPicker
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.presentation.common.isTextMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.dialogs.ChooseFileTypeDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.AttachmentItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.AttachmentsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.MessageInputClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.MessageInputClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.SelectFileTypePopupClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.SelectFileTypePopupClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessageInputViewStyle
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
                    messageInputActionCallback?.sendEditMessage(it.toSceytUiMessage())
                }
            } else {
                val messageToSend: Message? = Message.MessageBuilder()
                    .setAttachments(allAttachments.toTypedArray())
                    .setType(getMessageType(allAttachments, messageBody))
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
        with(layoutReplayMessage) {
            horizontalView.backgroundTintList = ColorStateList.valueOf(context.getCompatColorByTheme(MessageInputViewStyle.horizontalLineColor))
            tvName.setTextColor(context.getCompatColorByTheme(MessageInputViewStyle.userNameTextColor))
        }
    }

    private fun getMessageType(attachments: List<Attachment>, body: String?): String {
        if (attachments.isNotEmpty() && attachments.size == 1) {
            return if (attachments[0].type.isEqualsVideoOrImage())
                MessageTypeEnum.Media.value()
            else MessageTypeEnum.File.value()
        }
        return if (body.isLink()) MessageTypeEnum.Link.value() else MessageTypeEnum.Text.value()
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

    internal fun replayMessage(message: Message) {
        replayMessage = message
        with(binding.layoutReplayMessage) {
            isVisible = true
            ViewUtil.expandHeight(root, 1, 200)
            tvName.text = userNameBuilder?.invoke(message.from) ?: message.from.getPresentableName()
            tvMessageBody.text = if (message.isTextMessage())
                message.body.trim() else context.getString(R.string.sceyt_attachment)
        }
    }

    internal fun cancelReplay(readyCb: (() -> Unit?)? = null) {
        if (replayMessage == null)
            readyCb?.invoke()
        else {
            replayMessage = null
            ViewUtil.collapseHeight(binding.layoutReplayMessage.root, to = 1, duration = 200) {
                binding.layoutReplayMessage.root.isVisible = false
                context.asComponentActivity().lifecycleScope.launchWhenResumed { readyCb?.invoke() }
            }
        }
    }

    internal fun setReplayInThreadMessageId(messageId: Long?) {
        replayThreadMessageId = messageId
    }

    internal fun checkIsParticipant(channel: SceytChannel) {
        if (channel.channelType == ChannelTypeEnum.Public) {
            if (channel.toGroupChannel().members.find { it.id == preferences.getUserId() } == null) {
                showHideJoinButton(true)
            } else showHideJoinButton(false)
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

    interface MessageInputActionCallback {
        fun sendMessage(message: Message)
        fun sendReplayMessage(message: Message, parent: Message?)
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


    fun send() {
        GlobalScope.launch {
            for (i in 1..30) {
                sed(i)
            }
        }

    }

    suspend fun sed(i: Int) {
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
        GalleryMediaPicker().apply {
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