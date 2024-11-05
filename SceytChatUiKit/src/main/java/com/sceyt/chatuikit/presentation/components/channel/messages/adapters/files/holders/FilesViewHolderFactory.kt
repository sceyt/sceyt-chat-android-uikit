package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.databinding.SceytMessageFileItemBinding
import com.sceyt.chatuikit.databinding.SceytMessageImageItemBinding
import com.sceyt.chatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class FilesViewHolderFactory(
        context: Context,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) {

    private val layoutInflater = LayoutInflater.from(context)
    private lateinit var style: MessageItemStyle

    internal fun setStyle(style: MessageItemStyle) {
        this.style = style
    }

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseMessageFileViewHolder {
        return when (viewType) {
            FileViewType.File.ordinal -> {
                MessageFileViewHolder(SceytMessageFileItemBinding.inflate(layoutInflater, parent, false),
                    style, messageListeners, needMediaDataCallback)
            }

            FileViewType.Image.ordinal -> {
                MessageImageViewHolder(SceytMessageImageItemBinding.inflate(layoutInflater, parent, false),
                    style, messageListeners, needMediaDataCallback)
            }

            FileViewType.Video.ordinal -> {
                MessageVideoViewHolder(SceytMessageVideoItemBinding.inflate(layoutInflater, parent, false),
                    style, messageListeners, needMediaDataCallback)
            }

            else ->  {
                MessageFileViewHolder(SceytMessageFileItemBinding.inflate(layoutInflater, parent, false),
                    style, messageListeners, needMediaDataCallback)
            }
        }
    }

    fun getItemViewType(item: FileListItem): Int {
        return when (item.type) {
            AttachmentTypeEnum.Image -> FileViewType.Image.ordinal
            AttachmentTypeEnum.Video -> FileViewType.Video.ordinal
            else -> AttachmentTypeEnum.File.ordinal
        }
    }

    enum class FileViewType {
        File, Image, Video
    }
}