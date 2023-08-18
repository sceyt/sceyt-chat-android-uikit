package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.databinding.SceytItemOutVoiceMessageBinding
import com.sceyt.sceytchatuikit.extensions.TAG_REF
import com.sceyt.sceytchatuikit.extensions.durationToMinSecShort
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.mediaPlayerPositionToSeekBarProgress
import com.sceyt.sceytchatuikit.extensions.progressToMediaPlayerPosition
import com.sceyt.sceytchatuikit.extensions.runOnMainThread
import com.sceyt.sceytchatuikit.extensions.setPlayButtonIcon
import com.sceyt.sceytchatuikit.extensions.setTextAndDrawableColor
import com.sceyt.sceytchatuikit.media.audio.AudioPlayer
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem.MessageItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.PlaybackSpeed
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.PlaybackSpeed.Companion.fromValue
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class OutVoiceMsgViewHolder(
        private val binding: SceytItemOutVoiceMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListeners.ClickListeners,
        userNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, messageListeners, userNameBuilder = userNameBuilder, needMediaDataCallback = needMediaDataCallback) {
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
                messageListeners.onMessageClick(it, messageListItem as MessageItem)
            }

            root.setOnLongClickListener {
                messageListeners.onMessageLongClick(it, messageListItem as MessageItem)
                return@setOnLongClickListener true
            }

            playBackSpeed.setOnClickListener {
                val nextPlaybackSpeed = currentPlaybackSpeed.next()
                currentPlaybackSpeed = nextPlaybackSpeed
                AudioPlayerHelper.setPlaybackSpeed(lastFilePath, nextPlaybackSpeed.value)
            }

            loadProgress.setOnClickListener {
                messageListeners.onAttachmentLoaderClick(it, FileListItem.File(fileItem.file, (messageListItem as MessageItem).message))
            }

            playPauseButton.setOnClickListener {
                onPlayPauseClick(fileItem.file)
            }
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)
        lastFilePath = fileItem.file.filePath

        with(binding) {
            val message = (item as MessageItem).message
            tvForwarded.isVisible = message.isForwarded

            if (diff.edited || diff.statusChanged)
                setMessageStatusAndDateText(message, binding.messageDate)

            if (diff.replyCountChanged)
                setReplyCount(binding.tvReplyCount, toReplyLine, item)

            if (diff.reactionsChanged)
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
            val playBackPos = AudioPlayerHelper.getCurrentPlayer()?.playbackPosition ?: 0
            binding.voiceDuration.text = playBackPos.durationToMinSecShort()
            binding.seekBar.progress = mediaPlayerPositionToSeekBarProgress(playBackPos, fileItem.duration?.times(1000L)
                    ?: 0)
            currentPlaybackSpeed = fromValue(AudioPlayerHelper.getCurrentPlayer()?.playbackSpeed)
            true
        } else {
            binding.voiceDuration.text = fileItem.duration?.durationToMinSecShort()
            binding.seekBar.progress = 0f
            currentPlaybackSpeed = PlaybackSpeed.X1
            false
        }
    }

    private fun onPlayPauseClick(attachment: SceytAttachment) {
        if (attachment.transferState != Uploaded && attachment.transferState != Downloaded)
            return
        if (AudioPlayerHelper.alreadyInitialized(lastFilePath ?: return)) {
            AudioPlayerHelper.getCurrentPlayer()?.addEventListener(playerListener, TAG_REF, lastFilePath)
            AudioPlayerHelper.toggle(lastFilePath)
        } else
            initAudioPlayer()
    }

    private fun initAudioPlayer() {
        AudioPlayerHelper.init(lastFilePath, playerListener, TAG_REF)
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
                runOnMainThread { setPlayButtonIcon(playing, binding.playPauseButton) }
            }

            override fun onStop(filePath: String) {
                if (!checkIsValid(filePath)) return
                runOnMainThread {
                    setPlayButtonIcon(false, binding.playPauseButton)
                    binding.seekBar.progress = 0f
                    binding.voiceDuration.text = fileItem.duration?.durationToMinSecShort()
                    binding.seekBar.isEnabled = false
                    binding.playBackSpeed.isEnabled = false
                }
            }

            override fun onPaused(filePath: String?) {
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

            Downloading, Uploading, Preparing -> {
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

    override val loadingProgressView: SceytCircularProgressView
        get() = binding.loadProgress

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }

    private fun SceytItemOutVoiceMessageBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
            playPauseButton.backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
            seekBar.waveProgressColor = getCompatColor(SceytKitConfig.sceytColorAccent)
            tvForwarded.setTextAndDrawableColor(SceytKitConfig.sceytColorAccent)
        }
    }
}