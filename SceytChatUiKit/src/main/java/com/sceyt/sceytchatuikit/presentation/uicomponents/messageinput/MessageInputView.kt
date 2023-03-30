package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.databinding.SceytMessageInputViewBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.imagepicker.GalleryMediaPicker
import com.sceyt.sceytchatuikit.media.audio.AudioRecorderHelper
import com.sceyt.sceytchatuikit.persistence.constants.SceytConstants
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.mappers.getAttachmentType
import com.sceyt.sceytchatuikit.persistence.mappers.getInfoFromMetadataByKey
import com.sceyt.sceytchatuikit.persistence.mappers.getMessageTypeFromAttachments
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog
import com.sceyt.sceytchatuikit.presentation.common.getShowBody
import com.sceyt.sceytchatuikit.presentation.common.isTextMessage
import com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder.AudioMetadata
import com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder.RecordingListener
import com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder.SceytRecordedVoicePresenter
import com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder.SceytVoiceMessageRecorderView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.dialogs.ChooseFileTypeDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.InputState.Text
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.InputState.Voice
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentsViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners.InputEventsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners.InputEventsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserData
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper.getAsObjectDataIndexed
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.mentionsrc.TokenCompleteTextView.ObjectDataIndexed
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.sceytconfigs.MessageInputViewStyle
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.AttachmentChooseType
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.sceyt.sceytchatuikit.shared.utils.ViewUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.io.File

class MessageInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageInputClickListeners.ClickListeners,
        SelectFileTypePopupClickListeners.ClickListeners, InputEventsListener.InputEventListeners, SceytKoinComponent {

    private val preferences by inject<SceytSharedPreference>()
    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private var allAttachments = mutableListOf<Attachment>()
    private val binding: SceytMessageInputViewBinding
    private var clickListeners = MessageInputClickListenersImpl(this)
    private var eventListeners = InputEventsListenerImpl(this)
    private var selectFileTypePopupClickListeners = SelectFileTypePopupClickListenersImpl(this)
    private var chooseAttachmentHelper: ChooseAttachmentHelper? = null
    private var typingJob: Job? = null
    private var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
    private var inputState = Voice
    private var disabledInput: Boolean = false
    private var showingJoinButton: Boolean = false
    private var voiceMessageRecorderView: SceytVoiceMessageRecorderView? = null
    private var mentionUserContainer: MentionUserContainer? = null
    private val mentionUserDebounceHelper by lazy { DebounceHelper(200, context.asComponentActivity().lifecycleScope) }

    var messageInputActionCallback: MessageInputActionCallback? = null
    private var editMessage: Message? = null
        set(value) {
            field = value
            if (value != null) {
                with(binding) {
                    messageInput.setText(value.body)
                    messageInput.text?.let { text -> messageInput.setSelection(text.length) }
                    if (!value.mentionedUsers.isNullOrEmpty()) {
                        val data = getAsObjectDataIndexed(value.metadata, value.mentionedUsers)
                        messageInput.setMentionUsers(data)
                    }
                    context.showSoftInput(messageInput)
                }
            }
        }

    var replyMessage: Message? = null
        private set
    var replyThreadMessageId: Long? = null
        private set

    init {
        if (!isInEditMode)
            chooseAttachmentHelper = ChooseAttachmentHelper(context.asComponentActivity())

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.MessageInputView)
            MessageInputViewStyle.updateWithAttributes(context, a)
            a.recycle()
        }
        binding = SceytMessageInputViewBinding.inflate(LayoutInflater.from(context), this, true)

        init()
        setupAttachmentsList()

        post {
            (parent as? ViewGroup)?.addView(SceytVoiceMessageRecorderView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                setRecordingListener()
                voiceMessageRecorderView = this
                isVisible = !disabledInput && !showingJoinButton
            })
        }

        Handler(Looper.getMainLooper()).postDelayed({ binding.messageInput.requestFocus() }, 500)
    }

    private fun init() {
        with(binding) {
            setUpStyle()
            determineState()
            post { onStateChanged(inputState) }

            messageInput.doAfterTextChanged { text ->
                if (isRecording())
                    return@doAfterTextChanged

                determineState()
                checkNeedMentionUsers(text)
                typingJob?.cancel()
                typingJob = MainScope().launch {
                    messageInputActionCallback?.typing(text.isNullOrBlank().not())
                    messageInputActionCallback?.updateDraftMessage(text, binding.messageInput.objectsIndexed)
                    delay(2000)
                    messageInputActionCallback?.typing(false)
                }
            }

            icSendMessage.setOnClickListener {
                when (inputState) {
                    Text -> clickListeners.onSendMsgClick(it)
                    Voice -> clickListeners.onVoiceClick(it)
                }
            }

            icSendMessage.setOnLongClickListener {
                if (inputState == Voice) clickListeners.onVoiceLongClick(it)
                return@setOnLongClickListener true
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

    private fun checkNeedMentionUsers(inputText: Editable?) {
        mentionUserDebounceHelper.submit {
            if (inputText.isNullOrBlank() || inputText.last() == ' ') {
                setMentionList(emptyList())
                return@submit
            }

            val lastWord = inputText.toString().replace("\n", " ").split(" ").lastOrNull()
            if (lastWord?.startsWith("@") == true) {
                val mentionText = lastWord.removePrefix("@")
                eventListeners.onMentionUsersListener(mentionText)
            } else
                setMentionList(emptyList())
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
                    checkAndAddMentionedUsers(it)
                }
                editMessage?.let {
                    cancelReply {
                        messageInputActionCallback?.sendEditMessage(it.toSceytUiMessage())
                        reset()
                    }
                }
            } else {
                cancelReply {
                    val link = getLinkAttachmentFromBody()
                    if (allAttachments.isNotEmpty()) {
                        val messages = arrayListOf<Message>()
                        allAttachments.forEachIndexed { index, attachment ->
                            val attachments = arrayListOf(attachment)
                            val message = Message.MessageBuilder()
                                .setType(getMessageTypeFromAttachments(if (index == 0) messageBody else null, attachment))
                                .apply {
                                    if (index == 0) {
                                        if (link != null)
                                            attachments.add(link)
                                        setAttachments(attachments.toTypedArray())
                                        setBody(messageBody)
                                        replyMessage?.let {
                                            setParentMessageId(it.id)
                                            setParentMessage(it)
                                            setReplyInThread(replyThreadMessageId != null)
                                        } ?: replyThreadMessageId?.let {
                                            setParentMessageId(it)
                                            setReplyInThread(true)
                                        }
                                    } else setAttachments(arrayOf(attachment))
                                }.build().apply {
                                    if (index == 0) checkAndAddMentionedUsers(this)
                                }

                            messages.add(message)
                        }
                        messageInputActionCallback?.sendMessages(messages)
                    } else {
                        val attachment = if (link != null) arrayOf(link) else arrayOf()
                        val message = Message.MessageBuilder()
                            .setAttachments(attachment)
                            .setType(getMessageTypeFromAttachments(messageBody))
                            .setBody(messageBody)
                            .apply {
                                replyMessage?.let {
                                    setParentMessageId(it.id)
                                    setParentMessage(it)
                                    setReplyInThread(replyThreadMessageId != null)
                                } ?: replyThreadMessageId?.let {
                                    setParentMessageId(it)
                                    setReplyInThread(true)
                                }
                            }.build()

                        checkAndAddMentionedUsers(message)
                        messageInputActionCallback?.sendMessage(message)
                    }
                    reset()
                }
            }
        }
    }

    private fun checkAndAddMentionedUsers(messageBuilder: Message) {
        val mentionedUsers = binding.messageInput.objectsIndexed
        if (mentionedUsers.isEmpty()) {
            messageBuilder.metadata = ""
            messageBuilder.mentionedUsers = null
            return
        }

        MentionUserHelper.initMentionMetaData(binding.messageInput.text.toString(), mentionedUsers).let {
            messageBuilder.metadata = it
        }
        messageBuilder.mentionedUsers = mentionedUsers.map { it.token.user }.toTypedArray()
    }

    private fun tryToSendRecording(file: File, amplitudes: IntArray, duration: Int) {
        val metadata = Gson().toJson(AudioMetadata(amplitudes, duration))
        createAttachmentWithPaths(file.path, metadata = metadata, attachmentType = AttachmentTypeEnum.Voice.value()).getOrNull(0)?.let {
            allAttachments.add(it)
            sendMessage()
        } ?: finishRecording()
    }

    private fun finishRecording() {
        binding.layoutInput.isVisible = true
        voiceMessageRecorderView?.isVisible = true
        binding.voiceRecordPresenter.isVisible = false
        binding.messageInput.setText("")
        determineState()
        binding.messageInput.requestFocus()
    }

    private fun SceytVoiceMessageRecorderView.setRecordingListener() {
        setListener(object : RecordingListener {
            override fun onRecordingStarted() {
                val directoryToSaveRecording = context.filesDir.path + "/Audio"
                AudioRecorderHelper.startRecording(directoryToSaveRecording) {}
                binding.layoutInput.isInvisible = true
            }

            override fun onRecordingCompleted(shouldShowPreview: Boolean) {
                AudioRecorderHelper.stopRecording { isTooShort, file, duration, amplitudes ->
                    runOnMainThread {
                        if (isTooShort) {
                            finishRecording()
                            return@runOnMainThread
                        }
                        if (shouldShowPreview) {
                            showRecordPreview(file, amplitudes, duration)
                        } else {
                            finishRecording()
                            tryToSendRecording(file, amplitudes.toIntArray(), duration)
                        }
                    }
                }
            }

            override fun onRecordingCanceled() {
                AudioRecorderHelper.cancelRecording {}
                finishRecording()
            }
        })
    }

    private fun showRecordPreview(file: File, amplitudes: Array<Int>, duration: Int) {
        val metadata = AudioMetadata(amplitudes.toIntArray(), duration)
        binding.voiceRecordPresenter.init(file, metadata, object : SceytRecordedVoicePresenter.RecordedVoicePresentListeners {
            override fun onDeleteVoiceRecord() {
                file.deleteOnExit()
                finishRecording()
            }

            override fun onSendVoiceMessage() {
                tryToSendRecording(file, amplitudes.toIntArray(), duration)
                finishRecording()
            }
        })
        voiceMessageRecorderView?.isVisible = false
        binding.voiceRecordPresenter.isVisible = true
    }

    private fun getLinkAttachmentFromBody(): Attachment? {
        val body = binding.messageInput.text.toString()
        val links = body.extractLinks()
        val isContainsLink = links.isNotEmpty()
        if (isContainsLink) {
            return Attachment.Builder("", links[0], AttachmentTypeEnum.Link.value())
                .withTid(ClientWrapper.generateTid())
                .setName("")
                .setMetadata("")
                .build()
        }
        return null
    }

    private fun handleAttachmentClick() {
        ChooseFileTypeDialog(context) { chooseType ->
            when (chooseType) {
                AttachmentChooseType.Gallery -> {
                    selectFileTypePopupClickListeners.onGalleryClick()
                }
                AttachmentChooseType.Camera -> {
                    //TODO custom camera impl
                }
                AttachmentChooseType.Image -> {
                    selectFileTypePopupClickListeners.onTakePhotoClick()
                }
                AttachmentChooseType.Video -> {
                    selectFileTypePopupClickListeners.onTakeVideoClick()
                }
                AttachmentChooseType.File -> {
                    selectFileTypePopupClickListeners.onFileClick()
                }
            }
        }.show()
    }

    private fun SceytMessageInputViewBinding.setUpStyle() {
        icAddAttachments.setImageResource(MessageInputViewStyle.attachmentIcon)
        messageInput.setTextColor(context.getCompatColor(MessageInputViewStyle.inputTextColor))
        messageInput.hint = MessageInputViewStyle.inputHintText
        messageInput.setHintTextColor(context.getCompatColor(MessageInputViewStyle.inputHintTextColor))
        btnJoin.setTextColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        with(layoutReplyOrEditMessage) {
            icReplyOrEdit.setColorFilter(context.getCompatColorByTheme(SceytKitConfig.sceytColorAccent))
            tvName.setTextColor(context.getCompatColorByTheme(MessageInputViewStyle.userNameTextColor))
        }
    }

    private fun determineState() {
        val showVoiceIcon = binding.messageInput.text?.trim().isNullOrEmpty() && allAttachments.isEmpty()
                && !binding.voiceRecordPresenter.isShowing
        val newState = if (showVoiceIcon) Voice else Text
        if (inputState != newState)
            onStateChanged(newState)
        inputState = newState

        binding.icSendMessage.isVisible = !showVoiceIcon && !disabledInput
        if (!showVoiceIcon || disabledInput) {
            hideAndStopVoiceRecorder()
        } else showVoiceRecorder()
    }

    private fun addAttachments(attachments: List<Attachment>) {
        binding.rvAttachments.isVisible = true
        allAttachments.addAll(attachments)
        attachmentsAdapter.addItems(attachments.map { AttachmentItem(it) })
        determineState()
    }

    private fun setupAttachmentsList() {
        attachmentsAdapter = AttachmentsAdapter(allAttachments.map { AttachmentItem(it) }.toArrayList(),
            AttachmentsViewHolderFactory(context).also {
                it.setClickListener(AttachmentClickListeners.RemoveAttachmentClickListener { _, item ->
                    clickListeners.onRemoveAttachmentClick(item)
                })
            })
        binding.rvAttachments.adapter = attachmentsAdapter
    }

    private fun showHideJoinButton(show: Boolean) {
        showingJoinButton = show
        binding.btnJoin.isVisible = show
        binding.layoutInput.isVisible = !disabledInput && !show
        binding.layoutCloseInput.root.isVisible = disabledInput && !show
        if (!show) determineState()
    }

    private fun checkIsExistAttachment(path: String?): Boolean {
        return allAttachments.map { it.filePath }.contains(path)
    }

    private fun cancelReply(readyCb: (() -> Unit?)? = null) {
        if (replyMessage == null && editMessage == null)
            readyCb?.invoke()
        else {
            ViewUtil.collapseHeight(binding.layoutReplyOrEditMessage.root, to = 1, duration = 200) {
                binding.layoutReplyOrEditMessage.root.isVisible = false
                context.asComponentActivity().lifecycleScope.launch { readyCb?.invoke() }
            }
        }
    }

    private fun loadReplyMessageImage(attachment: Attachment) {
        when {
            attachment.type.isEqualsVideoOrImage() -> {
                val placeHolder = attachment.metadata.getInfoFromMetadataByKey(SceytConstants.Thumb)?.toByteArraySafety()
                    ?.decodeByteArrayToBitmap()?.toDrawable(context.resources)?.mutate()
                Glide.with(context)
                    .load(attachment.filePath)
                    .placeholder(placeHolder)
                    .override(100)
                    .error(placeHolder)
                    .into(binding.layoutReplyOrEditMessage.imageAttachment)
            }
            attachment.type == AttachmentTypeEnum.Voice.value() || attachment.type == AttachmentTypeEnum.Link.value() -> {
                binding.layoutReplyOrEditMessage.layoutImage.isVisible = false
            }
            else -> binding.layoutReplyOrEditMessage.imageAttachment.setImageResource(MessagesStyle.fileAttachmentIcon)
        }
    }

    private fun hideInputWithMessage(message: String, @DrawableRes startIcon: Int) {
        binding.layoutCloseInput.apply {
            tvMessage.text = message
            icStateIcon.setImageResource(startIcon)
            root.isVisible = true
        }
        binding.layoutInput.isVisible = false
        binding.btnJoin.isVisible = false
        voiceMessageRecorderView?.isVisible = false
    }

    private fun showInput() {
        binding.layoutCloseInput.root.isVisible = false
        if (isRecording())
            binding.layoutInput.isInvisible = true
        else
            binding.layoutInput.isVisible = true
        determineState()
    }

    private fun showVoiceRecorder() {
        voiceMessageRecorderView?.isVisible = true
    }

    private fun hideAndStopVoiceRecorder() {
        voiceMessageRecorderView?.isVisible = false
        voiceMessageRecorderView?.forceStopRecording()
    }

    private fun initMentionUsersContainer() {
        if (mentionUserContainer == null)
            (parent as? ViewGroup)?.addView(MentionUserContainer(context).apply {
                mentionUserContainer = initWithMessageInputView(this@MessageInputView).also {
                    setUserClickListener {
                        binding.messageInput.appendObjectSyncAsync(MentionUserData(it))
                    }
                }
            })
    }

    private fun onStateChanged(newState: InputState) {
        eventListeners.onInputStateChanged(binding.icSendMessage, newState)
    }

    internal fun replyMessage(message: Message) {
        checkIfRecordingAndConfirm {
            replyMessage = message
            with(binding.layoutReplyOrEditMessage) {
                isVisible = true
                ViewUtil.expandHeight(root, 1, 200)
                val name = userNameBuilder?.invoke(message.from)
                        ?: message.from.getPresentableName()
                val text = "${getString(R.string.sceyt_reply)} $name".run {
                    setBoldSpan(length - name.length, length)
                }
                tvName.text = text
                icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_input_reply)

                if (message.attachments.isNullOrEmpty().not()) {
                    binding.layoutReplyOrEditMessage.layoutImage.isVisible = true
                    loadReplyMessageImage(message.attachments[0])
                } else binding.layoutReplyOrEditMessage.layoutImage.isVisible = false

                tvMessageBody.text = if (message.isTextMessage())
                    MentionUserHelper.buildOnlyNamesWithMentionedUsers(message.body, message.metadata, message.mentionedUsers)
                else message.toSceytUiMessage().getShowBody(context)
            }
        }
    }

    internal fun editMessage(message: Message) {
        checkIfRecordingAndConfirm {
            editMessage = message
            with(binding.layoutReplyOrEditMessage) {
                isVisible = true
                ViewUtil.expandHeight(root, 1, 200)
                icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_edit)
                layoutImage.isVisible = false
                tvName.text = getString(R.string.sceyt_edit_message)
                tvMessageBody.text = if (message.isTextMessage())
                    MentionUserHelper.buildOnlyNamesWithMentionedUsers(message.body, message.metadata, message.mentionedUsers)
                else message.toSceytUiMessage().getShowBody(context)
            }
        }
    }

    internal fun setReplyInThreadMessageId(messageId: Long?) {
        replyThreadMessageId = messageId
    }

    internal fun setDraftMessage(draftMessage: DraftMessage?) {
        if (draftMessage == null || draftMessage.message.isNullOrEmpty())
            return
        with(binding.messageInput) {
            setTextAndMoveSelectionEnd(draftMessage.message)
            if (!draftMessage.mentionUsers.isNullOrEmpty()) {
                val data = getAsObjectDataIndexed(draftMessage.metadata, draftMessage.mentionUsers.toTypedArray())
                setMentionUsers(data)
            }
        }
        determineState()
    }

    internal fun checkIsParticipant(channel: SceytChannel) {
        when (channel.channelType) {
            ChannelTypeEnum.Public -> {
                if (channel.toGroupChannel().lastActiveMembers.find { it.id == preferences.getUserId() } == null) {
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
                    if (isBlockedPeer)
                        hideInputWithMessage(getString(R.string.sceyt_you_blocked_this_user), R.drawable.sceyt_ic_warning)
                    else {
                        if (disabledInput.not())
                            showInput()
                    }
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

    fun checkIfRecordingAndConfirm(onConfirm: () -> Unit) {
        if (isRecording()) {
            SceytDialog.showSceytDialog(context, R.string.sceyt_stop_recording, R.string.sceyt_stop_recording_desc, R.string.sceyt_discard) {
                stopRecording()
                onConfirm()
            }
        } else onConfirm()
    }

    fun createAttachmentWithPaths(vararg filePath: String, metadata: String = "", attachmentType: String? = null): MutableList<Attachment> {
        val attachments = mutableListOf<Attachment>()
        for (path in filePath) {
            if (checkIsExistAttachment(path))
                continue

            val file = File(path)
            if (file.exists()) {
                val type = attachmentType ?: getAttachmentType(path).value()
                val attachment = Attachment.Builder(path, null, type)
                    .setName(File(path).name)
                    .withTid(ClientWrapper.generateTid())
                    .setMetadata(metadata)
                    .setCreatedAt(System.currentTimeMillis())
                    .setFileSize(getFileSize(path))
                    .setUpload(false)
                    .build()

                attachments.add(attachment)
            } else
                Toast.makeText(context, "\"${file.name}\" ${getString(R.string.sceyt_unsupported_file_format)}", Toast.LENGTH_SHORT).show()
        }
        return attachments
    }

    fun addAttachment(vararg filePath: String) {
        val attachments = createAttachmentWithPaths(*filePath)
        addAttachments(attachments)
    }

    fun reset(clearInput: Boolean = true) {
        if (clearInput)
            binding.messageInput.text = null
        cancelReply()
        editMessage = null
        replyMessage = null
        allAttachments.clear()
        attachmentsAdapter.clear()
        determineState()
    }

    fun enableDisableInput(message: String, enable: Boolean, @DrawableRes startIcon: Int = R.drawable.sceyt_ic_warning) {
        disabledInput = enable.not()
        if (!enable)
            hideInputWithMessage(message, startIcon)
        else showInput()
    }

    fun stopRecording() {
        voiceMessageRecorderView?.forceStopRecording()
    }

    fun isEmpty() = binding.messageInput.text.isNullOrBlank() && allAttachments.isEmpty()

    fun isRecording() = voiceMessageRecorderView?.isRecording == true

    fun getComposedMessage() = binding.messageInput.text

    interface MessageInputActionCallback {
        fun sendMessage(message: Message)
        fun sendMessages(message: List<Message>)
        fun sendEditMessage(message: SceytMessage)
        fun typing(typing: Boolean)
        fun updateDraftMessage(text: Editable?, mentionUserIds: List<ObjectDataIndexed<MentionUserData>>)
        fun mention(query: String)
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

    fun setEventListener(listener: InputEventsListener) {
        eventListeners.setListener(listener)
    }

    fun setCustomEventListener(listener: InputEventsListenerImpl) {
        eventListeners = listener
    }

    fun setCustomSelectFileTypePopupClickListener(listener: SelectFileTypePopupClickListenersImpl) {
        selectFileTypePopupClickListeners = listener
    }

    fun setMentionList(data: List<SceytMember>) {
        if (data.isEmpty() && mentionUserContainer == null) return
        initMentionUsersContainer()
        mentionUserContainer?.setMentionList(data)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mentionUserContainer?.onInputSizeChanged(h)
    }

    override fun onSendMsgClick(view: View) {
        sendMessage()
    }

    override fun onSendAttachmentClick(view: View) {
        handleAttachmentClick()
    }

    override fun onVoiceClick(view: View) {

    }

    override fun onVoiceLongClick(view: View) {

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

    private fun getPickerListener(): GalleryMediaPicker.PickerListener {
        return GalleryMediaPicker.PickerListener {
            addAttachment(*it.map { mediaData -> mediaData.realPath }.toTypedArray())
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
        GalleryMediaPicker.instance(selections = allAttachments.map { it.url }.toTypedArray()).apply {
            GalleryMediaPicker.pickerListener = getPickerListener()
        }.show(context.asFragmentActivity().supportFragmentManager, GalleryMediaPicker.TAG)
    }

    override fun onTakePhotoClick() {
        chooseAttachmentHelper?.takePicture {
            addAttachment(it)
        }
    }

    override fun onTakeVideoClick() {
        chooseAttachmentHelper?.takeVideo {
            addAttachment(it)
        }
    }

    override fun onFileClick() {
        chooseAttachmentHelper?.chooseMultipleFiles(allowMultiple = true) {
            addAttachment(*it.toTypedArray())
        }
    }

    override fun onInputStateChanged(sendImage: ImageView, state: InputState) {
        val iconResId = if (state == Voice) R.drawable.sceyt_ic_voice
        else MessageInputViewStyle.sendMessageIcon
        binding.icSendMessage.setImageResource(iconResId)
    }

    override fun onMentionUsersListener(query: String) {
        messageInputActionCallback?.mention(query)
    }
}