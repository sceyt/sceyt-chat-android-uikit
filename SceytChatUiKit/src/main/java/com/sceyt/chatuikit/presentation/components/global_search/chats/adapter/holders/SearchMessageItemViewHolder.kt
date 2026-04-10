package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMessageBinding
import com.sceyt.chatuikit.formatters.attributes.SearchMessageResultFormatterAttributes
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
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
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return text
        val textLower = text.toString().lowercase()

        // Priority 1: full phrase match (words joined by single space)
        val phrase = words.joinToString(" ").lowercase()
        val phraseMatch = textLower.indexOf(phrase)

        val matchStart = if (phraseMatch >= 0) {
            phraseMatch
        } else {
            // Priority 2: earliest individual word match
            words.minOfOrNull { word ->
                val idx = textLower.indexOf(word.lowercase())
                if (idx >= 0) idx else Int.MAX_VALUE
            }.takeIf { it != Int.MAX_VALUE } ?: return text
        }

        if (matchStart < CONTEXT_BEFORE_MATCH) return text
        val trimFrom = matchStart - CONTEXT_BEFORE_MATCH
        return SpannableStringBuilder("…").append(text.subSequence(trimFrom, text.length))
    }

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
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                start = textLower.indexOf(wordLower, end)
            }
        }
        return spannable
    }

    companion object {
        private const val CONTEXT_BEFORE_MATCH = 10
    }

    private fun SceytItemGlobalSearchMessageBinding.applyStyle() {
        style.titleTextStyle.apply(tvTitle)
        style.messageBodyTextStyle.apply(tvBody)
        style.dateTextStyle.apply(tvDate)
    }
}
