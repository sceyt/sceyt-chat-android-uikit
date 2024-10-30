package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelVoiceBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.runOnMainThread
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.media.audio.AudioPlayer
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.channel_info.voice.ChannelInfoVoiceItemStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle


class VoiceViewHolder(
        private val binding: SceytItemChannelVoiceBinding,
        private val style: ChannelInfoVoiceItemStyle,
        private val clickListener: AttachmentClickListeners.ClickListeners,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseFileViewHolder<ChannelFileItem>(binding.root, needMediaDataCallback) {

    private var lastFilePath: String? = ""

    init {
        binding.applyStyle()
        binding.root.setOnClickListener {
            clickListener.onAttachmentClick(it, item = fileItem)
        }

        binding.loadProgress.setOnClickListener {
            clickListener.onAttachmentLoaderClick(it, fileItem)
        }

        binding.icFile.setOnClickListener {
            val path = fileItem.attachment.filePath ?: return@setOnClickListener
            if (AudioPlayerHelper.alreadyInitialized(path)) {
                AudioPlayerHelper.toggle(path)
            } else initAudioPlayer()
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        val attachment = item.attachment

        lastFilePath = attachment.filePath

        if (AudioPlayerHelper.alreadyInitialized(fileItem.attachment.filePath ?: ""))
            initAudioPlayer()

        with(binding) {
            val user = item.getItemData()?.user
            tvUserName.text = user?.let {
                style.userNameFormatter.format(context, it)
            } ?: ""
            tvDate.text = style.subtitleFormatter.format(context, attachment)

            setVoiceDuration()
            setPlayingState(AudioPlayerHelper.isPlaying(lastFilePath ?: ""))
        }
    }

    private fun initAudioPlayer() {
        val path = fileItem.attachment.filePath ?: return
        AudioPlayerHelper.init(path, object : OnAudioPlayer {
            override fun onInitialized(alreadyInitialized: Boolean, player: AudioPlayer, filePath: String) {
                if (!checkIsValid(filePath)) return

                if (!alreadyInitialized)
                    AudioPlayerHelper.toggle(path)
            }

            override fun onProgress(position: Long, duration: Long, filePath: String) {
                if (!checkIsValid(filePath)) return
                runOnMainThread {
                    binding.tvDuration.text = position.durationToMinSecShort()
                }
            }

            override fun onSeek(position: Long, filePath: String) {
            }

            override fun onToggle(playing: Boolean, filePath: String) {
                if (!checkIsValid(filePath)) return
                binding.root.post { setPlayingState(playing) }
            }

            override fun onStop(filePath: String) {
                if (!checkIsValid(filePath)) return
                binding.root.post {
                    setPlayingState(false)
                }
            }

            override fun onPaused(filePath: String) {
                if (!checkIsValid(filePath)) return
                binding.root.post {
                    setPlayingState(false)
                }
            }

            override fun onSpeedChanged(speed: Float, filePath: String) {
            }

            override fun onError(filePath: String) {
                if (!checkIsValid(filePath)) return
                binding.root.post {
                    setPlayingState(false)
                }
            }
        }, TAG_REF)
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)

        when (data.state) {
            TransferState.PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
                binding.icFile.setImageResource(0)
            }

            TransferState.Downloaded, TransferState.Uploaded -> {
                lastFilePath = data.filePath
                setPlayingState(false)
            }

            else -> binding.icFile.setImageResource(0)
        }
    }

    private fun setPlayingState(playing: Boolean) {
        if (lastFilePath.isNullOrBlank()) return
        val iconRes = if (playing) style.pauseIcon else style.playIcon
        binding.icFile.setImageDrawable(iconRes)

        if (!playing)
            setVoiceDuration()
    }

    private fun checkIsValid(filePath: String?): Boolean {
        filePath ?: return false
        if (!viewHolderHelper.isFileItemInitialized) return false
        return fileItem.attachment.filePath == filePath
    }

    private fun setVoiceDuration() {
        with(binding.tvDuration) {
            fileItem.duration?.let {
                text = style.durationFormatter.format(context, it)
                isVisible = true
            } ?: run { isVisible = false }
        }
    }

    override val loadingProgressViewWithStyle: Pair<CircularProgressView, MediaLoaderStyle>
        get() = binding.loadProgress to style.mediaLoaderStyle

    private fun SceytItemChannelVoiceBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
        style.subtitleTextStyle.apply(tvDate)
        style.userNameTextStyle.apply(tvUserName)
        style.durationTextStyle.apply(tvDuration)
        style.mediaLoaderStyle.apply(loadProgress)
    }
}