package com.sceyt.chatuikit.presentation.uicomponents.conversationheader

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.extensions.getPresentableFirstName
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HeaderTypingUsersHelper(
        private val context: Context,
        private val isGroup: Boolean,
        private var typingTextUpdatedListener: (String) -> Unit,
        private val typingStateUpdated: (Boolean) -> Unit
) {
    private val typingCancelHelper by lazy { TypingCancelHelper() }
    private var typingTextBuilder: ((SceytMember) -> String)? = null
    private val _typingUsers by lazy { mutableSetOf<SceytMember>() }
    private var userNameFormatter: UserNameFormatter? = SceytKitConfig.userNameFormatter
    private val debounceHelper by lazy { DebounceHelper(200, context.asComponentActivity().lifecycleScope) }
    private var updateTypingJob: Job? = null
    var isTyping: Boolean = false
        private set

    private fun updateTypingText() {
        when {
            _typingUsers.isEmpty() -> {
                updateTypingJob?.cancel()
            }

            _typingUsers.size == 1 -> {
                typingTextUpdatedListener.invoke(initTypingTitle(_typingUsers.last()))
                updateTypingJob?.cancel()
            }

            else -> {
                if (updateTypingJob == null || updateTypingJob?.isActive?.not() == true)
                    updateTypingTitleEveryTwoSecond()
            }
        }
    }

    private fun updateTypingTitleEveryTwoSecond() {
        updateTypingJob?.cancel()
        updateTypingJob = context.asComponentActivity().lifecycleScope.launch {
            while (true) {
                _typingUsers.toList().forEach {
                    typingTextUpdatedListener.invoke(initTypingTitle(it))
                    delay(2000)
                }
            }
        }
    }

    private fun initTypingTitle(member: SceytMember): String {
        return typingTextBuilder?.invoke(member) ?: if (isGroup)
            buildString {
                append(userNameFormatter?.format(member.user)
                        ?: member.getPresentableFirstName().take(10))
                append(" ${context.getString(R.string.sceyt_typing)}")
            }
        else context.getString(R.string.sceyt_typing)
    }

    private fun setTyping(data: ChannelTypingEventData) {
        if (data.member.id == ChatClient.getClient().user?.id) return
        debounceHelper.submit {
            val typing = data.typing
            isTyping = typing

            if (isGroup) {
                if (typing) {
                    _typingUsers.add(data.member)
                } else
                    _typingUsers.remove(data.member)

                updateTypingText()
            } else
                typingTextUpdatedListener.invoke(initTypingTitle(data.member))

            setTypingState(typing)
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

    fun setTypingTextBuilder(builder: (SceytMember) -> String) {
        typingTextBuilder = builder
    }

    fun setUserNameFormatter(formatter: UserNameFormatter) {
        userNameFormatter = formatter
    }

    val typingUsers get() = _typingUsers.toList()
}