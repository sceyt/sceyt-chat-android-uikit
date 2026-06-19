package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemIncVoiceMessageBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerState
import com.sceyt.chatuikit.media.audio.AudioPlayerStateCollector
import com.sceyt.chatuikit.media.audio.AudioPlayerStatus
import com.sceyt.chatuikit.media.audio.VoiceStateCoordinator
import com.sceyt.chatuikit.media.audio.alreadyInitialized
import com.sceyt.chatuikit.media.audio.isAudioPlaybackAvailable
import com.sceyt.chatuikit.media.audio.isPlaying
import com.sceyt.chatuikit.media.audio.seek
import com.sceyt.chatuikit.media.audio.toggle
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PlaybackSpeed
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class IncVoiceMessageViewHolder(
    private val binding: SceytItemIncVoiceMessageBinding,
    private val viewPoolReactions: RecyclerView.RecycledViewPool,
    private val style: MessageItemStyle,
    private val isViewOnce: Boolean,
    private val messageListeners: MessageClickListeners.ClickListeners,
    displayedListener: ((MessageListItem) -> Unit)?,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val voicePlayPauseListener: ((FileListItem, SceytMessage, playing: Boolean) -> Unit)?,
) : BaseMediaMessageViewHolder(
    view = binding.root,
    style = style,
    messageListeners = messageListeners,
    displayedListener = displayedListener,
    needMediaDataCallback = needMediaDataCallback
) {
    private val playbackStateCollector = AudioPlayerStateCollector(
        onStateChanged = ::onPlaybackStateChanged
    )
    private var lastReportedPlaying = false
    private var currentPlaybackSpeed: PlaybackSpeed = PlaybackSpeed.X1
        set(value) {
            field = value
            binding.playBackSpeed.text = value.displayValue
        }
    private var lastFilePath: String? = ""

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                if (isViewOnce) {
                    messageListeners.onAttachmentClick(it, fileItem, requireMessage)
                } else {
                    messageListeners.onMessageClick(it, requireMessageItem)
                }
            }

            root.setOnLongClickListener {
                messageListeners.onMessageLongClick(it, requireMessageItem)
                return@setOnLongClickListener true
            }

            playBackSpeed.setOnClickListener {
                val nextPlaybackSpeed = currentPlaybackSpeed.next()
                currentPlaybackSpeed = nextPlaybackSpeed
                AudioPlayerHelper.setPlaybackSpeed(
                    filePath = lastFilePath,
                    messageTid = fileItem.messageTid,
                    speed = nextPlaybackSpeed.value
                )
            }

            loadProgress.setOnClickListener {
                messageListeners.onAttachmentLoaderClick(it, fileItem, requireMessage)
            }

            playPauseButton.setOnClickListener {
                onPlayPauseClick(fileItem.attachment)
            }
        }
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        lastFilePath = fileItem.attachment.filePath

        with(binding) {
            val message = (item as MessageListItem.MessageItem).message
            tvForwarded.isVisible = message.isForwarded

            val body = message.body.trim()
            if (body.isNotBlank()) {
                messageBody.isVisible = true
                setMessageBody(messageBody, message)
            } else messageBody.isVisible = false

            if (diff.edited || diff.statusChanged)
                setMessageStatusAndDateText(message, messageDate)

            if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                setMessageUserAvatarAndName(avatar, tvUserName, message)

            if (diff.replyCountChanged)
                setReplyCount(tvReplyCount, toReplyLine, item)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, viewReply, false)

            if (diff.reactionsChanged || diff.edited)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (diff.filesChanged)
                initAttachment()

            if (item.message.shouldShowAvatarAndName)
                avatar.setOnClickListener {
                    messageListeners.onAvatarClick(it, item)
                }

            initVoiceMessage()
            updateViewOnceUI()
        }
    }

    private fun updateViewOnceUI() {
        with(binding) {
            ivViewOnceIcon.isVisible = isViewOnce
            playPauseButton.isClickable = !isViewOnce
            playBackSpeed.isClickable = !isViewOnce
            seekBar.isEnabled = !isViewOnce
        }
    }

    override val layoutBubbleConfig get() = Pair(binding.root, false)

    private fun SceytItemIncVoiceMessageBinding.initVoiceMessage() {
        val metaDuration: Long = fileItem.duration?.times(1000L) ?: 0
        fileItem.audioMetadata?.tmb?.let { binding.seekBar.setSampleFrom(it) }

        seekBar.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(
                waveformSeekBar: WaveformSeekBar,
                progress: Float,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val seekPosition = progressToMediaPlayerPosition(progress, metaDuration)
                    AudioPlayerHelper.seek(fileItem, seekPosition)
                }
            }
        }

        lastReportedPlaying = false
        onPlaybackStateChanged(AudioPlayerHelper.state.value)
    }

    private fun onPlayPauseClick(attachment: SceytAttachment) {
        if (attachment.transferState != Uploaded && attachment.transferState != Downloaded)
            return

        // Stop any active recording before starting playback
        VoiceStateCoordinator.stopRecordingIfActive()

        if (AudioPlayerHelper.alreadyInitialized(attachment)) {
            AudioPlayerHelper.toggle(attachment)
        } else
            initAudioPlayer()
    }

    private fun initAudioPlayer() {
        AudioPlayerHelper.init(
            filePath = lastFilePath ?: return,
            messageTid = fileItem.attachment.messageTid
        )
    }

    private fun onPlaybackStateChanged(state: AudioPlayerState) {
        if (!viewHolderHelper.isFileItemInitialized) return
        val playbackAvailable = fileItem.isAudioPlaybackAvailable()
        val isCurrent = state.matches(lastFilePath, fileItem.attachment.messageTid)
        val playing = playbackAvailable && isCurrent && state.isPlaying
        setPlayButtonIcon(playing)
        if (lastReportedPlaying != playing) {
            lastReportedPlaying = playing
            voicePlayPauseListener?.invoke(fileItem, requireMessage, playing)
        }

        when {
            !playbackAvailable -> {
                showSavedPlaybackState()
            }

            isCurrent && state.status != AudioPlayerStatus.Stopped -> {
                val duration = state.duration.takeIf { it > 0 }
                    ?: fileItem.duration?.times(1000L)
                    ?: 0
                binding.seekBar.progress = mediaPlayerPositionToSeekBarProgress(
                    currentPosition = state.position,
                    mediaDuration = duration
                )
                binding.voiceDuration.text = style.voiceDurationFormatter.format(
                    context = context,
                    from = if (state.status == AudioPlayerStatus.Completed) duration else state.position
                )
                currentPlaybackSpeed = PlaybackSpeed.fromValue(state.speed)
                val controlsEnabled = state.status == AudioPlayerStatus.Playing ||
                        state.status == AudioPlayerStatus.Paused ||
                        state.status == AudioPlayerStatus.Initializing
                binding.seekBar.isEnabled = controlsEnabled
                binding.playBackSpeed.isEnabled = controlsEnabled
            }

            else -> {
                showSavedPlaybackState()
            }
        }
    }

    private fun showSavedPlaybackState() {
        val duration = fileItem.duration?.times(1000L) ?: 0
        val savedState = lastFilePath?.let {
            AudioPlayerHelper.getPlaybackState(it, fileItem.attachment.messageTid)
        }
        val position = savedState?.position ?: 0
        binding.voiceDuration.text = style.voiceDurationFormatter.format(
            context = context,
            from = if (position > 0) position else duration
        )
        binding.seekBar.progress = mediaPlayerPositionToSeekBarProgress(
            currentPosition = position,
            mediaDuration = duration
        )
        currentPlaybackSpeed = PlaybackSpeed.fromValue(savedState?.speed)
        binding.seekBar.isEnabled = false
        binding.playBackSpeed.isEnabled = false
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            Uploaded, Downloaded -> {
                lastFilePath = data.filePath
                setPlayButtonIcon(AudioPlayerHelper.isPlaying(fileItem))
            }

            PendingUpload, PauseUpload -> {
                binding.playPauseButton.setImageResource(0)
            }

            PendingDownload -> {
                binding.playPauseButton.setImageResource(0)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
            }

            Downloading, Uploading, Preparing, WaitingToUpload -> {
                binding.playPauseButton.setImageResource(0)
            }

            ErrorUpload, ErrorDownload, PauseDownload -> {
                binding.playPauseButton.setImageResource(0)
            }

            FilePathChanged, ThumbLoaded -> return
        }
    }

    private fun getPlayPauseItem(
        isPlaying: Boolean = AudioPlayerHelper.isPlaying(fileItem),
    ): Drawable? {
        return if (isPlaying) style.voicePauseIcon else style.voicePlayIcon
    }

    private fun setPlayButtonIcon(playing: Boolean) {
        if (fileItem.isAudioPlaybackAvailable()) {
            binding.playPauseButton.setImageDrawable(getPlayPauseItem(playing))
        } else {
            binding.playPauseButton.setImageResource(0)
        }
    }

    override val loadingProgressView: CircularProgressView
        get() = binding.loadProgress

    override val selectMessageView: View
        get() = binding.selectView

    override val incoming: Boolean
        get() = true

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }

    private fun SceytItemIncVoiceMessageBinding.setMessageItemStyle() {
        val accentColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        playPauseButton.setBackgroundTint(accentColor)
        seekBar.waveProgressColor = style.voiceWaveformStyle.progressColor
        seekBar.waveBackgroundColor = style.voiceWaveformStyle.trackColor
        style.voiceSpeedTextStyle.apply(playBackSpeed)
        style.voiceDurationTextStyle.apply(voiceDuration)
        style.mediaLoaderStyle.apply(loadProgress)
        style.viewOnceBadgeStyle.apply(ivViewOnceIcon)

        applyCommonStyle(
            layoutDetails = layoutDetails,
            tvForwarded = tvForwarded,
            messageBody = messageBody,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine,
            tvSenderName = tvUserName,
            avatarView = avatar
        )
    }

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        playbackStateCollector.start(itemView)
    }

    override fun onViewDetachedFromWindow() {
        playbackStateCollector.stop()
        super.onViewDetachedFromWindow()
    }
}
