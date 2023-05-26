package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
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
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.DraftMessage
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.databinding.SceytMessageInputViewBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.asFragmentActivity
import com.sceyt.sceytchatuikit.extensions.decodeByteArrayToBitmap
import com.sceyt.sceytchatuikit.extensions.extractLinks
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.sceytchatuikit.extensions.runOnMainThread
import com.sceyt.sceytchatuikit.extensions.setBoldSpan
import com.sceyt.sceytchatuikit.extensions.setTextAndMoveSelectionEnd
import com.sceyt.sceytchatuikit.extensions.showSoftInput
import com.sceyt.sceytchatuikit.extensions.toByteArraySafety
import com.sceyt.sceytchatuikit.imagepicker.GalleryMediaPicker
import com.sceyt.sceytchatuikit.media.audio.AudioRecorderHelper
import com.sceyt.sceytchatuikit.persistence.constants.SceytConstants
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.mappers.createEmptyUser
import com.sceyt.sceytchatuikit.persistence.mappers.getAttachmentType
import com.sceyt.sceytchatuikit.persistence.mappers.getInfoFromMetadataByKey
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
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.AttachmentClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.SelectFileTypePopupClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.SelectFileTypePopupClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners.InputEventsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners.InputEventsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionAnnotation
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionValidatorWatcher
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQuery
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQueryChangedListener
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
    private var disabledInputByGesture: Boolean = false
    private var enableMention: Boolean = true
    private var voiceMessageRecorderView: SceytVoiceMessageRecorderView? = null
    private var mentionUserContainer: MentionUserContainer? = null
    var messageInputActionCallback: MessageInputActionCallback? = null

    var isInputHidden = false
        private set
    var editMessage: Message? = null
        private set
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
                isVisible = canShowRecorderView()
            })
        }

        Handler(Looper.getMainLooper()).postDelayed({ binding.messageInput.requestFocus() }, 500)
    }

    private fun init() {
        with(binding) {
            setUpStyle()
            setOnClickListeners()
            addMentionUserListener()
            determineInputState()
            post { onStateChanged(inputState) }

            messageInput.doAfterTextChanged { text ->
                if (isRecording())
                    return@doAfterTextChanged

                determineInputState()
                typingJob?.cancel()
                typingJob = MainScope().launch {
                    messageInputActionCallback?.typing(text.isNullOrBlank().not())
                    messageInputActionCallback?.updateDraftMessage(text, binding.messageInput.mentions)
                    delay(2000)
                    messageInputActionCallback?.typing(false)
                }
            }
        }
    }

    private fun SceytMessageInputViewBinding.setOnClickListeners() {
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
            clickListeners.onAddAttachmentClick(it)
        }

        layoutReplyOrEditMessage.icCancelReply.setOnClickListener {
            clickListeners.onCancelReplyMessageViewClick(it)
        }

        btnJoin.setOnClickListener {
            clickListeners.onJoinClick()
        }
    }

    private fun addMentionUserListener() {
        binding.messageInput.setInlineQueryChangedListener(object : InlineQueryChangedListener {
            override fun onQueryChanged(inlineQuery: InlineQuery) {
                when (inlineQuery) {
                    is InlineQuery.Mention -> {
                        if (enableMention)
                            eventListeners.onMentionUsersListener(inlineQuery.query)
                    }

                    else -> setMentionList(emptyList())
                }
            }
        })
    }

    private fun sendMessage() {
        val messageBody = binding.messageInput.text.toString().trim()
        if (messageBody.isEmpty() && allAttachments.isEmpty()) return

        if (!checkIsEditingMessage(messageBody)) {
            cancelReply {
                val link = getLinkAttachmentFromBody()
                if (allAttachments.isNotEmpty()) {
                    val messages = arrayListOf<Message>()
                    allAttachments.forEachIndexed { index, attachment ->
                        var attachments = arrayOf(attachment)
                        val message = if (index == 0) {
                            if (link != null)
                                attachments = attachments.plus(link)
                            buildMessage(messageBody, attachments, true)
                        } else buildMessage("", attachments, false)

                        messages.add(message)
                    }
                    messageInputActionCallback?.sendMessages(messages)
                } else {
                    val attachment = if (link != null) arrayOf(link) else arrayOf()
                    messageInputActionCallback?.sendMessage(buildMessage(messageBody, attachment, true))
                }
                reset()
            }
        }
    }

    private fun buildMessage(body: String, attachments: Array<Attachment>, withMentionedUsers: Boolean): Message {
        val message = Message.MessageBuilder()
            .setTid(ClientWrapper.generateTid())
            .setAttachments(attachments)
            .setType("text")
            .setBody(body)
            .initRelyMessage()
            .build()

        if (withMentionedUsers)
            checkAndAddMentionedUsers(message)

        return message
    }

    private fun Message.MessageBuilder.initRelyMessage(): Message.MessageBuilder {
        replyMessage?.let {
            setParentMessageId(it.id)
            setParentMessage(it)
            setReplyInThread(replyThreadMessageId != null)
        } ?: replyThreadMessageId?.let {
            setParentMessageId(it)
            setReplyInThread(true)
        }
        return this
    }

    private fun checkIsEditingMessage(messageBody: String): Boolean {
        if (editMessage != null) {
            if (editMessage?.body?.trim() == messageBody.trim()) {
                cancelReply()
                reset()
                return true
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
            return true
        }
        return false
    }

    private fun checkAndAddMentionedUsers(message: Message) {
        val mentionedUsers = binding.messageInput.mentions
        if (mentionedUsers.isEmpty()) {
            message.metadata = ""
            message.mentionedUsers = null
            return
        }
        MentionUserHelper.initMentionMetaData(binding.messageInput.text.toString(), mentionedUsers).let {
            message.metadata = it
        }
        message.mentionedUsers = mentionedUsers.map { createEmptyUser(it.recipientId, it.name) }.toTypedArray()
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
        showVoiceRecorder()
        binding.voiceRecordPresenter.isVisible = false
        binding.messageInput.setText("")
        determineInputState()
        binding.messageInput.requestFocus()
    }

    private fun canShowRecorderView() = !disabledInputByGesture && !isInputHidden && inputState == Voice

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
                .setCreatedAt(System.currentTimeMillis())
                .setUpload(false)
                .build()
        }
        return null
    }

    private fun handleAttachmentClick() {
        ChooseFileTypeDialog(context) { chooseType ->
            when (chooseType) {
                AttachmentChooseType.Gallery -> selectFileTypePopupClickListeners.onGalleryClick()
                AttachmentChooseType.Camera -> {
                    //TODO custom camera impl
                }

                AttachmentChooseType.Image -> selectFileTypePopupClickListeners.onTakePhotoClick()
                AttachmentChooseType.Video -> selectFileTypePopupClickListeners.onTakeVideoClick()
                AttachmentChooseType.File -> selectFileTypePopupClickListeners.onFileClick()
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

    private fun determineInputState() {
        if (!isEnabledInput())
            return

        val showVoiceIcon = binding.messageInput.text?.trim().isNullOrEmpty() && allAttachments.isEmpty()
                && !binding.voiceRecordPresenter.isShowing
        val newState = if (showVoiceIcon) Voice else Text
        if (inputState != newState)
            onStateChanged(newState)
        inputState = newState

        binding.icSendMessage.isVisible = !showVoiceIcon
        if (showVoiceIcon) {
            showVoiceRecorder()
        } else hideAndStopVoiceRecorder()
    }

    private fun isEnabledInput() = !disabledInputByGesture && !isInputHidden

    private fun addAttachments(attachments: List<Attachment>) {
        binding.rvAttachments.isVisible = true
        allAttachments.addAll(attachments)
        attachmentsAdapter.addItems(attachments.map { AttachmentItem(it) })
        determineInputState()
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
        isInputHidden = show
        binding.btnJoin.isVisible = show
        binding.layoutInput.isVisible = !disabledInputByGesture && !show
        binding.layoutCloseInput.root.isVisible = disabledInputByGesture && !show
        voiceMessageRecorderView?.isVisible = canShowRecorderView()
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
        with(binding) {
            layoutCloseInput.apply {
                tvMessage.text = message
                icStateIcon.setImageResource(startIcon)
                root.isVisible = true
            }
            layoutInput.isInvisible = true
            messageInput.setText("")
            btnJoin.isVisible = false
            rvAttachments.isVisible = false
            hideAndStopVoiceRecorder()
        }
    }

    private fun showInput() {
        if (isRecording())
            binding.layoutInput.isInvisible = true
        else
            binding.layoutInput.isVisible = true

        binding.layoutCloseInput.root.isVisible = false
        determineInputState()
    }

    private fun showVoiceRecorder() {
        if (canShowRecorderView())
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
                        binding.messageInput.replaceTextWithMention(SceytKitConfig.userNameBuilder?.invoke(it.user)
                                ?: it.getPresentableName(), it.id)
                    }
                }
            })
    }

    private fun onStateChanged(newState: InputState) {
        eventListeners.onInputStateChanged(binding.icSendMessage, newState)
    }

    private fun initInputWithEditMessage(message: Message) {
        with(binding) {
            var body = SpannableString(message.body)
            if (!message.mentionedUsers.isNullOrEmpty()) {
                val data = MentionUserHelper.getMentionsIndexed(message.metadata, message.mentionedUsers)
                val updatedBody = MentionUserHelper.updateBodyWithUsers(message.body, message.metadata, message.mentionedUsers)
                body = MentionAnnotation.setMentionAnnotations(SpannableString(updatedBody), data)
            }
            messageInput.setText(body)
            messageInput.text?.let { text -> messageInput.setSelection(text.length) }
            context.showSoftInput(messageInput)
        }
    }

    internal fun editMessage(message: Message) {
        checkIfRecordingAndConfirm {
            editMessage = message
            initInputWithEditMessage(message)
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

    internal fun setReplyInThreadMessageId(messageId: Long?) {
        replyThreadMessageId = messageId
    }

    internal fun setDraftMessage(draftMessage: DraftMessage?) {
        if (draftMessage == null || draftMessage.message.isNullOrEmpty())
            return
        var body = SpannableString(draftMessage.message)
        with(binding.messageInput) {
            if (!draftMessage.mentionUsers.isNullOrEmpty()) {
                val data = MentionUserHelper.getMentionsIndexed(draftMessage.metadata, draftMessage.mentionUsers.toTypedArray())
                val updatedBody = MentionUserHelper.updateBodyWithUsers(draftMessage.message,
                    draftMessage.metadata, draftMessage.mentionUsers.toTypedArray())
                body = MentionAnnotation.setMentionAnnotations(SpannableString(updatedBody), data)
            }
            setTextAndMoveSelectionEnd(body)
        }
        determineInputState()
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
                    isInputHidden = if (isBlockedPeer) {
                        hideInputWithMessage(getString(R.string.sceyt_you_blocked_this_user), R.drawable.sceyt_ic_warning)
                        true
                    } else {
                        if (disabledInputByGesture.not())
                            showInput()
                        false
                    }
                }
            }

            else -> return
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
        determineInputState()
    }

    fun disableInputWithMessage(message: String, @DrawableRes startIcon: Int = R.drawable.sceyt_ic_warning) {
        disabledInputByGesture = true
        hideInputWithMessage(message, startIcon)
    }

    fun enableInput() {
        disabledInputByGesture = false
        if (!isInputHidden)
            showInput()
    }

    fun setMentionValidator(validator: MentionValidatorWatcher.MentionValidator) {
        binding.messageInput.setMentionValidator(validator)
    }

    fun enableDisableMention(enable: Boolean) {
        enableMention = enable
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
        fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>)
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

    fun setSaveUrlsPlace(savePaths: MutableSet<String>) {
        chooseAttachmentHelper?.setSaveUrlsPlace(savePaths)
    }

    fun setMentionList(data: List<SceytMember>) {
        if (data.isEmpty() && mentionUserContainer == null) return
        initMentionUsersContainer()
        mentionUserContainer?.setMentionList(data.take(30))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mentionUserContainer?.onInputSizeChanged(h)
    }

    override fun onSendMsgClick(view: View) {
        sendMessage()
    }

    override fun onAddAttachmentClick(view: View) {
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
        determineInputState()
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