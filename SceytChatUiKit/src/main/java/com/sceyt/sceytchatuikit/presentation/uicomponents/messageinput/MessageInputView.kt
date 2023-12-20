package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
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
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.DraftMessage
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytMessageInputViewBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.sceytchatuikit.extensions.notAutoCorrectable
import com.sceyt.sceytchatuikit.extensions.runOnMainThread
import com.sceyt.sceytchatuikit.extensions.setBoldSpan
import com.sceyt.sceytchatuikit.extensions.setTextAndMoveSelectionEnd
import com.sceyt.sceytchatuikit.extensions.showSoftInput
import com.sceyt.sceytchatuikit.imagepicker.GalleryMediaPicker
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper
import com.sceyt.sceytchatuikit.media.audio.AudioRecorderHelper
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.mappers.getThumbFromMetadata
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.getFormattedBody
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
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MessageBodyStyleHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQuery
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.inlinequery.InlineQueryChangedListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.MessageInputViewStyle
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.AttachmentChooseType
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.sceyt.sceytchatuikit.shared.utils.ViewUtil
import com.vanniktech.ui.animateToGone
import com.vanniktech.ui.animateToVisible
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MessageInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageInputClickListeners.ClickListeners,
        SelectFileTypePopupClickListeners.ClickListeners, InputEventsListener.InputEventListeners, SceytKoinComponent {

    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private var allAttachments = mutableListOf<Attachment>()
    private val binding: SceytMessageInputViewBinding
    private var clickListeners = MessageInputClickListenersImpl(this)
    private var eventListeners = InputEventsListenerImpl(this)
    private var selectFileTypePopupClickListeners = SelectFileTypePopupClickListenersImpl(this)
    private var chooseAttachmentHelper: ChooseAttachmentHelper? = null
    private val typingDebounceHelper = DebounceHelper(100)
    private var typingTimeoutJob: Job? = null
    private var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
    private var inputState = Voice
    private var disabledInputByGesture: Boolean = false
    private var enableMention: Boolean = true
    private var voiceMessageRecorderView: SceytVoiceMessageRecorderView? = null
    private var mentionUserContainer: MentionUserContainer? = null
    private var inputTextWatcher: TextWatcher? = null
    private var messageInputActionCallback: MessageInputActionCallback? = null
    private val messageToSendHelper by lazy { MessageToSendHelper(context) }

    var isInputHidden = false
        private set
    var editMessage: SceytMessage? = null
        private set
    var replyMessage: SceytMessage? = null
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
        val voiceRecorderView = SceytVoiceMessageRecorderView(context)
        post {
            (parent as? ViewGroup)?.addView(voiceRecorderView.apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                setRecordingListener()
                voiceMessageRecorderView = this
                isVisible = canShowRecorderView()
            })
        }
    }

    private fun init() {
        with(binding) {
            setUpStyle()
            setOnClickListeners()
            addInoutListeners()
            determineInputState()
            addInputTextWatcher()
            post { onStateChanged(inputState) }
        }
    }

    private fun addInputTextWatcher() {
        inputTextWatcher = binding.messageInput.doAfterTextChanged { text ->
            onInputChanged(text)
        }
    }

    private fun onInputChanged(text: Editable?) {
        if (isRecording())
            return

        determineInputState()

        typingTimeoutJob?.cancel()
        typingTimeoutJob = MainScope().launch {
            delay(2000)
            messageInputActionCallback?.typing(false)
        }

        typingDebounceHelper.submit {
            messageInputActionCallback?.typing(text.isNullOrBlank().not())
            updateDraftMessage()
        }
    }

    private fun updateDraftMessage() {
        val replyOrEditMessage = replyMessage ?: editMessage
        val isReply = replyMessage != null
        with(binding.messageInput) {
            messageInputActionCallback?.updateDraftMessage(text, mentions, styling, replyOrEditMessage, isReply)
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

        btnClearChat.setOnClickListener {
            clickListeners.onClearChatClick()
        }
    }

    private fun addInoutListeners() {
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

        binding.messageInput.setStylingChangedListener(::updateDraftMessage)
    }

    private fun sendMessage() {
        val body = binding.messageInput.text
        if (body.isNullOrBlank() && allAttachments.isEmpty() && editMessage?.attachments.isNullOrEmpty()) {
            if (isEditingMessage())
                customToastSnackBar(this, context.getString(R.string.sceyt_empty_message_body_message))
            return
        }

        cancelReply {
            messageToSendHelper.sendMessage(allAttachments, body, editMessage, replyMessage, replyThreadMessageId)
            reset()
        }
    }

    private fun isEditingMessage() = editMessage != null

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
                AudioPlayerHelper.pauseAll()
                AudioRecorderHelper.startRecording(directoryToSaveRecording) {}
                binding.layoutInput.isInvisible = true
                voiceMessageRecorderView?.keepScreenOn = true
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
                    voiceMessageRecorderView?.keepScreenOn = false
                }
            }

            override fun onRecordingCanceled() {
                AudioRecorderHelper.cancelRecording {}
                finishRecording()
                voiceMessageRecorderView?.keepScreenOn = false
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
        btnClearChat.setTextColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        with(layoutReplyOrEditMessage) {
            icReplyOrEdit.setColorFilter(context.getCompatColorByTheme(SceytKitConfig.sceytColorAccent))
            tvName.setTextColor(context.getCompatColorByTheme(MessageInputViewStyle.userNameTextColor))
        }
    }

    private fun determineInputState() {
        if (!isEnabledInput())
            return

        val showVoiceIcon = binding.messageInput.text?.trim().isNullOrEmpty() && allAttachments.isEmpty()
                && !binding.voiceRecordPresenter.isShowing && !isEditingMessage()
        val newState = if (showVoiceIcon) Voice else Text
        if (inputState != newState)
            onStateChanged(newState)
        inputState = newState

        binding.icSendMessage.isVisible = !showVoiceIcon
        binding.icAddAttachments.isVisible = !isEditingMessage()
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

    private fun loadReplyMessageImage(attachment: SceytAttachment?) {
        attachment ?: return
        when {
            attachment.type.isEqualsVideoOrImage() -> {
                val placeHolder = getThumbFromMetadata(attachment.metadata)?.toDrawable(context.resources)?.mutate()
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
                        val name = (MentionUserHelper.userNameBuilder?.invoke(it.user)
                                ?: it.getPresentableName()).notAutoCorrectable()
                        binding.messageInput.replaceTextWithMention(name, it.id)
                    }
                }
            })
    }

    private fun onStateChanged(newState: InputState) {
        eventListeners.onInputStateChanged(binding.icSendMessage, newState)
    }

    private fun initInputWithEditMessage(message: SceytMessage) {
        with(binding) {
            var body = MessageBodyStyleHelper.buildOnlyStylesWithAttributes(message.body, message.bodyAttributes)
            if (!message.mentionedUsers.isNullOrEmpty()) {
                val data = MentionUserHelper.getMentionsIndexed(message.bodyAttributes, message.mentionedUsers)
                body = MentionAnnotation.setMentionAnnotations(body, data)
            }
            messageInput.setText(body)
            messageInput.text?.let { text -> messageInput.setSelection(text.length) }
            context.showSoftInput(messageInput)
        }
    }

    internal fun editMessage(message: SceytMessage, initWithDraft: Boolean) {
        checkIfRecordingAndConfirm {
            replyMessage = null
            editMessage = message.clone()
            determineInputState()
            if (!initWithDraft)
                initInputWithEditMessage(message)
            with(binding.layoutReplyOrEditMessage) {
                isVisible = true
                ViewUtil.expandHeight(root, 1, 200)
                icReplyOrEdit.setImageResource(R.drawable.sceyt_ic_edit)
                layoutImage.isVisible = false
                tvName.text = getString(R.string.sceyt_edit_message)
                tvMessageBody.text = if (message.isTextMessage())
                    MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(message)
                else message.getFormattedBody(context)
            }
            if (!initWithDraft)
                updateDraftMessage()
        }
    }

    internal fun replyMessage(message: SceytMessage, initWithDraft: Boolean) {
        checkIfRecordingAndConfirm {
            editMessage = null
            replyMessage = message.clone()
            with(binding.layoutReplyOrEditMessage) {
                isVisible = true
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
                    binding.layoutReplyOrEditMessage.layoutImage.isVisible = true
                    loadReplyMessageImage(message.attachments?.getOrNull(0))
                } else binding.layoutReplyOrEditMessage.layoutImage.isVisible = false

                tvMessageBody.text = if (message.isTextMessage())
                    MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(message)
                else message.getFormattedBody(context)
            }

            if (!initWithDraft) {
                context.showSoftInput(binding.messageInput)
                updateDraftMessage()
            }
        }
    }

    internal fun setReplyInThreadMessageId(messageId: Long?) {
        replyThreadMessageId = messageId
    }

    internal fun setDraftMessage(draftMessage: DraftMessage?) {
        if (draftMessage == null || draftMessage.message.isNullOrEmpty())
            return
        var body: CharSequence = draftMessage.message
        binding.messageInput.removeTextChangedListener(inputTextWatcher)
        with(binding.messageInput) {
            body = MessageBodyStyleHelper.buildWithMentionsAndAttributes(context, body.toString(),
                draftMessage.mentionUsers?.toTypedArray(), draftMessage.bodyAttributes)

            if (!draftMessage.mentionUsers.isNullOrEmpty()) {
                val data = MentionUserHelper.getMentionsIndexed(draftMessage.bodyAttributes, draftMessage.mentionUsers.toTypedArray())
                body = MentionAnnotation.setMentionAnnotations(body, data)
            }
            setTextAndMoveSelectionEnd(body)

            if (draftMessage.replyOrEditMessage != null)
                if (draftMessage.isReply)
                    replyMessage(draftMessage.replyOrEditMessage, initWithDraft = true)
                else editMessage(draftMessage.replyOrEditMessage, initWithDraft = true)
        }
        determineInputState()
        addInputTextWatcher()
    }

    internal fun checkIsParticipant(channel: SceytChannel) {
        when (channel.getChannelType()) {
            ChannelTypeEnum.Public, ChannelTypeEnum.Broadcast -> {
                if (channel.userRole.isNullOrBlank()) {
                    showHideJoinButton(true)
                } else showHideJoinButton(false)
            }

            ChannelTypeEnum.Direct -> {
                val isBlockedPeer = channel.getFirstMember()?.user?.blocked == true
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

    internal fun getEventListeners() = eventListeners

    fun setInputActionCallback(callback: MessageInputActionCallback) {
        messageInputActionCallback = callback
        messageToSendHelper.setInputActionCallback(callback)
    }

    @SuppressWarnings("WeakerAccess")
    fun checkIfRecordingAndConfirm(onConfirm: () -> Unit) {
        if (isRecording()) {
            SceytDialog.showSceytDialog(context, R.string.sceyt_stop_recording,
                R.string.sceyt_stop_recording_desc, R.string.sceyt_discard, positiveCb = {
                    stopRecording()
                    onConfirm()
                })
        } else onConfirm()
    }

    @SuppressWarnings("WeakerAccess")
    fun createAttachmentWithPaths(vararg filePath: String, metadata: String = "", attachmentType: String? = null): MutableList<Attachment> {
        val attachments = mutableListOf<Attachment>()
        for (path in filePath) {
            if (checkIsExistAttachment(path))
                continue

            val attachment = messageToSendHelper.buildAttachment(path, metadata, attachmentType)
            if (attachment != null) {
                attachments.add(attachment)
            } else
                Toast.makeText(context, "\"${File(path).name}\" ${getString(R.string.sceyt_unsupported_file_format)}", Toast.LENGTH_SHORT).show()
        }
        return attachments
    }

    fun addAttachment(vararg filePath: String) {
        val attachments = createAttachmentWithPaths(*filePath)
        addAttachments(attachments)
    }

    @SuppressWarnings("WeakerAccess")
    fun reset(clearInput: Boolean = true) {
        if (clearInput)
            binding.messageInput.text = null
        cancelReply()
        editMessage = null
        replyMessage = null
        allAttachments.clear()
        attachmentsAdapter.clear()
        determineInputState()
        updateDraftMessage()
    }

    @Suppress("Unused")
    fun disableInputWithMessage(message: String, @DrawableRes startIcon: Int = R.drawable.sceyt_ic_warning) {
        disabledInputByGesture = true
        hideInputWithMessage(message, startIcon)
    }

    @Suppress("unused")
    fun enableInput() {
        disabledInputByGesture = false
        if (!isInputHidden)
            showInput()
    }

    fun setMentionValidator(validator: MentionValidatorWatcher.MentionValidator) {
        binding.messageInput.setMentionValidator(validator)
    }

    @Suppress("unused")
    fun enableDisableMention(enable: Boolean) {
        enableMention = enable
    }

    @SuppressWarnings("WeakerAccess")
    fun stopRecording() {
        voiceMessageRecorderView?.forceStopRecording()
    }

    fun isEmpty() = binding.messageInput.text.isNullOrBlank() && allAttachments.isEmpty()

    @SuppressWarnings("WeakerAccess")
    fun isRecording() = voiceMessageRecorderView?.isRecording == true

    fun getComposedMessage() = binding.messageInput.text

    interface MessageInputActionCallback {
        fun sendMessage(message: Message)
        fun sendMessages(message: List<Message>)
        fun sendEditMessage(message: SceytMessage)
        fun typing(typing: Boolean)
        fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>, styling: List<BodyStyleRange>?,
                               replyOrEditMessage: SceytMessage?, isReply: Boolean)

        fun mention(query: String)
        fun join()
        fun clearChat()
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

    @Suppress("unused")
    fun setEventListener(listener: InputEventsListener) {
        eventListeners.setListener(listener)
    }

    fun setCustomEventListener(listener: InputEventsListenerImpl) {
        eventListeners = listener
    }

    @Suppress("unused")
    fun setCustomSelectFileTypePopupClickListener(listener: SelectFileTypePopupClickListenersImpl) {
        selectFileTypePopupClickListeners = listener
    }

    fun setSaveUrlsPlace(savePathsTo: MutableSet<String>) {
        chooseAttachmentHelper?.setSaveUrlsPlace(savePathsTo)
    }

    fun setMentionList(data: List<SceytMember>) {
        if (data.isEmpty() && mentionUserContainer == null) return
        initMentionUsersContainer()
        mentionUserContainer?.setMentionList(data.toSet().take(30))
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
            // Remove attachments that are not in the picker result
            allAttachments.filter { item ->
                item.type.isEqualsVideoOrImage() && it.none { mediaData -> mediaData.realPath == item.filePath }
            }.forEach { attachment ->
                val item = AttachmentItem(attachment)
                attachmentsAdapter.removeItem(item)
                allAttachments.remove(attachment)
            }
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
        binding.messageInput.clearFocus()
        chooseAttachmentHelper?.openSceytGallery(getPickerListener(), *allAttachments.map { it.filePath }.toTypedArray())
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

    override fun onClearChatClick() {
        messageInputActionCallback?.clearChat()
    }

    override fun onMultiselectModeListener(isMultiselectMode: Boolean) {
        with(binding) {
            layoutInput.isInvisible = isMultiselectMode
            rvAttachments.isVisible = !isMultiselectMode && allAttachments.isNotEmpty()
            if (isMultiselectMode) {
                hideAndStopVoiceRecorder()
                cancelReply()
                btnClearChat.animateToVisible(150)
            } else {
                when {
                    replyMessage != null -> replyMessage(replyMessage!!, initWithDraft = true)
                    editMessage != null -> editMessage(editMessage!!, initWithDraft = true)
                }
                btnClearChat.animateToGone(150)
            }
        }
    }
}