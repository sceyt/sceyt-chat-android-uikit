package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.view.isVisible
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.ItemChannelVoiceBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper.OnAudioPlayer
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseChannelFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil


class VoiceViewHolder(private var binding: ItemChannelVoiceBinding,
                      private val clickListener: AttachmentClickListenersImpl,
                      private val userNameBuilder: ((User) -> String)?,
                      needMediaDataCallback: (NeedMediaInfoData) -> Unit)
    : BaseChannelFileViewHolder(binding.root, needMediaDataCallback) {

    private var lastFilePath: String? = ""

    init {
        binding.setupStyle()
        binding.root.setOnClickListener {
            clickListener.onAttachmentClick(it, item = fileItem)
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        val attachment = item.file
        lastFilePath = attachment.filePath
        with(binding) {
            val user = (item as ChannelFileItem.Voice).data.user
            tvFileName.text = user?.let {
                userNameBuilder?.invoke(it) ?: it.getPresentableName()
            } ?: ""
            tvDate.text = DateTimeUtil.getDateTimeString(attachment.createdAt, "dd.MM.yy • HH:mm")
            with(tvDuration) {
                fileItem.duration?.let {
                    text = DateTimeUtil.secondsToTime(it)
                    isVisible = true
                } ?: run { isVisible = false }
            }
            icFile.setOnClickListener {
                AudioPlayerHelper.init(lastFilePath, object : OnAudioPlayer {
                    override fun onInitialized() {
                        AudioPlayerHelper.toggle(lastFilePath)
                    }

                    override fun onProgress(position: Long, duration: Long) {
                    }

                    override fun onSeek(position: Long) {
                    }

                    override fun onToggle(playing: Boolean) {
                        binding.root.post { setPlayButtonIcon(playing, binding.icFile) }
                    }

                    override fun onStop() {
                        binding.root.post {
                            setPlayButtonIcon(false, binding.icFile)
                        }
                    }

                    override fun onSpeedChanged(speed: Float) {
                    }

                    override fun onError() {
                    }
                })
            }
        }
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        AudioPlayerHelper.stop(lastFilePath ?: "")
    }

    private fun ItemChannelVoiceBinding.setupStyle() {
        icFile.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    private fun setPlayButtonIcon(playing: Boolean, imageView: ImageView) {
        val iconRes = if (playing) R.drawable.sceyt_ic_pause else R.drawable.sceyt_ic_play
        imageView.setImageResource(iconRes)
    }
}