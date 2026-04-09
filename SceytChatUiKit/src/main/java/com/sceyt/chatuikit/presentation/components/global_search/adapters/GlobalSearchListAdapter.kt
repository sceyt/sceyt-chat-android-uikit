package com.sceyt.chatuikit.presentation.components.global_search.adapters

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelLinkBinding
import com.sceyt.chatuikit.databinding.SceytItemChannelVoiceBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMediaResultBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.displayName
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class GlobalSearchListAdapter(
    private val style: GlobalSearchStyle,
    private val onChannelClick: (SceytChannel) -> Unit,
    private val onMessageClick: (messageId: Long, channel: SceytChannel) -> Unit,
    private val onAttachmentClick: (GlobalSearchListItem.AttachmentItem) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        private const val VIEW_TYPE_SECTION = 0
        private const val VIEW_TYPE_CHANNEL = 1
        private const val VIEW_TYPE_MESSAGE = 2
        private const val VIEW_TYPE_MEDIA = 3
        private const val VIEW_TYPE_FILE = 4
        private const val VIEW_TYPE_VOICE = 5
        private const val VIEW_TYPE_LINK = 6
    }

    private var items: List<GlobalSearchListItem> = emptyList()
    private var highlightQuery: String = ""
    private var showMessageChannel: Boolean = true

    private val channelClickListeners = ChannelClickListenersImpl().apply {
        setListener(ChannelClickListeners.ChannelClickListener { _, item ->
            onChannelClick(item.channel)
        })
    }

    open fun submit(items: List<GlobalSearchListItem>, query: String, showMessageChannel: Boolean) {
        this.items = items
        this.highlightQuery = query
        this.showMessageChannel = showMessageChannel
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is GlobalSearchListItem.SectionHeader -> VIEW_TYPE_SECTION
            is GlobalSearchListItem.ChannelItem -> VIEW_TYPE_CHANNEL
            is GlobalSearchListItem.MessageItem -> VIEW_TYPE_MESSAGE
            is GlobalSearchListItem.AttachmentItem -> when (item.result.kind) {
                GlobalSearchAttachmentKind.Media -> VIEW_TYPE_MEDIA
                GlobalSearchAttachmentKind.File -> VIEW_TYPE_FILE
                GlobalSearchAttachmentKind.Voice -> VIEW_TYPE_VOICE
                GlobalSearchAttachmentKind.Link -> VIEW_TYPE_LINK
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION -> SectionViewHolder(
                SceytItemGlobalSearchSectionBinding.inflate(inflater, parent, false),
                style
            )

            VIEW_TYPE_CHANNEL -> ChannelItemViewHolder(
                ChannelViewHolder(
                    binding = SceytItemChannelBinding.inflate(inflater, parent, false),
                    itemStyle = style.chatsPageStyle.channelItemStyle,
                    listeners = channelClickListeners
                )
            )

            VIEW_TYPE_MESSAGE -> MessageItemViewHolder(
                SceytItemGlobalSearchMessageBinding.inflate(inflater, parent, false),
                style,
                ::highlight
            )

            VIEW_TYPE_MEDIA -> MediaResultViewHolder(
                SceytItemGlobalSearchMediaResultBinding.inflate(inflater, parent, false),
                style,
                ::highlight
            )

            VIEW_TYPE_FILE -> FileResultViewHolder(
                SceytItemChannelFileBinding.inflate(inflater, parent, false),
                style,
                ::highlight
            )

            VIEW_TYPE_VOICE -> VoiceResultViewHolder(
                SceytItemChannelVoiceBinding.inflate(inflater, parent, false),
                style,
                ::highlight
            )

            VIEW_TYPE_LINK -> LinkResultViewHolder(
                SceytItemChannelLinkBinding.inflate(inflater, parent, false),
                style,
                ::highlight
            )

            else -> error("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is GlobalSearchListItem.SectionHeader -> (holder as SectionViewHolder).bind(item.titleRes)
            is GlobalSearchListItem.ChannelItem -> (holder as ChannelItemViewHolder).bind(item.channel)
            is GlobalSearchListItem.MessageItem -> (holder as MessageItemViewHolder).bind(item)
            is GlobalSearchListItem.AttachmentItem -> {
                when (holder) {
                    is MediaResultViewHolder -> holder.bind(item)
                    is FileResultViewHolder -> holder.bind(item)
                    is VoiceResultViewHolder -> holder.bind(item)
                    is LinkResultViewHolder -> holder.bind(item)
                }
            }
        }
    }

    private fun highlight(text: String): CharSequence {
        if (highlightQuery.isBlank() || text.isBlank()) return text
        val spannable = SpannableStringBuilder(text)
        val queryLower = highlightQuery.lowercase()
        val textLower = text.lowercase()
        var start = textLower.indexOf(queryLower)
        while (start >= 0) {
            val end = start + queryLower.length
            spannable.setSpan(
                ForegroundColorSpan(style.highlightTextColor),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            start = textLower.indexOf(queryLower, end)
        }
        return spannable
    }

    private inner class SectionViewHolder(
        private val binding: SceytItemGlobalSearchSectionBinding,
        private val style: GlobalSearchStyle,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(titleRes: Int) {
            style.chatsPageStyle.separatorTextStyle.apply(binding.root)
            binding.root.setText(titleRes)
        }
    }

    private inner class ChannelItemViewHolder(
        private val holder: ChannelViewHolder,
    ) : RecyclerView.ViewHolder(holder.itemView) {
        fun bind(channel: SceytChannel) {
            holder.bind(ChannelListItem.ChannelItem(channel), ChannelDiff.DEFAULT)
        }
    }

    private abstract inner class ResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        protected fun bindMessageClick(item: GlobalSearchListItem.MessageItem) {
            itemView.setOnClickListener {
                onMessageClick(item.result.message.id, item.result.channel)
            }
        }

        protected fun bindAttachmentClick(item: GlobalSearchListItem.AttachmentItem) {
            itemView.setOnClickListener {
                onAttachmentClick(item)
            }
        }

        protected fun formatTime(epoch: Long): String = DateTimeUtil.getDateTimeString(epoch)

        protected fun channelTitle(channel: SceytChannel): String {
            return SceytChatUIKit.formatters.channelNameFormatter.format(itemView.context, channel)
                .toString()
        }

        protected fun loadImage(
            imageView: ImageView,
            attachment: SceytAttachment,
            fallbackRes: Int,
        ) {
            val model = attachment.filePath
                ?: attachment.url
                ?: attachment.linkPreviewDetails?.imageUrl

            if (model == null) {
                imageView.setImageResource(fallbackRes)
                return
            }

            Glide.with(imageView.context.applicationContext)
                .load(model)
                .placeholder(fallbackRes)
                .centerCrop()
                .into(imageView)
        }
    }

    private inner class MessageItemViewHolder(
        private val binding: SceytItemGlobalSearchMessageBinding,
        private val style: GlobalSearchStyle,
        private val highlighter: (String) -> CharSequence,
    ) : ResultViewHolder(binding.root) {
        fun bind(item: GlobalSearchListItem.MessageItem) {
            val result = item.result
            val user = result.message.user ?: createEmptyUser(
                result.channel.id.toString(),
                channelTitle(result.channel)
            )
            SceytChatUIKit.renderers.userAvatarRenderer.render(
                binding.root.context,
                user,
                style.avatarStyle,
                binding.avatarView
            )

            style.titleTextStyle.apply(binding.tvTitle)
            style.subtitleTextStyle.apply(binding.tvBody)
            style.metaTextStyle.apply(binding.tvDate)

            binding.tvTitle.text = user.displayName()
            binding.tvDate.text = formatTime(result.message.createdAt)
            binding.tvBody.text = highlighter(result.message.body)
            binding.tvBody.maxLines = if (showMessageChannel) 2 else 1
            bindMessageClick(item)
        }
    }

    private inner class MediaResultViewHolder(
        private val binding: SceytItemGlobalSearchMediaResultBinding,
        private val style: GlobalSearchStyle,
        private val highlighter: (String) -> CharSequence,
    ) : ResultViewHolder(binding.root) {
        fun bind(item: GlobalSearchListItem.AttachmentItem) {
            val sender = item.result.sender ?: createEmptyUser(
                item.result.channel.id.toString(),
                channelTitle(item.result.channel)
            )
            SceytChatUIKit.renderers.userAvatarRenderer.render(
                binding.root.context,
                sender,
                style.avatarStyle,
                binding.avatarView
            )

            style.titleTextStyle.apply(binding.tvTitle)
            style.subtitleTextStyle.apply(binding.tvBody)
            style.metaTextStyle.apply(binding.tvDate)

            binding.tvTitle.text = sender.displayName()
            binding.tvDate.text = formatTime(item.result.message.createdAt)
            binding.tvBody.text = highlighter(
                item.result.message.body.ifBlank { channelTitle(item.result.channel) }
            )
            loadImage(
                binding.previewImage,
                item.result.attachment,
                R.drawable.sceyt_ic_empty_medias
            )
            bindAttachmentClick(item)
        }
    }

    private inner class FileResultViewHolder(
        private val binding: SceytItemChannelFileBinding,
        private val style: GlobalSearchStyle,
        private val highlighter: (String) -> CharSequence,
    ) : ResultViewHolder(binding.root) {
        fun bind(item: GlobalSearchListItem.AttachmentItem) {
            style.titleTextStyle.apply(binding.tvFileName)
            style.metaTextStyle.apply(binding.tvFileSizeAndDate)
            binding.tvFileName.text = highlighter(item.result.attachment.name)
            binding.tvFileSizeAndDate.text = buildString {
                append(item.result.attachment.fileSize.toPrettySize())
                append(" • ")
                append(formatTime(item.result.message.createdAt))
            }
            binding.loadProgress.visibility = View.GONE
            bindAttachmentClick(item)
        }
    }

    private inner class VoiceResultViewHolder(
        private val binding: SceytItemChannelVoiceBinding,
        private val style: GlobalSearchStyle,
        private val highlighter: (String) -> CharSequence,
    ) : ResultViewHolder(binding.root) {
        fun bind(item: GlobalSearchListItem.AttachmentItem) {
            val senderName = item.result.sender?.displayName().orEmpty().ifBlank {
                channelTitle(item.result.channel)
            }
            val duration = item.result.attachment.getInfoFromMetadata().duration ?: 0L

            style.titleTextStyle.apply(binding.tvUserName)
            style.metaTextStyle.apply(binding.tvDate)
            style.metaTextStyle.apply(binding.tvDuration)

            binding.tvUserName.text = highlighter(senderName)
            binding.tvDate.text = formatTime(item.result.message.createdAt)
            binding.tvDuration.text = DateTimeUtil.convertMillisToString(duration)
            binding.loadProgress.visibility = View.GONE
            bindAttachmentClick(item)
        }
    }

    private inner class LinkResultViewHolder(
        private val binding: SceytItemChannelLinkBinding,
        private val style: GlobalSearchStyle,
        private val highlighter: (String) -> CharSequence,
    ) : ResultViewHolder(binding.root) {
        fun bind(item: GlobalSearchListItem.AttachmentItem) {
            val details = item.result.attachment.linkPreviewDetails
            val title = details?.title ?: details?.siteName
            val description = details?.description
            val url = details?.url ?: item.result.attachment.url ?: details?.link.orEmpty()

            style.titleTextStyle.apply(binding.tvLinkName)
            style.subtitleTextStyle.apply(binding.tvLinkUrl)
            style.metaTextStyle.apply(binding.tvLinkDescription)

            binding.tvLinkName.isVisible = !title.isNullOrBlank()
            binding.tvLinkName.text = title?.let(highlighter)
            binding.tvLinkUrl.text = highlighter(url)
            binding.tvLinkDescription.isVisible = !description.isNullOrBlank()
            binding.tvLinkDescription.text = description?.let(highlighter)

            val model = details?.imageUrl ?: details?.faviconUrl
            if (model == null) {
                binding.icLinkImage.setImageResource(R.drawable.sceyt_ic_link)
                binding.icLinkImage.scaleType = ImageView.ScaleType.CENTER
            } else {
                binding.icLinkImage.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(binding.icLinkImage.context.applicationContext)
                    .load(model)
                    .placeholder(R.drawable.sceyt_ic_link)
                    .centerCrop()
                    .into(binding.icLinkImage)
            }
            bindAttachmentClick(item)
        }
    }
}
