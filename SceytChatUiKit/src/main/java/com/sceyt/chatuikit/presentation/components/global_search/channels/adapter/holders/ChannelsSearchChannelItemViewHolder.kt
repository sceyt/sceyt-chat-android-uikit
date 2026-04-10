package com.sceyt.chatuikit.presentation.components.global_search.channels.adapter.holders

import android.widget.TextView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders.SearchChannelItemViewHolder
import com.sceyt.chatuikit.styles.channel.ChannelItemStyle
import java.util.Locale

open class ChannelsSearchChannelItemViewHolder(
    binding: SceytItemChannelBinding,
    itemStyle: ChannelItemStyle,
    listeners: ChannelClickListeners.ClickListeners,
    attachDetachListener: ((ChannelListItem?, attached: Boolean) -> Unit)? = null,
) : SearchChannelItemViewHolder(binding, itemStyle, listeners, attachDetachListener) {

    override fun setLastMessagedText(channel: SceytChannel, textView: TextView) {
        textView.text = formatSubscriberCount(channel.memberCount)
    }

    private fun formatSubscriberCount(count: Long): String {
        val formatted = when {
            count >= 1_000_000 -> {
                val value = count / 100_000.0
                val rounded = (value / 10.0)
                if (count % 1_000_000 == 0L) "${count / 1_000_000}M"
                else "${
                    String.format(Locale.getDefault(), "%.1f", rounded).trimEnd('0').trimEnd('.')
                }M"
            }

            count >= 1_000 -> {
                val value = count / 100.0
                val rounded = (value / 10.0)
                if (count % 1_000 == 0L) "${count / 1_000}k"
                else "${
                    String.format(Locale.getDefault(), "%.1f", rounded).trimEnd('0').trimEnd('.')
                }k"
            }

            else -> count.toString()
        }
        return if (count == 1L) {
            context.getString(R.string.sceyt_subscriber_count, formatted)
        } else {
            context.getString(R.string.sceyt_subscribers_count, formatted)
        }
    }
}
