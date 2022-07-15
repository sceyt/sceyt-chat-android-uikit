package com.sceyt.chat.ui.presentation.uicomponents.conversationheader

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.*
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.data.models.channels.SceytGroupChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.databinding.SceytConversationHeaderViewBinding
import com.sceyt.chat.ui.extensions.asActivity
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.extensions.getPresentableFirstName
import com.sceyt.chat.ui.extensions.getString
import com.sceyt.chat.ui.presentation.uicomponents.conversationheader.listeners.HeaderClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversationheader.listeners.HeaderClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.chat.ui.sceytconfigs.ConversationHeaderViewStyle
import com.sceyt.chat.ui.shared.utils.BindingUtil
import com.sceyt.chat.ui.shared.utils.DateTimeUtil.setLastActiveDateByTime
import kotlinx.coroutines.*

class ConversationHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), HeaderClickListeners.ClickListeners {

    private val binding: SceytConversationHeaderViewBinding
    private var clickListeners = HeaderClickListenersImpl(this)
    private lateinit var channel: SceytChannel
    private val typingUsers by lazy { mutableSetOf<SceytMember>() }
    private var isTyping: Boolean = false
    private var updateTypingJob: Job? = null
    private var isGroup = false

    init {
        binding = SceytConversationHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)

        if (!isInEditMode)
            BindingUtil.themedBackgroundColor(this, R.color.sceyt_color_bg)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ConversationHeaderView)
            ConversationHeaderViewStyle.updateWithAttributes(a)
            a.recycle()
        }
        init()
    }

    private fun init() {
        binding.setUpStyle()

        binding.icBack.setOnClickListener {
            clickListeners.onBackClick(it)
        }

        binding.avatar.setOnClickListener {
            clickListeners.onAvatarClick(it)
        }

        binding.layoutToolbar.setOnClickListener {
            clickListeners.onToolbarClick(it)
        }
    }

    private fun SceytConversationHeaderViewBinding.setUpStyle() {
        icBack.setImageResource(ConversationHeaderViewStyle.backIcon)
        title.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.titleColor))
        subTitle.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.subTitleColor))
    }

    private fun setChannelSubTitle(channel: SceytChannel) {
        post {
            val title = if (channel is SceytDirectChannel) {
                val member = channel.peer ?: return@post
                if (member.presence?.state == PresenceState.Online) {
                    getString(R.string.online)
                } else {
                    if (member.presence?.lastActiveAt != 0L)
                        setLastActiveDateByTime(member.presence.lastActiveAt)
                    else null
                }
            } else getString(R.string.members_count, (channel as SceytGroupChannel).memberCount)

            binding.subTitle.text = title
            binding.subTitle.isVisible = !title.isNullOrBlank() && !isTyping
        }
    }

    internal fun setChannel(channel: SceytChannel) {
        this.channel = channel
        isGroup = channel.isGroup
        val subjAndSUrl = channel.getSubjectAndAvatarUrl()
        binding.avatar.setNameAndImageUrl(subjAndSUrl.first, subjAndSUrl.second)
        binding.title.text = subjAndSUrl.first

        setChannelSubTitle(channel)
    }

    internal fun setReplayMessage(message: SceytMessage?) {
        binding.avatar.isVisible = false
        with(binding.title) {
            text = getString(R.string.thread_replay)
            (layoutParams as MarginLayoutParams).setMargins(binding.avatar.marginLeft, marginTop, marginRight, marginBottom)
        }

        val fullName = message?.from?.fullName
        val subTitleText = String.format(getString(R.string.with), fullName)
        binding.subTitle.text = subTitleText
        binding.subTitle.isVisible = !fullName.isNullOrBlank() && !isTyping
    }

    internal fun onTyping(data: ChannelTypingEventData) {
        if (data.member.id == ChatClient.getClient().user.id) return
        val typing = data.typing
        isTyping = typing

        if (isGroup) {
            if (typing) {
                typingUsers.add(data.member)
            } else
                typingUsers.remove(data.member)

            updateTypingText()
        } else
            binding.tvTyping.text = initTypingTitle(data.member)

        setTypingState(typing)
    }

    private fun initTypingTitle(member: SceytMember): String {
        return if (isGroup)
            buildString {
                append(member.getPresentableFirstName().take(10))
                append(" ${getString(R.string.sceyt_typing)}")
            }
        else getString(R.string.sceyt_typing)
    }

    private fun updateTypingText() {
        when {
            typingUsers.isEmpty() -> {
                updateTypingJob?.cancel()
            }
            typingUsers.size == 1 -> {
                binding.tvTyping.text = initTypingTitle(typingUsers.last())
                updateTypingJob?.cancel()
            }
            else -> {
                if (updateTypingJob == null || updateTypingJob!!.isActive.not())
                    updateTypingTitleEveryTwoSecond()
            }
        }
    }

    private fun updateTypingTitleEveryTwoSecond() {
        updateTypingJob?.cancel()
        updateTypingJob = CoroutineScope(Dispatchers.Main + Job()).launch {
            while (true) {
                typingUsers.toList().forEach {
                    binding.tvTyping.text = initTypingTitle(it)
                    delay(2000)
                }
            }
        }
    }

    private fun setTypingState(typing: Boolean) {
        if ((typing && isGroup.not()) || (isGroup && typingUsers.isNotEmpty())) {
            binding.subTitle.isVisible = false
            binding.groupTyping.isVisible = true
        } else {
            binding.groupTyping.isVisible = false
            binding.subTitle.isVisible = true
        }
    }

    fun setCustomClickListener(headerClickListenersImpl: HeaderClickListenersImpl) {
        clickListeners = headerClickListenersImpl
    }

    fun setClickListener(listeners: HeaderClickListeners) {
        clickListeners.setListener(listeners)
    }

    override fun onAvatarClick(view: View) {
        if (::channel.isInitialized)
            ConversationInfoActivity.newInstance(context, channel)
    }

    override fun onToolbarClick(view: View) {
        if (::channel.isInitialized)
            ConversationInfoActivity.newInstance(context, channel)
    }

    override fun onBackClick(view: View) {
        context.asActivity().onBackPressed()
    }
}