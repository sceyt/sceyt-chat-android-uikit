package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelVoiceBinding
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerState
import com.sceyt.chatuikit.media.audio.AudioPlayerStateCollector
import com.sceyt.chatuikit.media.audio.alreadyInitialized
import com.sceyt.chatuikit.media.audio.isAudioPlaybackAvailable
import com.sceyt.chatuikit.media.audio.toggle
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

    private val playbackStateCollector = AudioPlayerStateCollector(
        onStateChanged = ::onPlaybackStateChanged
    )
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
            if (!fileItem.isAudioPlaybackAvailable()) return@setOnClickListener
            if (AudioPlayerHelper.alreadyInitialized(fileItem)) {
                AudioPlayerHelper.toggle(fileItem.attachment)
            } else initAudioPlayer()
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        val attachment = item.attachment

        lastFilePath = attachment.filePath

        with(binding) {
            val user = item.getItemData()?.user
            tvUserName.text = user?.let {
                style.userNameFormatter.format(context, it)
            } ?: ""
            tvDate.text = style.subtitleFormatter.format(context, attachment)

            setVoiceDuration()
            onPlaybackStateChanged(AudioPlayerHelper.state.value)
        }
    }

    private fun initAudioPlayer() {
        val path = fileItem.attachment.filePath ?: return
        AudioPlayerHelper.init(
            filePath = path,
            messageTid = fileItem.attachment.messageTid
        )
    }

    private fun onPlaybackStateChanged(state: AudioPlayerState) {
        if (!viewHolderHelper.isFileItemInitialized) return
        if (!fileItem.isAudioPlaybackAvailable()) {
            binding.icFile.setImageResource(0)
            return
        }
        val isCurrent = state.matches(lastFilePath, fileItem.attachment.messageTid)
        setPlayingState(isCurrent && state.isPlaying)
        if (isCurrent && state.position > 0) {
            binding.tvDuration.text = state.position.durationToMinSecShort()
        }
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
        if (!fileItem.isAudioPlaybackAvailable()) {
            binding.icFile.setImageResource(0)
            return
        }
        val iconRes = if (playing) style.pauseIcon else style.playIcon
        binding.icFile.setImageDrawable(iconRes)

        if (!playing)
            setVoiceDuration()
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

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        playbackStateCollector.start(itemView)
    }

    override fun onViewDetachedFromWindow() {
        playbackStateCollector.stop()
        super.onViewDetachedFromWindow()
    }

    private fun SceytItemChannelVoiceBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
        style.subtitleTextStyle.apply(tvDate)
        style.userNameTextStyle.apply(tvUserName)
        style.durationTextStyle.apply(tvDuration)
        style.mediaLoaderStyle.apply(loadProgress)
    }
}
