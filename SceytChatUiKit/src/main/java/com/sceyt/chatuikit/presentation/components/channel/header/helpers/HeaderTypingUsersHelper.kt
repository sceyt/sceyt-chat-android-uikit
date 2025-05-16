package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.TypingTitleFormatterAttributes
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HeaderTypingUsersHelper(
        private val context: Context,
        private val channel: SceytChannel,
        private val typingTitleFormatter: Formatter<TypingTitleFormatterAttributes>,
        private var typingTextUpdatedListener: (CharSequence) -> Unit,
        private val typingStateUpdated: (Boolean) -> Unit,
        private val showTypingUsersInSequence: Boolean
) {
    private val typingCancelHelper by lazy { TypingCancelHelper() }
    private val _typingUsers by lazy { mutableSetOf<SceytUser>() }
    private val debounceHelpers = mutableMapOf<String, DebounceHelper>()
    private var updateTypingJob: Job? = null

    private fun updateTypingText() {
        if (!showTypingUsersInSequence) {
            val title = typingTitleFormatter.format(context, TypingTitleFormatterAttributes(
                channel = channel,
                users = typingUsers
            ))
            typingTextUpdatedListener.invoke(title)
            return
        }

        if (_typingUsers.isEmpty() || _typingUsers.size == 1) {
            updateTypingJob?.cancel()
            typingTextUpdatedListener.invoke(initTypingTitle(typingUsers))
        } else {
            if (updateTypingJob == null || updateTypingJob?.isActive?.not() == true)
                updateTypingTitleEveryTwoSecond()
        }
    }

    private fun updateTypingTitleEveryTwoSecond() {
        updateTypingJob?.cancel()
        updateTypingJob = context.asComponentActivity().lifecycleScope.launch {
            while (true) {
                _typingUsers.toList().forEach {
                    typingTextUpdatedListener.invoke(initTypingTitle(listOf(it)))
                    delay(2000)
                }
            }
        }
    }

    private fun initTypingTitle(users: List<SceytUser>): CharSequence {
        return typingTitleFormatter.format(
            context = context,
            from = TypingTitleFormatterAttributes(
                channel = channel,
                users = users,
            )
        )
    }

    private fun setTyping(data: ChannelTypingEventData) {
        if (data.user.id == SceytChatUIKit.currentUserId) return
        val debounceHelper = debounceHelpers.getOrPut(data.user.id) {
            DebounceHelper(200, context.asComponentActivity().lifecycleScope)
        }
        debounceHelper.submit {
            if (data.typing) {
                _typingUsers.add(data.user)
            } else
                _typingUsers.remove(data.user)

            updateTypingText()
            setTypingState(_typingUsers.isNotEmpty())
        }
    }

    private fun setTypingState(typing: Boolean) {
        typingStateUpdated.invoke(typing)
    }

    fun onTypingEvent(data: ChannelTypingEventData) {
        typingCancelHelper.await(data) {
            setTyping(it)
        }
        setTyping(data)
    }

    val typingUsers: List<SceytUser>
        get() = _typingUsers.toList()

    val isTyping: Boolean
        get() = _typingUsers.isNotEmpty()
}