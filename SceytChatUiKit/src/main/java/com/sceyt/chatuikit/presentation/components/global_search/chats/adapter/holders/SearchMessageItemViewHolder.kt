package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMessageBinding
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
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

    fun bind(item: GlobalSearchListItem.MessageItem, query: String, showMessageChannel: Boolean) {
        val result = item.result
        setSenderAvatarAndName(binding.avatarView, binding.tvTitle, result.message.user)

        binding.tvDate.text = style.messageDateFormatter.format(
            context = context,
            from = Date(result.message.createdAt)
        )
        val body = style.messageBodyFormatter.format(
            context = context,
            from = MessageBodyFormatterAttributes(
                message = result.message,
                mentionTextStyle = style.messageBodyTextStyle
            )
        )
        binding.tvBody.text = highlight(body, query)
        binding.tvBody.maxLines = if (showMessageChannel) 2 else 1
        binding.tvChannel.isVisible = showMessageChannel
        binding.tvChannel.text = style.channelNameFormatter.format(context, result.channel)

        itemView.setOnClickListener {
            onMessageClickListener?.invoke(result.message.id, result.channel)
        }
    }

    protected open fun setSenderAvatarAndName(
        avatarView: AvatarView,
        tvName: TextView,
        user: SceytUser?,
    ) {
        user ?: return
        tvName.text = style.senderNameFormatter.format(context, user)
        style.userAvatarRenderer.render(
            context = context,
            from = user,
            style = style.avatarStyle,
            avatarView = avatarView
        )
    }

    protected open fun highlight(text: CharSequence, query: String): CharSequence {
        if (query.isBlank() || text.isBlank()) return text
        val spannable = SpannableStringBuilder(text)
        val queryLower = query.lowercase()
        val textLower = text.toString().lowercase()
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
        style.senderNameTextStyle.apply(tvTitle)
        style.messageBodyTextStyle.apply(tvBody)
        style.metaTextStyle.apply(tvDate)
        style.metaTextStyle.apply(tvChannel)
    }
}
