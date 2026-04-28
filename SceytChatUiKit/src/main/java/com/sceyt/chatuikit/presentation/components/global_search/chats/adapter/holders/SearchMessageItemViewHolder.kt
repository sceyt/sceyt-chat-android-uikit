package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMessageBinding
import com.sceyt.chatuikit.formatters.attributes.SearchMessageResultFormatterAttributes
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.findWordPrefixIndex
import com.sceyt.chatuikit.presentation.components.global_search.highlightQueryWords
import com.sceyt.chatuikit.shared.utils.tokenizeGlobalSearchQuery
import com.sceyt.chatuikit.styles.search.ChatsSearchMessageItemStyle
import java.util.Date

open class SearchMessageItemViewHolder(
    private val style: ChatsSearchMessageItemStyle,
    private val binding: SceytItemGlobalSearchMessageBinding,
    private val onMessageClickListener: ((Long, SceytChannel) -> Unit)?,
) : RecyclerView.ViewHolder(binding.root) {

    protected val context: Context get() = binding.root.context

    init {
        binding.applyStyle()
    }

    fun bind(item: GlobalSearchListItem.MessageItem) = with(binding) {
        val result = item.result
        tvTitle.text = style.channelNameFormatter.format(context, result.channel)
        style.channelAvatarRenderer.render(
            context = context,
            from = result.channel,
            style = style.avatarStyle,
            avatarView = avatarView
        )

        binding.tvDate.text = style.messageDateFormatter.format(
            context = context,
            from = Date(result.message.createdAt)
        )
        val body = style.searchMessageResultBodyFormatter.format(
            context = context,
            from = SearchMessageResultFormatterAttributes(
                channel = result.channel,
                message = result.message,
                searchMessageItemStyle = style
            )
        )
        binding.tvBody.text = highlight(trimBodyToShowMatch(body, item.query), item.query)

        itemView.setOnClickListener {
            onMessageClickListener?.invoke(result.message.id, result.channel)
        }
    }

    protected open fun trimBodyToShowMatch(text: CharSequence, query: String): CharSequence {
        if (query.isBlank() || text.isBlank()) return text
        val words = tokenizeGlobalSearchQuery(query)
        if (words.isEmpty()) return text
        val textLower = text.toString().lowercase()

        // Priority 1: full phrase word-prefix match
        val phrase = words.joinToString(" ").lowercase()
        val phraseMatch = findWordPrefixIndex(textLower, phrase)

        val matchStart = if (phraseMatch >= 0) {
            phraseMatch
        } else {
            // Priority 2: earliest individual word-prefix match
            words.minOfOrNull { word ->
                val idx = findWordPrefixIndex(textLower, word.lowercase())
                if (idx >= 0) idx else Int.MAX_VALUE
            }.takeIf { it != Int.MAX_VALUE } ?: return text
        }

        // If the view has not been laid out yet, show full text rather than guess.
        val width = binding.tvBody.width
        if (width <= 0) return text

        val maxLines = binding.tvBody.maxLines.takeIf { it > 0 } ?: return text

        // Build a full layout to find exactly which visual line the match falls on.
        // StaticLayout accounts for both hard line breaks (\n) and soft word-wrap,
        // so this works correctly for plain paragraphs and multi-line text alike.
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, binding.tvBody.paint, width)
            .build()
        val matchLine = layout.getLineForOffset(matchStart)

        // Match is already visible within the allowed lines — show the full text.
        if (matchLine < maxLines) return text

        // Match is beyond the fold: trim to the start of its visual line.
        val lineStart = layout.getLineStart(matchLine)
        if (lineStart < MIN_TRIM_CHARS) return text
        return SpannableStringBuilder("… ").append(text.subSequence(lineStart, text.length))
    }

    protected open fun highlight(text: CharSequence, query: String): CharSequence =
        highlightQueryWords(text, query, style.highlightTextColor)

    companion object {
        private const val MIN_TRIM_CHARS = 5
    }

    private fun SceytItemGlobalSearchMessageBinding.applyStyle() {
        style.titleTextStyle.apply(tvTitle)
        style.messageBodyTextStyle.apply(tvBody)
        style.dateTextStyle.apply(tvDate)
    }
}
