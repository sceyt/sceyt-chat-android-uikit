package com.sceyt.chatuikit.presentation.components.global_search.voice.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelVoiceBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.runOnMainThread
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.media.audio.AudioPlaybackState
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.media.audio.alreadyInitialized
import com.sceyt.chatuikit.media.audio.isPlaying
import com.sceyt.chatuikit.media.audio.toggle
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.channel_info.voice.ChannelInfoVoiceItemStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle

open class VoiceSearchItemViewHolder(
    private val style: ChannelInfoVoiceItemStyle,
    private val binding: SceytItemChannelVoiceBinding,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val onAttachmentClickListener: ((GlobalSearchListItem.AttachmentItem) -> Unit)?,
) : BaseFileViewHolder<GlobalSearchListItem.AttachmentItem>(binding.root, needMediaDataCallback) {

    private var lastFilePath: String? = ""

    init {
        binding.applyStyle()
        binding.root.setOnClickListener {
            onAttachmentClickListener?.invoke(fileItem)
        }
        binding.icFile.setOnClickListener {
            if (AudioPlayerHelper.alreadyInitialized(fileItem)) {
                AudioPlayerHelper.toggle(fileItem.attachment)
            } else initAudioPlayer()
        }
    }

    override fun bind(item: GlobalSearchListItem.AttachmentItem) {
        super.bind(item)
        val attachment = item.attachment
        lastFilePath = attachment.filePath

        if (AudioPlayerHelper.alreadyInitialized(fileItem))
            initAudioPlayer()

        with(binding) {
            val sender = item.result.sender
            tvUserName.text = sender?.let { style.userNameFormatter.format(context, it) } ?: ""
            tvDate.text = style.subtitleFormatter.format(context, attachment)
            setVoiceDuration()
            setPlayingState(AudioPlayerHelper.isPlaying(fileItem))
        }
    }

    private fun initAudioPlayer() {
        val path = fileItem.attachment.filePath ?: return
        AudioPlayerHelper.init(
            filePath = path,
            messageTid = fileItem.attachment.messageTid,
            events = object : OnAudioPlayer {
                override fun onProgress(
                    position: Long, duration: Long, filePath: String,
                    messageTid: MessageTid,
                ) {
                    if (!checkIsValid(filePath, messageTid)) return
                    runOnMainThread {
                        binding.tvDuration.text = position.durationToMinSecShort()
                    }
                }

                override fun onSeek(position: Long, filePath: String, messageTid: MessageTid) = Unit

                override fun onToggle(playing: Boolean, filePath: String, messageTid: MessageTid) {
                    if (!checkIsValid(filePath, messageTid)) return
                    binding.root.post { setPlayingState(playing) }
                }

                override fun onStop(
                    filePath: String,
                    messageTid: MessageTid,
                    savedState: AudioPlaybackState?,
                ) {
                    if (!checkIsValid(filePath, messageTid)) return
                    binding.root.post { setPlayingState(false) }
                }

                override fun onPaused(filePath: String, messageTid: MessageTid) {
                    if (!checkIsValid(filePath, messageTid)) return
                    binding.root.post { setPlayingState(false) }
                }

                override fun onSpeedChanged(speed: Float, filePath: String, messageTid: MessageTid) = Unit

                override fun onError(filePath: String, messageTid: MessageTid) {
                    if (!checkIsValid(filePath, messageTid)) return
                    binding.root.post { setPlayingState(false) }
                }
            },
            tag = TAG_REF
        )
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            TransferState.PendingDownload -> {
                binding.icFile.setImageResource(0)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
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
        binding.icFile.setImageDrawable(if (playing) style.pauseIcon else style.playIcon)
        if (!playing) setVoiceDuration()
    }

    private fun checkIsValid(filePath: String?, messageTid: MessageTid): Boolean {
        filePath ?: return false
        if (!viewHolderHelper.isFileItemInitialized) return false
        return fileItem.attachment.filePath == filePath && fileItem.attachment.messageTid == messageTid
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
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
        style.userNameTextStyle.apply(tvUserName)
        style.subtitleTextStyle.apply(tvDate)
        style.durationTextStyle.apply(tvDuration)
        style.mediaLoaderStyle.apply(loadProgress)
    }
}
