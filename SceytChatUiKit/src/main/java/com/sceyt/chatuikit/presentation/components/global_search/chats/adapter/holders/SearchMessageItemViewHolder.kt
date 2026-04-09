package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMessageBinding
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.displayName
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class SearchMessageItemViewHolder(
    private val style: GlobalSearchStyle,
    private val binding: SceytItemGlobalSearchMessageBinding,
    private val onMessageClickListener: ((Long, SceytChannel) -> Unit)?,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.applyStyle()
    }

    fun bind(item: GlobalSearchListItem.MessageItem, query: String, showMessageChannel: Boolean) {
        val result = item.result
        val user = result.message.user ?: createEmptyUser(
            id = result.channel.id.toString(),
            displayName = channelTitle(result.channel).toString()
        )

        SceytChatUIKit.renderers.userAvatarRenderer.render(
            binding.root.context,
            user,
            style.avatarStyle,
            binding.avatarView
        )

        binding.tvTitle.text = user.displayName()
        binding.tvDate.text = DateTimeUtil.getDateTimeString(result.message.createdAt)
        binding.tvBody.text = highlight(result.message.body, query)
        binding.tvBody.maxLines = if (showMessageChannel) 2 else 1
        binding.tvChannel.isVisible = showMessageChannel
        binding.tvChannel.text = channelTitle(result.channel)

        itemView.setOnClickListener {
            onMessageClickListener?.invoke(result.message.id, result.channel)
        }
    }

    private fun channelTitle(channel: SceytChannel): CharSequence {
        return SceytChatUIKit.formatters.channelNameFormatter.format(itemView.context, channel)
    }

    protected fun highlight(text: String, query: String): CharSequence {
        if (query.isBlank() || text.isBlank()) return text
        val spannable = SpannableStringBuilder(text)
        val queryLower = query.lowercase()
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

    private fun SceytItemGlobalSearchMessageBinding.applyStyle() {
        style.titleTextStyle.apply(tvTitle)
        style.subtitleTextStyle.apply(tvBody)
        style.metaTextStyle.apply(tvDate)
        style.metaTextStyle.apply(tvChannel)
    }
}
