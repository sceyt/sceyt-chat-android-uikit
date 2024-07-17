package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelVoiceBinding
import com.sceyt.chatuikit.extensions.TAG_REF
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.runOnMainThread
import com.sceyt.chatuikit.media.audio.AudioPlayer
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.shared.utils.DateTimeUtil


class VoiceViewHolder(private val binding: SceytItemChannelVoiceBinding,
                      private val clickListener: AttachmentClickListenersImpl,
                      private val userNameFormatter: UserNameFormatter?,
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
            val path = fileItem.file.filePath ?: return@setOnClickListener
            if (AudioPlayerHelper.alreadyInitialized(path)) {
                AudioPlayerHelper.toggle(path)
            } else initAudioPlayer()
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        val attachment = item.file

        lastFilePath = attachment.filePath

        if (AudioPlayerHelper.alreadyInitialized(fileItem.file.filePath ?: ""))
            initAudioPlayer()

        with(binding) {
            val user = (item as ChannelFileItem.Voice).data.user
            tvFileName.text = user?.let {
                userNameFormatter?.format(it) ?: it.getPresentableName()
            } ?: ""
            tvDate.text = DateTimeUtil.getDateTimeString(attachment.createdAt, "dd.MM.yy • HH:mm")

            setVoiceDuration()
            setPlayingState(AudioPlayerHelper.isPlaying(lastFilePath ?: ""))
        }
    }

    private fun initAudioPlayer() {
        val path = fileItem.file.filePath ?: return
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
            TransferState.PendingDownload -> needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            TransferState.Downloaded -> lastFilePath = data.filePath
            else -> return
        }
    }

    private fun setPlayingState(playing: Boolean) {
        val iconRes = if (playing) R.drawable.sceyt_ic_pause else R.drawable.sceyt_ic_play
        binding.icFile.setImageResource(iconRes)

        if (!playing)
            setVoiceDuration()
    }

    private fun checkIsValid(filePath: String?): Boolean {
        filePath ?: return false
        if (!viewHolderHelper.isFileItemInitialized) return false
        return fileItem.file.filePath == filePath
    }

    private fun setVoiceDuration() {
        with(binding.tvDuration) {
            fileItem.duration?.let {
                text = DateTimeUtil.secondsToTime(it)
                isVisible = true
            } ?: run { isVisible = false }
        }
    }

    override val loadingProgressView: SceytCircularProgressView
        get() = binding.loadProgress

    private fun SceytItemChannelVoiceBinding.applyStyle() {
        val accentColor = context.getCompatColor(SceytChatUIKit.theme.accentColor)
        root.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.backgroundColorSections))
        icFile.backgroundTintList = ColorStateList.valueOf(accentColor)
        loadProgress.setIconTintColor(accentColor)
        loadProgress.setProgressColor(accentColor)
    }
}