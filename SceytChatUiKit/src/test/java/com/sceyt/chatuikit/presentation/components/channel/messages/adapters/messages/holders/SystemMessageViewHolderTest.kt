package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.text.TextUtils
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.SystemMessageAction
import com.sceyt.chatuikit.databinding.SceytItemSystemMessageBinding
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.styles.messages_list.item.SystemMessageItemStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SystemMessageViewHolderTest {

    private fun holder(): SystemMessageViewHolder {
        val context = RuntimeEnvironment.getApplication()
        // Android resources aren't loaded in unit tests, so build the row instead of inflating it.
        val root = ConstraintLayout(context).apply {
            addView(TextView(context).apply { id = R.id.title })
        }
        val systemStyle = SystemMessageItemStyle(
            textStyle = mock(),
            textFormatter = Formatter { _, _ -> "text" },
            backgroundStyle = mock(),
        )
        val style = mock<MessageItemStyle> {
            on { systemMessageItemStyle } doReturn systemStyle
        }
        return SystemMessageViewHolder(SceytItemSystemMessageBinding.bind(root), style, displayedListener = null)
    }

    private fun systemItem(action: SystemMessageAction) = MessageListItem.MessageItem(
        createMessage(createdAt = 1).copy(body = action.value)
    )

    @Test
    fun `pin system message is single line`() {
        val holder = holder()

        holder.bind(systemItem(SystemMessageAction.PinMessage), MessageDiff.DEFAULT)

        assertThat(holder.binding.title.maxLines).isEqualTo(1)
        assertThat(holder.binding.title.ellipsize).isEqualTo(TextUtils.TruncateAt.END)
    }

    @Test
    fun `other system message on a recycled pin holder is not truncated`() {
        val holder = holder()
        holder.bind(systemItem(SystemMessageAction.PinMessage), MessageDiff.DEFAULT)

        holder.bind(systemItem(SystemMessageAction.MemberAdded), MessageDiff.DEFAULT)

        assertThat(holder.binding.title.maxLines).isEqualTo(Int.MAX_VALUE)
        assertThat(holder.binding.title.ellipsize).isNull()
    }
}
