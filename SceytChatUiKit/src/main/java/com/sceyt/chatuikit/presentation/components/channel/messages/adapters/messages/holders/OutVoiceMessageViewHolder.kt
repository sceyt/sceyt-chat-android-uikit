package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.graphics.drawable.Drawable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemOutVoiceMessageBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.runOnMainThread
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.media.audio.AudioPlaybackState
import com.sceyt.chatuikit.media.audio.AudioPlayer
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.media.audio.VoiceStateCoordinator
import com.sceyt.chatuikit.media.audio.alreadyInitialized
import com.sceyt.chatuikit.media.audio.isCurrentPlayer
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
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PlaybackSpeed
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PlaybackSpeed.Companion.fromValue
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class OutVoiceMessageViewHolder(
    private val binding: SceytItemOutVoiceMessageBinding,
    private val viewPoolReactions: RecyclerView.RecycledViewPool,
    private val style: MessageItemStyle,
    private val isViewOnce: Boolean,
    private val messageListeners: MessageClickListeners.ClickListeners?,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val voicePlayPauseListener: ((FileListItem, SceytMessage, playing: Boolean) -> Unit)?,
) : BaseMediaMessageViewHolder(
    binding.root, style, messageListeners,
    needMediaDataCallback = needMediaDataCallback
) {
    private var lastFilePath: String? = ""
    private var currentPlaybackSpeed: PlaybackSpeed = PlaybackSpeed.X1
        set(value) {
            field = value
            binding.playBackSpeed.text = value.displayValue
        }

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                if (isViewOnce) {
                    messageListeners?.onAttachmentClick(it, fileItem, requireMessage)
                } else {
                    messageListeners?.onMessageClick(it, messageListItem as MessageItem)
                }
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageItem)
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
                messageListeners?.onAttachmentLoaderClick(it, fileItem, requireMessage)
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
            val message = (item as MessageItem).message
            tvForwarded.isVisible = message.isForwarded

            val body = message.body.trim()
            if (body.isNotBlank()) {
                messageBody.isVisible = true
                setMessageBody(messageBody, message)
            } else messageBody.isVisible = false

            if (diff.edited || diff.statusChanged)
                setMessageStatusAndDateText(message, binding.messageDate)

            if (diff.replyCountChanged)
                setReplyCount(binding.tvReplyCount, toReplyLine, item)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, viewReply, false)

            if (diff.reactionsChanged || diff.edited)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (diff.filesChanged)
                initAttachment()

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

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, false)

    private fun SceytItemOutVoiceMessageBinding.initVoiceMessage() {
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

        val isPlaying = checkIsPlayingAndSetState()
        seekBar.isEnabled = isPlaying
        playBackSpeed.isEnabled = isPlaying

        AudioPlayerHelper.addEventListener(playerListener, TAG_REF)
    }

    private fun checkIsPlayingAndSetState(): Boolean {
        return if (AudioPlayerHelper.isCurrentPlayer(fileItem)) {
            val playBackPos = AudioPlayerHelper.getCurrentPlayer()?.getPlaybackPosition() ?: 0
            binding.voiceDuration.text = style.voiceDurationFormatter.format(context, playBackPos)
            binding.seekBar.progress = mediaPlayerPositionToSeekBarProgress(
                playBackPos, fileItem.duration?.times(1000L) ?: 0
            )
            currentPlaybackSpeed =
                fromValue(AudioPlayerHelper.getCurrentPlayer()?.getPlaybackSpeed())
            true
        } else {
            // Check if there's a saved state for this voice message
            val savedState = AudioPlayerHelper.getPlaybackState(
                fileItem.attachment.filePath ?: "",
                fileItem.attachment.messageTid
            )
            if (savedState != null) {
                // Restore the saved position and speed in UI
                binding.voiceDuration.text =
                    style.voiceDurationFormatter.format(context, savedState.position)
                binding.seekBar.progress = mediaPlayerPositionToSeekBarProgress(
                    savedState.position, fileItem.duration?.times(1000L) ?: 0
                )
                currentPlaybackSpeed = fromValue(savedState.speed)
            } else {
                // No saved state, show default values
                binding.voiceDuration.text = style.voiceDurationFormatter.format(
                    context = context,
                    from = fileItem.duration?.times(1000L) ?: 0
                )
                binding.seekBar.progress = 0f
                currentPlaybackSpeed = PlaybackSpeed.X1
            }
            false
        }
    }

    private fun onPlayPauseClick(attachment: SceytAttachment) {
        if (attachment.transferState != Uploaded && attachment.transferState != Downloaded)
            return

        // Stop any active recording before starting playback
        VoiceStateCoordinator.stopRecordingIfActive()

        if (AudioPlayerHelper.alreadyInitialized(attachment)) {
            AudioPlayerHelper.getCurrentPlayer()?.addEventListener(
                event = playerListener,
                tag = TAG_REF,
            )
            AudioPlayerHelper.toggle(attachment)
        } else
            initAudioPlayer()
    }

    private fun initAudioPlayer() {
        AudioPlayerHelper.init(
            filePath = lastFilePath ?: return,
            messageTid = fileItem.attachment.messageTid,
            events = playerListener,
            tag = TAG_REF
        )
    }

    private val playerListener: OnAudioPlayer by lazy {
        object : OnAudioPlayer {
            override fun onInitialized(
                alreadyInitialized: Boolean,
                player: AudioPlayer,
                filePath: String,
                messageTid: MessageTid
            ) {
                if (!checkIsValid(filePath, messageTid)) return

                runOnMainThread {
                    binding.seekBar.isEnabled = true
                    binding.playBackSpeed.isEnabled = true
                }
            }

            override fun onProgress(
                position: Long, duration: Long, filePath: String,
                messageTid: MessageTid
            ) {
                if (!checkIsValid(filePath, messageTid)) return
                val seekBarProgress = mediaPlayerPositionToSeekBarProgress(position, duration)
                runOnMainThread {
                    binding.seekBar.progress = seekBarProgress
                    binding.seekBar.isEnabled = true
                    binding.playBackSpeed.isEnabled = true
                    binding.voiceDuration.text =
                        style.voiceDurationFormatter.format(context, position)
                }
            }

            override fun onToggle(
                playing: Boolean, filePath: String,
                messageTid: MessageTid
            ) {
                if (!checkIsValid(filePath, messageTid)) return
                runOnMainThread {
                    setPlayButtonIcon(playing)
                    voicePlayPauseListener?.invoke(fileItem, requireMessage, playing)
                }
            }

            override fun onStop(
                filePath: String,
                messageTid: MessageTid,
                savedState: AudioPlaybackState?
            ) {
                if (!checkIsValid(filePath, messageTid)) return
                runOnMainThread {
                    setPlayButtonIcon(false)
                    if (savedState != null) {
                        // Switched to another voice message - show saved position and speed
                        binding.voiceDuration.text =
                            style.voiceDurationFormatter.format(context, savedState.position)
                        binding.seekBar.progress = mediaPlayerPositionToSeekBarProgress(
                            savedState.position, fileItem.duration?.times(1000L) ?: 0
                        )
                        currentPlaybackSpeed = fromValue(savedState.speed)
                    } else {
                        // Natural completion - reset position to beginning but keep speed
                        binding.seekBar.progress = 0f
                        binding.voiceDuration.text = style.voiceDurationFormatter.format(
                            context,
                            fileItem.duration?.times(1000L) ?: 0
                        )
                    }
                    binding.seekBar.isEnabled = false
                    binding.playBackSpeed.isEnabled = false
                }
            }

            override fun onPaused(
                filePath: String,
                messageTid: MessageTid
            ) {
                if (!checkIsValid(filePath, messageTid)) return
                runOnMainThread { setPlayButtonIcon(false) }
            }

            override fun onSpeedChanged(
                speed: Float, filePath: String,
                messageTid: MessageTid
            ) {
                if (!checkIsValid(filePath, messageTid)) return
                runOnMainThread {
                    currentPlaybackSpeed = PlaybackSpeed.fromValue(speed)
                }
            }
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            Uploaded, Downloaded -> {
                lastFilePath = data.filePath
                binding.playPauseButton.setImageDrawable(getPlayPauseItem())
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
        binding.playPauseButton.setImageDrawable(getPlayPauseItem(playing))
    }

    private fun checkIsValid(filePath: String?, messageTid: MessageTid): Boolean {
        filePath ?: return false
        if (!viewHolderHelper.isFileItemInitialized) return false
        return fileItem.attachment.filePath == filePath && fileItem.attachment.messageTid == messageTid
    }

    override val loadingProgressView: CircularProgressView
        get() = binding.loadProgress

    override val selectMessageView get() = binding.selectView

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }

    override val incoming: Boolean
        get() = false

    private fun SceytItemOutVoiceMessageBinding.setMessageItemStyle() {
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
            toReplyLine = toReplyLine
        )
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        AudioPlayerHelper.removeEventListener(TAG_REF)
    }
}