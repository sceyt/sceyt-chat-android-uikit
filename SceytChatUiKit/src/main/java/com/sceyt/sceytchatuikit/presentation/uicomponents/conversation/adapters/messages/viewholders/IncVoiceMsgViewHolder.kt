package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.databinding.SceytItemIncVoiceBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.persistence.mappers.toTransferData
import com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder.AudioMetadata
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.PlaybackSpeed
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class IncVoiceMsgViewHolder(
        private val binding: SceytItemIncVoiceBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListeners.ClickListeners,
        displayedListener: ((MessageListItem) -> Unit)?,
        senderNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseMsgViewHolder(binding.root, messageListeners = messageListeners, displayedListener = displayedListener, senderNameBuilder = senderNameBuilder) {
    private var currentPlaybackSpeed: PlaybackSpeed = PlaybackSpeed.X1
        set(value) {
            field = value
            binding.playBackSpeed.text = value.displayValue
        }

    init {
        binding.setMessageItemStyle()

        binding.playBackSpeed.setOnClickListener {
            val nextPlaybackSpeed = currentPlaybackSpeed.next()
            currentPlaybackSpeed = nextPlaybackSpeed
            AudioPlayerHelper.setPlaybackSpeed(lastFilePath, nextPlaybackSpeed.value)
        }

        binding.root.setOnClickListener {
            messageListeners.onMessageClick(it, messageListItem as MessageListItem.MessageItem)
        }

        binding.root.setOnLongClickListener {
            messageListeners.onMessageLongClick(
                view = it,
                item = messageListItem as MessageListItem.MessageItem,
            )
            return@setOnLongClickListener true
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message
                tvForwarded.isVisible = message.isForwarded

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)

                if (item.message.canShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners.onAvatarClick(it, item)
                    }

                initVoiceMessage(item)
            }
        }
    }

    private fun SceytItemIncVoiceBinding.initVoiceMessage(item: MessageListItem.MessageItem) {
        val attachment: SceytAttachment = item.message.attachments?.firstOrNull() ?: return
        lastFilePath = attachment.filePath
        loadProgress.release(attachment.progressPercent)
        attachment.toTransferData()?.let { updateState(it) }
        setListener()

        if (attachment.filePath.isNullOrBlank())
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(attachment))

        val audioMetadata: AudioMetadata = attachment.getMetadataFromAttachment()
        val metaDuration: Long = audioMetadata.dur.times(1000L) //convert to milliseconds
        audioMetadata.tmb?.let { seekBar.setSampleFrom(it) }

        with(playBackSpeed) {
            text = currentPlaybackSpeed.displayValue
            isEnabled = false

            setOnClickListener {
                val nextPlaybackSpeed = currentPlaybackSpeed.next()
                currentPlaybackSpeed = nextPlaybackSpeed
                AudioPlayerHelper.setPlaybackSpeed(lastFilePath, nextPlaybackSpeed.value)
            }
        }

        seekBar.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(waveformSeekBar: WaveformSeekBar, progress: Float, fromUser: Boolean) {
                if (fromUser) {
                    val seekPosition = progressToMediaPlayerPosition(progress, metaDuration)
                    AudioPlayerHelper.seek(lastFilePath, seekPosition)
                }
            }
        }

        voiceDuration.text = metaDuration.durationToMinSecShort()
        seekBar.isEnabled = false

        binding.loadProgress.setOnClickListener {
            messageListeners.onAttachmentLoaderClick(it, FileListItem.File(attachment, item.message))
        }

        playPauseButton.setOnClickListener {
            onPlayPauseClick(attachment, audioMetadata)
        }
    }

    private fun onPlayPauseClick(attachment: SceytAttachment, audioMetadata: AudioMetadata) {
        if (attachment.transferState != Uploaded && attachment.transferState != Downloaded)
            return

        AudioPlayerHelper.init(lastFilePath, object : OnAudioPlayer {
            override fun onInitialized() {
                AudioPlayerHelper.toggle(lastFilePath)
                runOnMainThread {
                    binding.seekBar.isEnabled = true
                    binding.playBackSpeed.isEnabled = true
                }
            }

            override fun onProgress(position: Long, duration: Long) {
                val seekBarProgress = mediaPlayerPositionToSeekBarProgress(position, duration)
                runOnMainThread {
                    binding.seekBar.progress = seekBarProgress
                    binding.seekBar.isEnabled = true
                    binding.playBackSpeed.isEnabled = true
                    binding.voiceDuration.text = position.durationToMinSecShort()
                }
            }

            override fun onSeek(position: Long) {
            }

            override fun onToggle(playing: Boolean) {
                runOnMainThread {
                    setPlayButtonIcon(playing, binding.playPauseButton)
                }
            }

            override fun onStop() {
                runOnMainThread {
                    setPlayButtonIcon(false, binding.playPauseButton)
                    binding.seekBar.progress = 0f
                    currentPlaybackSpeed = PlaybackSpeed.X1
                    AudioPlayerHelper.setPlaybackSpeed(lastFilePath, PlaybackSpeed.X1.value)
                    binding.voiceDuration.text = audioMetadata.dur.times(1000L).durationToMinSecShort()
                    binding.seekBar.isEnabled = false
                    binding.playBackSpeed.isEnabled = false
                }
            }

            override fun onSpeedChanged(speed: Float) {
                runOnMainThread {
                    currentPlaybackSpeed = PlaybackSpeed.fromValue(speed)
                }
            }

            override fun onError() {
            }
        })
    }

    private fun updateState(data: TransferData) {
        if (isMessageListItemInitialized.not()) return
        val message = (messageListItem as? MessageListItem.MessageItem)?.message ?: return
        if ((data.messageTid != message.tid)) return

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload, PauseUpload -> {
                binding.playPauseButton.setImageResource(0)
            }
            PendingDownload -> {
                binding.playPauseButton.setImageResource(0)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(
                    (message.attachments ?: return)[0]))
            }
            Downloading, Uploading -> {
                binding.playPauseButton.setImageResource(0)
            }
            Uploaded -> {
                binding.playPauseButton.setImageResource(R.drawable.sceyt_ic_play)
            }
            Downloaded -> {
                lastFilePath = data.filePath
                binding.playPauseButton.setImageResource(R.drawable.sceyt_ic_play)
            }
            ErrorUpload, ErrorDownload, PauseDownload -> {
                binding.playPauseButton.setImageResource(0)
            }
            FilePathChanged, ThumbLoaded -> return
        }
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        AudioPlayerHelper.stop(lastFilePath)
    }

    private var lastFilePath: String? = ""

    private fun setListener() {
        MessageEventsObserver.onTransferUpdatedLiveData
            .observe(context.asComponentActivity(), ::updateState)
    }

    private fun SceytItemIncVoiceBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
            tvUserName.setTextColor(getCompatColorByTheme(MessagesStyle.senderNameTextColor))
            playPauseButton.backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
            seekBar.waveProgressColor = getCompatColor(SceytKitConfig.sceytColorAccent)
            tvForwarded.setTextAndDrawableColor(SceytKitConfig.sceytColorAccent)
        }
    }
}