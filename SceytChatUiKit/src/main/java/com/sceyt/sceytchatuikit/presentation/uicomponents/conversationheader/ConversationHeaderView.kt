package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.*
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytConversationHeaderViewBinding
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableFirstName
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners.HeaderEventsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners.HeaderEventsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.sceytconfigs.ConversationHeaderViewStyle
import com.sceyt.sceytchatuikit.shared.utils.BindingUtil
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil.setLastActiveDateByTime
import kotlinx.coroutines.*

class ConversationHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), HeaderClickListeners.ClickListeners,
        HeaderEventsListener.EventListeners, HeaderUIElementsListener.ElementsListeners {

    private val binding: SceytConversationHeaderViewBinding
    private var clickListeners = HeaderClickListenersImpl(this)
    private var eventListeners = HeaderEventsListenerImpl(this)
    private var uiElementsListeners = HeaderUIElementsListenerImpl(this)
    private lateinit var channel: SceytChannel
    private var replayMessage: SceytMessage? = null
    private val typingUsers by lazy { mutableSetOf<SceytMember>() }
    private var isTyping: Boolean = false
    private var isReplayInThread: Boolean = false
    private var updateTypingJob: Job? = null
    private var isGroup = false
    private lateinit var typingTextBuilder: (SceytMember) -> String

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

        typingTextBuilder = { member ->
            if (isGroup)
                buildString {
                    append(member.getPresentableFirstName().take(10))
                    append(" ${getString(R.string.sceyt_typing)}")
                }
            else getString(R.string.sceyt_typing)
        }
    }

    private fun SceytConversationHeaderViewBinding.setUpStyle() {
        icBack.setImageResource(ConversationHeaderViewStyle.backIcon)
        title.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.titleColor))
        subTitle.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.subTitleColor))
    }

    private fun setChannelTitle(titleTextView: TextView, channel: SceytChannel, replayMessage: SceytMessage? = null, replayInThread: Boolean = false) {
        if (replayInThread) {
            with(titleTextView) {
                text = getString(R.string.sceyt_thread_replay)
                (layoutParams as MarginLayoutParams).setMargins(binding.avatar.marginLeft, marginTop, marginRight, marginBottom)
            }
        } else
            titleTextView.text = channel.channelSubject
    }

    private fun setChannelSubTitle(subjectTextView: TextView, channel: SceytChannel, replayMessage: SceytMessage? = null, replayInThread: Boolean = false) {
        if (!replayInThread) {
            val title = if (channel is SceytDirectChannel) {
                val member = channel.peer ?: return
                if (member.user.presence?.state == PresenceState.Online) {
                    getString(R.string.sceyt_online)
                } else {
                    if (member.user.presence?.lastActiveAt != 0L)
                        setLastActiveDateByTime(member.user.presence.lastActiveAt)
                    else null
                }
            } else getString(R.string.sceyt_members_count, (channel as SceytGroupChannel).memberCount)

            subjectTextView.text = title
            subjectTextView.isVisible = !title.isNullOrBlank() && !isTyping
        } else {
            val fullName = replayMessage?.from?.fullName
            val subTitleText = String.format(getString(R.string.sceyt_with), fullName)
            subjectTextView.text = subTitleText
            subjectTextView.isVisible = !fullName.isNullOrBlank() && !isTyping
        }
    }

    private fun setAvatar(avatar: SceytAvatarView, channel: SceytChannel, replayInThread: Boolean = false) {
        binding.avatar.isVisible = !replayInThread
        if (!replayInThread) {
            val subjAndSUrl = channel.getSubjectAndAvatarUrl()
            avatar.setNameAndImageUrl(subjAndSUrl.first, subjAndSUrl.second)
        }
    }

    internal fun setChannel(channel: SceytChannel) {
        this.channel = channel
        isGroup = channel.isGroup

        with(binding) {
            uiElementsListeners.onAvatar(avatar, channel, false)
            uiElementsListeners.onTitle(title, channel, null, false)
            uiElementsListeners.onSubject(subTitle, channel, null, false)
        }
    }

    internal fun setReplayMessage(channel: SceytChannel, message: SceytMessage?) {
        this.channel = channel
        replayMessage = message
        isGroup = channel.isGroup
        isReplayInThread = true

        with(binding) {
            uiElementsListeners.onAvatar(avatar, channel, true)
            uiElementsListeners.onTitle(title, channel, message, true)
            uiElementsListeners.onSubject(subTitle, channel, message, true)
        }
    }

    private fun updateTypingText() {
        when {
            typingUsers.isEmpty() -> {
                updateTypingJob?.cancel()
            }
            typingUsers.size == 1 -> {
                binding.tvTyping.text = typingTextBuilder(typingUsers.last())
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
                    binding.tvTyping.text = typingTextBuilder(it)
                    delay(2000)
                }
            }
        }
    }

    private fun setTyping(data: ChannelTypingEventData) {
        if (data.member.id == ChatClient.getClient().user?.id) return
        val typing = data.typing
        isTyping = typing

        if (isGroup) {
            if (typing) {
                typingUsers.add(data.member)
            } else
                typingUsers.remove(data.member)

            updateTypingText()
        } else
            binding.tvTyping.text = /*initTypingTitle(data.member)*/typingTextBuilder.invoke(data.member)

        setTypingState(typing)
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

    private fun setPresenceUpdated(users: List<User>) {
        if (::channel.isInitialized.not() || channel.isGroup) return
        (channel as? SceytDirectChannel)?.let { directChannel ->
            val peer = directChannel.peer
            if (users.contains(peer?.user)) {
                users.find { user -> user.id == peer?.id }?.let {
                    directChannel.peer?.user = it
                    if (!isTyping)
                        uiElementsListeners.onSubject(binding.subTitle, channel, replayMessage, isReplayInThread)
                }
            }
        }
    }

    internal fun onTyping(data: ChannelTypingEventData) {
        eventListeners.onTypingEvent(data)
    }

    internal fun onPresenceUpdate(users: List<User>) {
        eventListeners.onPresenceUpdateEvent(users)
    }

    fun isTyping() = isTyping

    fun isGroup() = isGroup

    fun isReplayInThread() = isReplayInThread

    fun getChannel() = if (::channel.isInitialized) channel else null

    fun getReplayMessage() = replayMessage

    fun setCustomClickListener(headerClickListenersImpl: HeaderClickListenersImpl) {
        clickListeners = headerClickListenersImpl
    }

    fun setClickListener(listeners: HeaderClickListeners) {
        clickListeners.setListener(listeners)
    }

    fun setEventListener(listener: HeaderEventsListener) {
        eventListeners.setListener(listener)
    }

    fun setCustomEventListener(listener: HeaderEventsListenerImpl) {
        eventListeners = listener
    }

    fun setUiElementsListener(listener: HeaderUIElementsListener) {
        uiElementsListeners.setListener(listener)
    }

    fun setCustomUiElementsListener(listener: HeaderUIElementsListenerImpl) {
        uiElementsListeners = listener
    }

    fun setTypingTextBuilder(builder: (SceytMember) -> String) {
        typingTextBuilder = builder
    }

    //Event listeners
    override fun onTypingEvent(data: ChannelTypingEventData) {
        setTyping(data)
    }

    override fun onPresenceUpdateEvent(users: List<User>) {
        setPresenceUpdated(users)
    }

    //Ui elements listeners
    override fun onTitle(titleTextView: TextView, channel: SceytChannel, replayMessage: SceytMessage?, replayInThread: Boolean) {
        setChannelTitle(titleTextView, channel, replayMessage, replayInThread)
    }

    override fun onSubject(subjectTextView: TextView, channel: SceytChannel, replayMessage: SceytMessage?, replayInThread: Boolean) {
        post { setChannelSubTitle(subjectTextView, channel, replayMessage, replayInThread) }
    }

    override fun onAvatar(avatar: SceytAvatarView, channel: SceytChannel, replayInThread: Boolean) {
        setAvatar(avatar, channel, replayInThread)
    }

    //Click listeners
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