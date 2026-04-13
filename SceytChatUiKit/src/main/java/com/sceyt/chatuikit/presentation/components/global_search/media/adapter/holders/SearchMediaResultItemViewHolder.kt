package com.sceyt.chatuikit.presentation.components.global_search.media.adapter.holders

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMediaResultBinding
import com.sceyt.chatuikit.formatters.attributes.SearchMessageResultFormatterAttributes
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.styles.search.ChatsSearchMessageItemStyle
import com.sceyt.chatuikit.styles.search.MediaSearchMessageItemStyle
import java.util.Date

open class SearchMediaResultItemViewHolder(
    private val style: MediaSearchMessageItemStyle,
    private val messageItemStyle: ChatsSearchMessageItemStyle,
    private val binding: SceytItemGlobalSearchMediaResultBinding,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val onAttachmentClickListener: ((GlobalSearchListItem.AttachmentItem) -> Unit)?,
) : BaseFileViewHolder<GlobalSearchListItem.AttachmentItem>(binding.root, needMediaDataCallback) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: GlobalSearchListItem.AttachmentItem) = with(binding) {
        super.bind(item)

        val result = item.result
        tvTitle.text = style.channelNameFormatter.format(context, result.channel)
        style.channelAvatarRenderer.render(
            context = context,
            from = result.channel,
            style = style.avatarStyle,
            avatarView = avatarView,
        )
        tvDate.text = style.messageDateFormatter.format(
            context = context,
            from = Date(result.attachment.createdAt),
        )

        val body = style.searchMessageResultBodyFormatter.format(
            context = context,
            from = SearchMessageResultFormatterAttributes(
                channel = result.channel,
                message = result.message,
                searchMessageItemStyle = messageItemStyle,
            ),
        )
        tvBody.text = highlight(body, item.query)

        itemView.setOnClickListener {
            onAttachmentClickListener?.invoke(item)
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.previewImage)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
            }

            Downloading -> {
                if (isOnBind) viewHolderHelper.loadBlurThumb(imageView = binding.previewImage)
            }

            Downloaded, Uploaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.previewImage, ::requestThumb)
            }

            PendingUpload, ErrorUpload, PauseUpload, Uploading -> {
                if (isOnBind) viewHolderHelper.drawThumbOrRequest(
                    binding.previewImage,
                    ::requestThumb
                )
            }

            PauseDownload, ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.previewImage)
            }

            FilePathChanged -> {
                if (fileItem.thumbPath.isNullOrBlank()) requestThumb()
            }

            ThumbLoaded -> {
                if (isValidThumb(data.thumbData))
                    viewHolderHelper.drawImageWithBlurredThumb(
                        fileItem.thumbPath,
                        binding.previewImage
                    )
            }

            Preparing, WaitingToUpload -> Unit
        }
    }

    override fun needThumbFor() = ThumbFor.GlobalSearch

    protected open fun highlight(text: CharSequence, query: String): CharSequence {
        if (query.isBlank() || text.isBlank()) return text
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return text
        val spannable = SpannableStringBuilder(text)
        val textLower = text.toString().lowercase()
        for (word in words) {
            val wordLower = word.lowercase()
            var start = textLower.indexOf(wordLower)
            while (start >= 0) {
                val end = start + wordLower.length
                spannable.setSpan(
                    ForegroundColorSpan(style.highlightTextColor),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                start = textLower.indexOf(wordLower, end)
            }
        }
        return spannable
    }

    private fun SceytItemGlobalSearchMediaResultBinding.applyStyle() {
        style.titleTextStyle.apply(tvTitle)
        style.messageBodyTextStyle.apply(tvBody)
        style.dateTextStyle.apply(tvDate)
    }
}
