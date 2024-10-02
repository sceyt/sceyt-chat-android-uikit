package com.sceyt.chatuikit.presentation.components.channel.input

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytDisableMessageInputBinding
import com.sceyt.chatuikit.databinding.SceytMessageInputViewBinding
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.extensions.asFragmentActivity
import com.sceyt.chatuikit.extensions.checkIfInsideFragment
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.empty
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.chatuikit.extensions.notAutoCorrectable
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setTextAndMoveSelectionEnd
import com.sceyt.chatuikit.extensions.showSoftInput
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioRecorderHelper
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.isPeerBlocked
import com.sceyt.chatuikit.persistence.lazyVar
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.common.SceytDialog
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentsAdapter
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentsViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.input.components.MentionUsersListView
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState.Text
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState.Voice
import com.sceyt.chatuikit.presentation.components.channel.input.data.SearchResult
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.fragments.LinkPreviewFragment
import com.sceyt.chatuikit.presentation.components.channel.input.fragments.MessageActionsFragment
import com.sceyt.chatuikit.presentation.components.channel.input.helpers.MessageToSendHelper
import com.sceyt.chatuikit.presentation.components.channel.input.link.SingleLinkDetailsProvider
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.MessageInputActionCallback
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.action.InputActionsListener
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.action.InputActionsListenerImpl
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.MessageInputClickListeners
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.MessageInputClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.SelectFileTypePopupClickListeners
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.SelectFileTypePopupClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.event.InputEventsListener
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.event.InputEventsListenerImpl
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionAnnotation
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionUserHelper
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionValidatorWatcher
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper
import com.sceyt.chatuikit.presentation.components.channel.input.mention.query.InlineQuery
import com.sceyt.chatuikit.presentation.components.channel.input.mention.query.InlineQueryChangedListener
import com.sceyt.chatuikit.presentation.components.channel.messages.dialogs.ChooseFileTypeDialog
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.RecordingListener
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.VoiceRecordPlaybackView
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.VoiceRecorderView
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import com.sceyt.chatuikit.shared.helpers.picker.PickType
import com.sceyt.chatuikit.styles.input.InputCoverStyle
import com.sceyt.chatuikit.styles.input.InputSelectedMediaStyle
import com.sceyt.chatuikit.styles.input.MessageInputStyle
import com.sceyt.chatuikit.styles.input.MessageSearchControlsStyle
import com.vanniktech.ui.animateToGone
import com.vanniktech.ui.animateToVisible
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
class MessageInputView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), MessageInputClickListeners.ClickListeners,
        SelectFileTypePopupClickListeners.ClickListeners, InputEventsListener.InputEventListeners,
        InputActionsListener.InputActionListeners {

    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private var attachmentsViewHolderFactory by lazyVar { AttachmentsViewHolderFactory(context, style) }
    private var allAttachments = mutableListOf<Attachment>()
    private val binding: SceytMessageInputViewBinding
    private var style: MessageInputStyle
    private var clickListeners = MessageInputClickListenersImpl(this)
    private var eventListeners = InputEventsListenerImpl(this)
    private var actionListeners = InputActionsListenerImpl(this)
    private var selectFileTypePopupClickListeners = SelectFileTypePopupClickListenersImpl(this)
    private var filePickerHelper: FilePickerHelper? = null
    private val typingDebounceHelper by lazy { DebounceHelper(100, getScope()) }
    private var typingTimeoutJob: Job? = null
    private var inputState = Voice
    private var disabledInputByGesture: Boolean = false
    private var voiceRecorderView: VoiceRecorderView? = null
    private var mentionUsersListView: MentionUsersListView? = null
    private var inputTextWatcher: TextWatcher? = null
    private var messageInputActionCallback: MessageInputActionCallback? = null
    private val messageToSendHelper by lazy { MessageToSendHelper(context, actionListeners) }
    private val linkDetailsProvider by lazy { SingleLinkDetailsProvider(context, getScope()) }
    private val audioRecorderHelper: AudioRecorderHelper by lazy { AudioRecorderHelper(getScope(), context) }
    private var messageActionsFragment: MessageActionsFragment? = null
    private var linkPreviewFragment: LinkPreviewFragment? = null
    var enableVoiceRecord = true
        private set
    var enableSendAttachment = true
        private set
    var enableMention = true
        private set
    var isInputHidden = false
        private set
    var isInMultiSelectMode = false
        private set
    var isInSearchMode = false
        private set
    var editMessage: SceytMessage? = null
        private set
    var replyMessage: SceytMessage? = null
        private set
    var replyThreadMessageId: Long? = null
        private set
    var linkDetails: LinkPreviewDetails? = null
        private set

    init {
        binding = SceytMessageInputViewBinding.inflate(LayoutInflater.from(context), this)
        style = MessageInputStyle.Builder(context, attrs).build()

        if (!isInEditMode)
            filePickerHelper = FilePickerHelper(context.asComponentActivity())

        init()
    }

    private fun init() {
        with(binding) {
            applyStyle()
            setOnClickListeners()
            voiceRecordPlaybackView.setStyle(style.voiceRecordPlaybackViewStyle)
            initFragments()
            addInoutListeners()
            determineInputState()
            addInputTextWatcher()
            setupAttachmentsList()
            if (enableVoiceRecord) {
                // Init SceytVoiceMessageRecorderView outside of post, because it's using permission launcher
                val voiceRecorderView = VoiceRecorderView(context).also { it.setStyle(style) }
                post {
                    onStateChanged(inputState)
                    (parent as? ViewGroup)?.let { parentView ->
                        val index = parentView.indexOfChild(this@MessageInputView)
                        parentView.addView(voiceRecorderView.apply {
                            setRecordingListener()
                            this@MessageInputView.voiceRecorderView = this
                            this@MessageInputView.voiceRecorderView?.setRecorderHeight(binding.layoutInput.height)
                            isVisible = canShowRecorderView()
                        }, index + 1, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                    }
                }
            }
        }
    }

    private fun getScope(): LifecycleCoroutineScope {
        return (findViewTreeLifecycleOwner() ?: context.asComponentActivity()).lifecycleScope
    }

    private fun initFragments() = post {
        val fragmentManager = checkIfInsideFragment()?.childFragmentManager
                ?: context.asFragmentActivity().supportFragmentManager
        fragmentManager.commit {
            add(R.id.frameLayoutMessageActions, MessageActionsFragment().also {
                it.setStyle(style)
                it.setClickListener(clickListeners)
                messageActionsFragment = it
            })
            add(R.id.frameLayoutLinkPreview, LinkPreviewFragment().also {
                it.setStyle(style)
                it.setClickListener(clickListeners)
                linkPreviewFragment = it
            })
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
            actionListeners.sendTyping(false)
        }

        typingDebounceHelper.submit {
            actionListeners.sendTyping(text.isNullOrBlank().not())
            updateDraftMessage()
            tryToLoadLinkPreview(text)
        }
    }

    private fun hideAndReleaseLinkPreview() {
        linkDetails = null
        linkPreviewFragment?.hideLinkDetails {
            binding.frameLayoutLinkPreview.isVisible = false
            return@hideLinkDetails Unit
        }
    }

    private fun updateDraftMessage() {
        val replyOrEditMessage = replyMessage ?: editMessage
        val isReply = replyMessage != null
        with(binding.messageInput) {
            actionListeners.updateDraftMessage(text, mentions, styling, replyOrEditMessage, isReply)
        }
    }

    private fun tryToLoadLinkPreview(text: Editable?) {
        if (text.isNullOrBlank()) {
            hideAndReleaseLinkPreview()
            linkDetailsProvider.cancel()
        } else {
            linkDetails = null
            linkPreviewFragment?.hideLinkDetailsWithTimeout()
            linkDetailsProvider.loadLinkDetails(text.toString(), detailsCallback = {
                if (it != null) {
                    binding.frameLayoutLinkPreview.isVisible = true
                    linkPreviewFragment?.showLinkDetails(it)
                    linkDetails = it
                } else hideAndReleaseLinkPreview()
            }, imageSizeCallback = { size ->
                linkDetails = linkDetails?.copy(imageWidth = size.width, imageHeight = size.height)
            }, thumbCallback = {
                linkDetails = linkDetails?.copy(thumb = it)
            })
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

        btnJoin.setOnClickListener {
            clickListeners.onJoinClick()
        }

        btnClearChat.setOnClickListener {
            clickListeners.onClearChatClick()
        }

        layoutSearchControl.icDown.setOnClickListener {
            clickListeners.onScrollToNextMessageClick()
        }

        layoutSearchControl.icUp.setOnClickListener {
            clickListeners.onScrollToPreviousMessageClick()
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

        closeReplyOrEditView {
            messageToSendHelper.sendMessage(allAttachments, body, editMessage, replyMessage,
                replyThreadMessageId, linkDetails)
            reset(clearInput = true, closeLinkPreview = true)
        }
    }

    private fun isEditingMessage() = editMessage != null

    private fun tryToSendRecording(file: File?, amplitudes: IntArray, duration: Int) {
        file ?: run {
            finishRecording()
            return
        }
        val metadata = Gson().toJson(AudioMetadata(amplitudes, duration))
        createAttachmentWithPaths(file.path, metadata = metadata,
            attachmentType = AttachmentTypeEnum.Voice.value).getOrNull(0)?.let {
            allAttachments.add(it)
            sendMessage()
        } ?: finishRecording()
    }

    private fun finishRecording() {
        binding.layoutInput.isVisible = true
        showVoiceRecorder()
        binding.voiceRecordPlaybackView.isVisible = false
        binding.messageInput.setText(empty)
        determineInputState()
        binding.messageInput.requestFocus()
    }

    private fun canShowRecorderView() = !disabledInputByGesture && !isInputHidden && inputState == Voice

    private fun VoiceRecorderView.setRecordingListener() {
        setListener(object : RecordingListener {
            override fun onRecordingStarted() {
                val directoryToSaveRecording = context.filesDir.path + "/Audio"
                AudioPlayerHelper.pauseAll()
                audioRecorderHelper.startRecording(directoryToSaveRecording) {}
                binding.layoutInput.isInvisible = true
                voiceRecorderView?.keepScreenOn = true
            }

            override fun onRecordingCompleted(shouldShowPreview: Boolean) {
                audioRecorderHelper.stopRecording { isTooShort, file, duration, amplitudes ->
                    if (isTooShort) {
                        finishRecording()
                        return@stopRecording
                    }
                    if (shouldShowPreview) {
                        showRecordPreview(file, amplitudes, duration)
                    } else {
                        finishRecording()
                        tryToSendRecording(file, amplitudes.toIntArray(), duration)
                    }
                    voiceRecorderView?.keepScreenOn = false
                }
            }

            override fun onRecordingCanceled() {
                audioRecorderHelper.cancelRecording()
                finishRecording()
                voiceRecorderView?.keepScreenOn = false
            }
        })
    }

    private fun showRecordPreview(file: File?, amplitudes: Array<Int>, duration: Int) {
        file ?: return
        val metadata = AudioMetadata(amplitudes.toIntArray(), duration)
        binding.voiceRecordPlaybackView.init(file, metadata, object : VoiceRecordPlaybackView.VoiceRecordPlaybackListeners {
            override fun onDeleteVoiceRecord() {
                file.deleteOnExit()
                finishRecording()
            }

            override fun onSendVoiceMessage() {
                tryToSendRecording(file, amplitudes.toIntArray(), duration)
                finishRecording()
            }
        })
        voiceRecorderView?.isVisible = false
        binding.voiceRecordPlaybackView.isVisible = true
    }

    private fun handleAttachmentClick() {
        ChooseFileTypeDialog(context).setChooseListener { chooseType ->
            when (chooseType) {
                PickType.Gallery -> selectFileTypePopupClickListeners.onGalleryClick()
                PickType.Photo -> selectFileTypePopupClickListeners.onTakePhotoClick()
                PickType.Video -> selectFileTypePopupClickListeners.onTakeVideoClick()
                PickType.File -> selectFileTypePopupClickListeners.onFileClick()
            }
        }.show()
    }

    private fun SceytMessageInputViewBinding.applyStyle() {
        layoutInput.setBackgroundColor(style.backgroundColor)
        viewAttachments.setBackgroundColor(style.dividerColor)
        divider.setBackgroundColor(style.dividerColor)
        icSendMessage.setBackgroundTint(style.sendIconBackgroundColor)
        icAddAttachments.setImageDrawable(style.attachmentIcon)
        enableVoiceRecord = style.enableVoiceRecord
        enableSendAttachment = style.enableSendAttachment
        enableMention = style.enableMention
        style.inputStyle.apply(messageInput, null)
        style.joinButtonStyle.apply(btnJoin)
        style.clearChatTextStyle.apply(btnClearChat)
        applySelectedMediaStyle(style.selectedMediaStyle)
        applySearchResultStyle(style.messageSearchControlsStyle)
        layoutInputCover.applyInputCoverStyle(style.inputCoverStyle)
        icAddAttachments.isVisible = enableSendAttachment
        if (isInEditMode) {
            icSendMessage.setImageDrawable(if (enableVoiceRecord)
                style.voiceRecordIcon else style.sendMessageIcon)
        }
    }

    private fun SceytMessageInputViewBinding.applySelectedMediaStyle(style: InputSelectedMediaStyle) {
        rvAttachments.setBackgroundColor(style.backgroundColor)
    }

    private fun SceytMessageInputViewBinding.applySearchResultStyle(style: MessageSearchControlsStyle) {
        layoutSearchControl.root.setBackgroundColor(style.backgroundColor)
        layoutSearchControl.icDown.setImageDrawable(style.previousIcon)
        layoutSearchControl.icUp.setImageDrawable(style.nextIcon)
        style.resultTextStyle.apply(layoutSearchControl.tvResult)
    }

    private fun SceytDisableMessageInputBinding.applyInputCoverStyle(style: InputCoverStyle) {
        root.setBackgroundColor(style.backgroundColor)
        divider.setBackgroundColor(style.dividerColor)
        style.textStyle.apply(tvMessage)
    }

    private fun determineInputState() {
        if (!isEnabledInput() || isInMultiSelectMode || isInSearchMode || isInEditMode)
            return

        val showVoiceIcon = enableVoiceRecord && binding.messageInput.text?.trim().isNullOrEmpty() && allAttachments.isEmpty()
                && !binding.voiceRecordPlaybackView.isShowing && !isEditingMessage()
        val newState = if (showVoiceIcon) Voice else Text
        if (inputState != newState)
            onStateChanged(newState)
        inputState = newState

        binding.icSendMessage.isInvisible = showVoiceIcon
        binding.icAddAttachments.isVisible = enableSendAttachment && !isEditingMessage()
        binding.viewAttachments.isVisible = allAttachments.isNotEmpty()
        if (showVoiceIcon) {
            showVoiceRecorder()
        } else hideAndStopVoiceRecorder()
    }

    private fun isEnabledInput() = !disabledInputByGesture && !isInputHidden

    private fun addAttachments(attachments: List<Attachment>) {
        binding.viewAttachments.isVisible = true
        allAttachments.addAll(attachments)
        attachmentsAdapter.addItems(attachments.map { AttachmentItem(it) })
        determineInputState()
    }

    private fun setupAttachmentsList() {
        attachmentsAdapter = AttachmentsAdapter(
            data = allAttachments.map { AttachmentItem(it) },
            factory = attachmentsViewHolderFactory.also {
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
        binding.layoutInputCover.root.isVisible = disabledInputByGesture && !show
        voiceRecorderView?.isVisible = canShowRecorderView()
    }

    private fun checkIsExistAttachment(path: String?): Boolean {
        return allAttachments.map { it.filePath }.contains(path)
    }

    private fun closeReplyOrEditView(readyCb: (() -> Unit?)? = null) {
        if (replyMessage == null && editMessage == null)
            readyCb?.invoke()
        else messageActionsFragment?.close {
            binding.frameLayoutMessageActions.isVisible = false
            readyCb?.invoke()
        }
    }

    private fun closeLinkDetailsView(readyCb: (() -> Unit?)? = null) {
        if (linkDetails == null)
            readyCb?.invoke()
        else linkPreviewFragment?.hideLinkDetails {
            binding.frameLayoutLinkPreview.isVisible = false
            readyCb?.invoke()
        }
    }

    private fun hideInputWithMessage(message: String, @DrawableRes startIcon: Int) {
        with(binding) {
            layoutInputCover.apply {
                tvMessage.text = message
                icStateIcon.setImageResource(startIcon)
                icStateIcon.isVisible = startIcon != 0
                root.isVisible = true
            }
            layoutInput.isInvisible = true
            messageInput.setText(empty)
            btnJoin.isVisible = false
            viewAttachments.isVisible = false
            hideAndStopVoiceRecorder()
        }
    }

    private fun showInput() {
        if (isRecording())
            binding.layoutInput.isInvisible = true
        else
            binding.layoutInput.isVisible = true

        binding.layoutInputCover.root.isVisible = false
        determineInputState()
    }

    private fun showVoiceRecorder() {
        if (canShowRecorderView())
            voiceRecorderView?.isVisible = true
    }

    private fun hideAndStopVoiceRecorder() {
        voiceRecorderView?.isVisible = false
        voiceRecorderView?.forceStopRecording()
    }

    private fun initMentionUsersContainer() {
        if (mentionUsersListView == null)
            (parent as? ViewGroup)?.addView(MentionUsersListView(context).apply {
                setStyle(style.mentionUsersListStyle)
                mentionUsersListView = initWithMessageInputView(this@MessageInputView).also {
                    setUserClickListener {
                        clickListeners.onSelectedUserToMentionClick(it)
                    }
                }
            })
    }

    private fun onStateChanged(newState: InputState) {
        eventListeners.onInputStateChanged(binding.icSendMessage, newState)
    }

    private fun initInputWithEditMessage(message: SceytMessage) {
        with(binding) {
            var body = MessageBodyStyleHelper.buildOnlyTextStyles(message.body, message.bodyAttributes)
            if (!message.mentionedUsers.isNullOrEmpty()) {
                val data = MentionUserHelper.getMentionsIndexed(context, message.bodyAttributes, message.mentionedUsers)
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
            editMessage = message
            determineInputState()
            if (!initWithDraft)
                initInputWithEditMessage(message)
            binding.frameLayoutMessageActions.isVisible = true
            messageActionsFragment?.editMessage(message)
            if (!initWithDraft)
                updateDraftMessage()
        }
    }

    internal fun replyMessage(message: SceytMessage, initWithDraft: Boolean) {
        checkIfRecordingAndConfirm {
            editMessage = null
            replyMessage = message
            binding.frameLayoutMessageActions.isVisible = true
            messageActionsFragment?.replyMessage(message)

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
            body = MessageBodyStyleHelper.buildWithAttributes(
                context = context,
                body = body.toString(),
                mentionUsers = draftMessage.mentionUsers,
                bodyAttributes = draftMessage.bodyAttributes,
                mentionTextStyle = style.mentionTextStyle,
                mentionUserNameFormatter = style.mentionUserNameFormatter
            )
            if (!draftMessage.mentionUsers.isNullOrEmpty()) {
                val data = MentionUserHelper.getMentionsIndexed(context, draftMessage.bodyAttributes,
                    draftMessage.mentionUsers)
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
            ChannelTypeEnum.Public -> {
                if (channel.userRole.isNullOrBlank()) {
                    showHideJoinButton(true)
                } else showHideJoinButton(false)
            }

            ChannelTypeEnum.Direct -> {
                val isBlockedPeer = channel.isPeerBlocked()
                with(binding) {
                    if (isBlockedPeer) {
                        viewAttachments.isVisible = false
                        frameLayoutMessageActions.isVisible = false
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

    internal fun onSearchMessagesResult(data: SearchResult) {
        with(binding.layoutSearchControl) {
            val hasResult = data.messages.isNotEmpty()
            tvResult.text = if (hasResult)
                "${data.currentIndex + 1} ${getString(R.string.sceyt_of)} ${data.messages.size}"
            else getString(R.string.sceyt_not_found)
            icDown.isEnabled = hasResult && data.currentIndex > 0
            icUp.isEnabled = hasResult && data.currentIndex < data.messages.lastIndex
        }
    }

    private fun setInitialStateSearchMessagesResult() {
        with(binding.layoutSearchControl) {
            tvResult.text = empty
            icDown.isEnabled = false
            icUp.isEnabled = false
        }
    }

    internal fun setInputActionsCallback(callback: MessageInputActionCallback) {
        messageInputActionCallback = callback
    }

    @SuppressWarnings("WeakerAccess")
    fun checkIfRecordingAndConfirm(onConfirm: () -> Unit) {
        if (isRecording()) {
            SceytDialog.showDialog(context, R.string.sceyt_stop_recording,
                R.string.sceyt_stop_recording_desc, R.string.sceyt_discard, positiveCb = {
                    stopRecording()
                    onConfirm()
                })
        } else onConfirm()
    }

    @SuppressWarnings("WeakerAccess")
    fun createAttachmentWithPaths(vararg filePath: String,
                                  metadata: String = "",
                                  attachmentType: String? = null): MutableList<Attachment> {
        val attachments = mutableListOf<Attachment>()
        for (path in filePath) {
            if (checkIsExistAttachment(path))
                continue

            val attachment = messageToSendHelper.buildAttachment(path, metadata = metadata,
                attachmentType = attachmentType)
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
    fun reset(clearInput: Boolean, closeLinkPreview: Boolean) {
        if (clearInput)
            binding.messageInput.text = null
        closeReplyOrEditView()
        if (closeLinkPreview)
            closeLinkDetailsView()
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
        voiceRecorderView?.forceStopRecording()
    }

    fun isEmpty() = binding.messageInput.text.isNullOrBlank() && allAttachments.isEmpty()

    @SuppressWarnings("WeakerAccess")
    fun isRecording() = voiceRecorderView?.isRecording == true

    fun getComposedMessage() = binding.messageInput.text

    fun getInputCover() = binding.layoutInputCover

    val inputEditText: EditText get() = binding.messageInput

    fun setClickListener(listener: MessageInputClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setCustomClickListener(listener: MessageInputClickListenersImpl) {
        clickListeners = listener
    }

    fun setActionListener(listener: InputActionsListener) {
        actionListeners.setListener(listener)
    }

    fun setCustomActionListener(listener: InputActionsListenerImpl) {
        actionListeners = listener
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

    fun setCustomAttachmentViewHolderFactory(factory: AttachmentsViewHolderFactory) {
        attachmentsViewHolderFactory = factory
    }

    fun setSaveUrlsPlace(savePathsTo: MutableSet<String>) {
        filePickerHelper?.setSaveUrlsPlace(savePathsTo)
    }

    fun setMentionList(data: List<SceytMember>) {
        if (data.isEmpty() && mentionUsersListView == null) return
        initMentionUsersContainer()
        mentionUsersListView?.setMentionList(data.toSet().take(30))
    }

    fun setCustomMessageActionsFragment(fragment: MessageActionsFragment, fragmentManager: FragmentManager) {
        fragment.setClickListener(clickListeners)
        fragmentManager.commit {
            replace(R.id.frameLayoutMessageActions, fragment)
        }
    }

    fun setCustomLinkPreviewFragment(fragment: LinkPreviewFragment, fragmentManager: FragmentManager) {
        fragment.setClickListener(clickListeners)
        fragmentManager.commit {
            replace(R.id.frameLayoutLinkPreview, fragment)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mentionUsersListView?.onInputSizeChanged(h)
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
        closeReplyOrEditView()
        reset(replyMessage == null, false)
    }

    override fun onCancelLinkPreviewClick(view: View) {
        closeLinkDetailsView()
        linkDetails = linkDetails?.copy(hideDetails = true)
    }

    override fun onRemoveAttachmentClick(item: AttachmentItem) {
        attachmentsAdapter.removeItem(item)
        allAttachments.remove(item.attachment)
        binding.viewAttachments.isVisible = allAttachments.isNotEmpty()
        determineInputState()
    }

    override fun onJoinClick() {
        messageInputActionCallback?.join()
    }

    private fun getPickerListener(): BottomSheetMediaPicker.PickerListener {
        return BottomSheetMediaPicker.PickerListener {
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
        BottomSheetMediaPicker.pickerListener?.let {
            BottomSheetMediaPicker.pickerListener = getPickerListener()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!hasWindowFocus) {
            binding.messageInput.clearFocus()
            hideSoftInput()
        }
    }

    // Choose file type popup listeners
    override fun onGalleryClick() {
        binding.messageInput.clearFocus()
        filePickerHelper?.openMediaPicker(
            pickerListener = getPickerListener(),
            selections = allAttachments.map { it.filePath }.toTypedArray(),
            maxSelectCount = SceytChatUIKit.config.attachmentSelectionLimit
        )
    }

    override fun onTakePhotoClick() {
        filePickerHelper?.takePicture {
            addAttachment(it)
        }
    }

    override fun onTakeVideoClick() {
        filePickerHelper?.takeVideo {
            addAttachment(it)
        }
    }

    override fun onFileClick() {
        filePickerHelper?.chooseMultipleFiles(allowMultiple = true) {
            addAttachment(*it.toTypedArray())
        }
    }

    override fun onInputStateChanged(sendImage: ImageView, state: InputState) {
        val iconResId = if (state == Voice) style.voiceRecordIcon
        else style.sendMessageIcon
        binding.icSendMessage.setImageDrawable(iconResId)
    }

    // Input actions listeners
    override fun sendMessage(message: Message, linkDetails: LinkPreviewDetails?) {
        messageInputActionCallback?.sendMessage(message, linkDetails)
    }

    override fun sendMessages(message: List<Message>, linkDetails: LinkPreviewDetails?) {
        messageInputActionCallback?.sendMessages(message, linkDetails)
    }

    override fun sendEditMessage(message: SceytMessage, linkDetails: LinkPreviewDetails?) {
        messageInputActionCallback?.sendEditMessage(message, linkDetails)
    }

    override fun sendTyping(typing: Boolean) {
        messageInputActionCallback?.sendTyping(typing)
    }

    override fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>, styling: List<BodyStyleRange>?, replyOrEditMessage: SceytMessage?, isReply: Boolean) {
        messageInputActionCallback?.updateDraftMessage(text, mentionUserIds, styling, replyOrEditMessage, isReply)
    }

    override fun onMentionUsersListener(query: String) {
        messageInputActionCallback?.mention(query)
    }

    override fun onClearChatClick() {
        messageInputActionCallback?.clearChat()
    }

    override fun onScrollToNextMessageClick() {
        messageInputActionCallback?.scrollToNext()
    }

    override fun onScrollToPreviousMessageClick() {
        messageInputActionCallback?.scrollToPrev()
    }

    override fun onSelectedUserToMentionClick(member: SceytMember) {
        val name = style.mentionUserNameFormatter.format(context, member.user).notAutoCorrectable()
        binding.messageInput.replaceTextWithMention(name, member.id)
    }

    override fun onMultiselectModeListener(isMultiselectMode: Boolean) {
        with(binding) {
            isInMultiSelectMode = isMultiselectMode
            showHideInputOnModeChange(isMultiselectMode)
            if (isMultiselectMode) {
                btnClearChat.animateToVisible(150)
            } else
                btnClearChat.animateToGone(150)
        }
    }

    override fun onSearchModeChangeListener(inSearchMode: Boolean) {
        with(binding) {
            isInSearchMode = inSearchMode
            showHideInputOnModeChange(inSearchMode)
            if (inSearchMode) {
                setInitialStateSearchMessagesResult()
                layoutSearchControl.root.animateToVisible(150)
            } else
                layoutSearchControl.root.animateToGone(150)
        }
    }

    private fun showHideInputOnModeChange(isInSelectMode: Boolean) {
        with(binding) {
            layoutInput.isInvisible = isInSelectMode
            viewAttachments.isVisible = !isInSelectMode && allAttachments.isNotEmpty()
            if (isInSelectMode) {
                hideAndStopVoiceRecorder()
                closeReplyOrEditView()
                closeLinkDetailsView()
            } else {
                replyMessage?.let { replyMessage(it, initWithDraft = true) }
                editMessage?.let { editMessage(it, initWithDraft = true) }
                linkDetails?.let {
                    binding.frameLayoutLinkPreview.isVisible = true
                    linkPreviewFragment?.showLinkDetails(it)
                }
                determineInputState()
            }
        }
    }
}