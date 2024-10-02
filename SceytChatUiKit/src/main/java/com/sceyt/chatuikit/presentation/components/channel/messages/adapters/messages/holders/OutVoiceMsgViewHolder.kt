package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemOutVoiceMessageBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.chatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.chatuikit.extensions.runOnMainThread
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setPlayButtonIcon
import com.sceyt.chatuikit.media.audio.AudioPlayer
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PlaybackSpeed
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PlaybackSpeed.Companion.fromValue
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class OutVoiceMsgViewHolder(
        private val binding: SceytItemOutVoiceMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
        private val voicePlayPauseListener: ((FileListItem, SceytMessage, playing: Boolean) -> Unit)?,
) : BaseMediaMessageViewHolder(binding.root, style, messageListeners,
    needMediaDataCallback = needMediaDataCallback) {
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
                messageListeners?.onMessageClick(it, messageListItem as MessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageItem)
                return@setOnLongClickListener true
            }

            playBackSpeed.setOnClickListener {
                val nextPlaybackSpeed = currentPlaybackSpeed.next()
                currentPlaybackSpeed = nextPlaybackSpeed
                AudioPlayerHelper.setPlaybackSpeed(lastFilePath, nextPlaybackSpeed.value)
            }

            loadProgress.setOnClickListener {
                messageListeners?.onAttachmentLoaderClick(it, fileItem, requireMessage)
            }

            playPauseButton.setOnClickListener {
                onPlayPauseClick(fileItem.file)
            }
        }
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        lastFilePath = fileItem.file.filePath

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

            if (diff.reactionsChanged || diff.edited)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (diff.filesChanged)
                initAttachment()

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, viewReply, false)

            initVoiceMessage()
        }
    }

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, false)

    private fun SceytItemOutVoiceMessageBinding.initVoiceMessage() {
        val metaDuration: Long = fileItem.duration?.times(1000L) //convert to milliseconds
                ?: 0
        fileItem.audioMetadata?.tmb?.let { binding.seekBar.setSampleFrom(it) }

        seekBar.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(waveformSeekBar: WaveformSeekBar, progress: Float, fromUser: Boolean) {
                if (fromUser) {
                    val seekPosition = progressToMediaPlayerPosition(progress, metaDuration)
                    AudioPlayerHelper.seek(fileItem.file.filePath, seekPosition)
                }
            }
        }

        val isPlaying = checkIsPlayingAndSetState()
        seekBar.isEnabled = isPlaying
        playBackSpeed.isEnabled = isPlaying

        if (AudioPlayerHelper.alreadyInitialized(fileItem.file.filePath ?: ""))
            initAudioPlayer()
    }

    private fun checkIsPlayingAndSetState(): Boolean {
        return if (AudioPlayerHelper.getCurrentPlayingAudioPath() == fileItem.file.filePath) {
            val playBackPos = AudioPlayerHelper.getCurrentPlayer()?.getPlaybackPosition() ?: 0
            binding.voiceDuration.text = playBackPos.durationToMinSecShort()
            binding.seekBar.progress = mediaPlayerPositionToSeekBarProgress(playBackPos, fileItem.duration?.times(1000L)
                    ?: 0)
            currentPlaybackSpeed = fromValue(AudioPlayerHelper.getCurrentPlayer()?.getPlaybackSpeed())
            true
        } else {
            binding.voiceDuration.text = fileItem.duration?.times(1000L) // convert to milliseconds
                ?.durationToMinSecShort()
            binding.seekBar.progress = 0f
            currentPlaybackSpeed = PlaybackSpeed.X1
            false
        }
    }

    private fun onPlayPauseClick(attachment: SceytAttachment) {
        if (attachment.transferState != Uploaded && attachment.transferState != Downloaded)
            return

        val path = attachment.filePath ?: return
        if (AudioPlayerHelper.alreadyInitialized(path)) {
            AudioPlayerHelper.getCurrentPlayer()?.addEventListener(playerListener, TAG_REF, path)
            AudioPlayerHelper.toggle(path)
        } else
            initAudioPlayer()
    }

    private fun initAudioPlayer() {
        AudioPlayerHelper.init(lastFilePath ?: return, playerListener, TAG_REF)
    }

    private val playerListener: OnAudioPlayer by lazy {
        object : OnAudioPlayer {
            override fun onInitialized(alreadyInitialized: Boolean, player: AudioPlayer, filePath: String) {
                if (!checkIsValid(filePath)) return

                if (!alreadyInitialized)
                    player.togglePlayPause()

                runOnMainThread {
                    binding.seekBar.isEnabled = true
                    binding.playBackSpeed.isEnabled = true
                }
            }

            override fun onProgress(position: Long, duration: Long, filePath: String) {
                if (!checkIsValid(filePath)) return
                val seekBarProgress = mediaPlayerPositionToSeekBarProgress(position, duration)
                runOnMainThread {
                    binding.seekBar.progress = seekBarProgress
                    binding.seekBar.isEnabled = true
                    binding.playBackSpeed.isEnabled = true
                    binding.voiceDuration.text = position.durationToMinSecShort()
                }
            }

            override fun onToggle(playing: Boolean, filePath: String) {
                if (!checkIsValid(filePath)) return
                runOnMainThread {
                    setPlayButtonIcon(playing, binding.playPauseButton)
                    voicePlayPauseListener?.invoke(fileItem, requireMessage, playing)
                }
            }

            override fun onStop(filePath: String) {
                if (!checkIsValid(filePath)) return
                runOnMainThread {
                    setPlayButtonIcon(false, binding.playPauseButton)
                    binding.seekBar.progress = 0f
                    binding.voiceDuration.text = fileItem.duration?.times(1000L) // convert to milliseconds
                        ?.durationToMinSecShort()
                    binding.seekBar.isEnabled = false
                    binding.playBackSpeed.isEnabled = false
                }
            }

            override fun onPaused(filePath: String) {
                if (!checkIsValid(filePath)) return
                runOnMainThread { setPlayButtonIcon(false, binding.playPauseButton) }
            }

            override fun onSpeedChanged(speed: Float, filePath: String) {
                if (!checkIsValid(filePath)) return
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
                binding.playPauseButton.setImageResource(getPlayPauseItemResId())
            }

            PendingUpload, PauseUpload -> {
                binding.playPauseButton.setImageResource(0)
            }

            PendingDownload -> {
                binding.playPauseButton.setImageResource(0)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
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

    private fun getPlayPauseItemResId(): Int {
        val isPlaying = AudioPlayerHelper.isPlaying(fileItem.file.filePath ?: "")
        return if (isPlaying) R.drawable.sceyt_ic_pause else R.drawable.sceyt_ic_play
    }

    private fun checkIsValid(filePath: String?): Boolean {
        filePath ?: return false
        if (!viewHolderHelper.isFileItemInitialized) return false
        return fileItem.file.filePath == filePath
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
        applyCommonStyle(
            layoutDetails = layoutDetails,
            tvForwarded = tvForwarded,
            messageBody = messageBody,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine
        )
    }
}