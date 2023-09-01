package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytConversationHeaderViewBinding
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableFirstName
import com.sceyt.sceytchatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.extensions.maybeComponentActivity
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners.HeaderEventsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners.HeaderEventsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.sceytconfigs.ConversationHeaderViewStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle
import com.sceyt.sceytchatuikit.shared.utils.BindingUtil
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

class ConversationHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), HeaderClickListeners.ClickListeners,
        HeaderEventsListener.EventListeners, HeaderUIElementsListener.ElementsListeners {

    private val binding: SceytConversationHeaderViewBinding
    private var clickListeners = HeaderClickListenersImpl(this)
    private var eventListeners = HeaderEventsListenerImpl(this)
    internal var uiElementsListeners = HeaderUIElementsListenerImpl(this)
    private lateinit var channel: SceytChannel
    private var replyMessage: SceytMessage? = null
    private val typingUsers by lazy { mutableSetOf<SceytMember>() }
    private var isTyping: Boolean = false
    private var isReplyInThread: Boolean = false
    private var updateTypingJob: Job? = null
    private var isGroup = false
    private var typingTextBuilder: ((SceytMember) -> String)? = null
    private var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
    private val debounceHelper by lazy { DebounceHelper(200, context.asComponentActivity().lifecycleScope) }
    private val typingCancelHelper by lazy { TypingCancelHelper() }
    private var enablePresence: Boolean = true
    private var isShowingMessageActions = false
    private var toolbarActionsHiddenCallback: (() -> Unit)? = null

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
        with(binding) {
            setUpStyle()
            layoutToolbarRoot.layoutTransition?.setDuration(200)

            post { subTitle.isSelected = true }

            icBack.setOnClickListener {
                clickListeners.onBackClick(it)
            }

            avatar.setOnClickListener {
                clickListeners.onAvatarClick(it)
            }

            layoutToolbarDetails.setOnClickListener {
                clickListeners.onToolbarClick(it)
            }
        }

        if (!isInEditMode)
            updatePresenceEveryOneMin()
    }

    private fun updatePresenceEveryOneMin() {
        context.asComponentActivity().lifecycleScope.launch {
            while (isActive) {
                delay(1000 * 60)
                uiElementsListeners.onSubTitle(binding.subTitle, channel, replyMessage, isReplyInThread)
            }
        }
    }

    private fun SceytConversationHeaderViewBinding.setUpStyle() {
        icBack.setImageResource(ConversationHeaderViewStyle.backIcon)
        title.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.titleColor))
        subTitle.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.subTitleColor))
        toolbarUnderline.background = ColorDrawable(context.getCompatColor(ConversationHeaderViewStyle.underlineColor))
    }

    private fun setChannelTitle(titleTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage? = null, replyInThread: Boolean = false) {
        if (replyInThread) {
            with(titleTextView) {
                text = getString(R.string.sceyt_thread_reply)
                (layoutParams as MarginLayoutParams).setMargins(binding.avatar.marginLeft, marginTop, marginRight, marginBottom)
            }
        } else {
            val title = if (isGroup) channel.channelSubject else {
                val member = channel.getFirstMember() ?: return
                userNameBuilder?.invoke(member.user)
                        ?: member.user.getPresentableNameCheckDeleted(context)
            }
            if (titleTextView.text.equals(title)) return
            titleTextView.text = title
        }
    }

    private fun setChannelSubTitle(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage? = null, replyInThread: Boolean = false) {
        if (enablePresence.not() || channel.isPeerDeleted()) {
            subjectTextView.isVisible = false
            return
        }
        post {
            if (!replyInThread) {
                val title = when (channel.getChannelType()) {
                    ChannelTypeEnum.Direct -> {
                        val member = channel.getFirstMember() ?: return@post
                        if (member.user.presence?.state == PresenceState.Online) {
                            getString(R.string.sceyt_online)
                        } else {
                            member.user.presence?.lastActiveAt?.let {
                                if (it != 0L) {
                                    val text = DateTimeUtil.getPresenceDateFormatData(context, Date(it))
                                    if (subjectTextView.text.equals(text)) return@post
                                    else text
                                } else null
                            }
                        }
                    }

                    ChannelTypeEnum.Private -> {
                        val memberCount = channel.memberCount
                        if (memberCount > 1)
                            getString(R.string.sceyt_members_count, memberCount)
                        else getString(R.string.sceyt_member_count, memberCount)
                    }

                    ChannelTypeEnum.Public -> {
                        val memberCount = channel.memberCount
                        if (memberCount > 1)
                            getString(R.string.sceyt_subscribers_count, memberCount)
                        else getString(R.string.sceyt_subscriber_count, memberCount)
                    }
                }
                setSubTitleText(subjectTextView, title, !title.isNullOrBlank() && !isTyping)
            } else {
                val fullName = replyMessage?.user?.fullName
                val subTitleText = String.format(getString(R.string.sceyt_with), fullName)
                setSubTitleText(subjectTextView, subTitleText, !fullName.isNullOrBlank() && !isTyping)
            }
        }
    }

    private fun setSubTitleText(textView: TextView, title: String?, visible: Boolean) {
        if (!visible) {
            textView.isVisible = false
            return
        }
        if (textView.text.equals(title))
            return

        textView.text = title
        textView.isVisible = true
    }

    private fun setAvatar(avatar: SceytAvatarView, channel: SceytChannel, replyInThread: Boolean = false) {
        binding.avatar.isVisible = !replyInThread
        if (!replyInThread) {
            if (channel.isPeerDeleted())
                avatar.setImageUrl(null, UserStyle.deletedUserAvatar)
            else {
                val subjAndSUrl = channel.getSubjectAndAvatarUrl()
                avatar.setNameAndImageUrl(subjAndSUrl.first, subjAndSUrl.second, if (isGroup) 0 else UserStyle.userDefaultAvatar)
            }
        }
    }

    private fun showMessageActionsInToolbar(vararg messages: SceytMessage, @MenuRes resId: Int,
                                            listener: ((MenuItem) -> Unit)?): Menu? {
        val menu: Menu?
        with(binding) {
            menu = toolBarMessageActions.setupMenuWithMessages(resId, *messages)
            toolBarMessageActions.isVisible = true
            layoutToolbarDetails.isVisible = false
            isShowingMessageActions = true
            toolBarMessageActions.setMenuItemClickListener {
                listener?.invoke(it)
                hideMessageActions()
                toolbarActionsHiddenCallback?.invoke()
            }
        }
        return menu
    }

    internal fun setChannel(channel: SceytChannel) {
        this.channel = channel
        isGroup = channel.isGroup

        with(binding) {
            uiElementsListeners.onTitle(title, channel, null, false)
            uiElementsListeners.onSubTitle(subTitle, channel, null, false)
            uiElementsListeners.onAvatar(avatar, channel, false)
        }
    }

    internal fun setReplyMessage(channel: SceytChannel, message: SceytMessage?) {
        this.channel = channel
        replyMessage = message
        isGroup = channel.isGroup
        isReplyInThread = true

        with(binding) {
            uiElementsListeners.onTitle(title, channel, message, true)
            uiElementsListeners.onSubTitle(subTitle, channel, message, true)
            uiElementsListeners.onAvatar(avatar, channel, true)
        }
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
                if (updateTypingJob == null || updateTypingJob?.isActive?.not() == true)
                    updateTypingTitleEveryTwoSecond()
            }
        }
    }

    private fun updateTypingTitleEveryTwoSecond() {
        updateTypingJob?.cancel()
        updateTypingJob = context.asComponentActivity().lifecycleScope.launch {
            while (true) {
                typingUsers.toList().forEach {
                    binding.tvTyping.text = initTypingTitle(it)
                    delay(2000)
                }
            }
        }
    }

    private fun initTypingTitle(member: SceytMember): String {
        return typingTextBuilder?.invoke(member) ?: if (isGroup)
            buildString {
                append(userNameBuilder?.invoke(member.user)
                        ?: member.getPresentableFirstName().take(10))
                append(" ${getString(R.string.sceyt_typing)}")
            }
        else getString(R.string.sceyt_typing)
    }

    private fun setTyping(data: ChannelTypingEventData) {
        if (data.member.id == ChatClient.getClient().user?.id) return
        debounceHelper.submit {
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

    private fun setPresenceUpdated(user: User) {
        if (::channel.isInitialized.not() || channel.isGroup || enablePresence.not()) return
        channel.getFirstMember()?.let { member ->
            if (member.user.id == user.id) {
                member.user = user
                if (!isTyping)
                    uiElementsListeners.onSubTitle(binding.subTitle, channel, replyMessage, isReplyInThread)
            }
        }
    }

    private fun hideMessageActions() {
        binding.toolBarMessageActions.isVisible = false
        binding.layoutToolbarDetails.isVisible = true
        isShowingMessageActions = false
    }

    internal fun onTyping(data: ChannelTypingEventData) {
        eventListeners.onTypingEvent(data)
    }

    internal fun onPresenceUpdate(user: User) {
        eventListeners.onPresenceUpdateEvent(user)
    }

    internal fun setToolbarActionHiddenCallback(callback: () -> Unit) {
        toolbarActionsHiddenCallback = callback
    }

    fun isTyping() = isTyping

    fun getChannel() = if (::channel.isInitialized) channel else null

    fun getReplyMessage() = replyMessage

    fun setCustomClickListener(listeners: HeaderClickListenersImpl) {
        clickListeners = listeners
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

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    fun invalidateUi() {
        with(binding) {
            uiElementsListeners.onTitle(title, channel, replyMessage, isReplyInThread)
            uiElementsListeners.onSubTitle(subTitle, channel, replyMessage, isReplyInThread)
            uiElementsListeners.onAvatar(avatar, channel, isReplyInThread)
        }
    }

    fun setToolbarMenu(@MenuRes resId: Int, listener: Toolbar.OnMenuItemClickListener) {
        with(binding.headerToolbar) {
            inflateMenu(resId)
            setOnMenuItemClickListener(listener)
        }
    }

    fun enableDisableToShowPresence(enable: Boolean) {
        enablePresence = enable
    }

    //Event listeners
    override fun onTypingEvent(data: ChannelTypingEventData) {
        typingCancelHelper.await(data) {
            setTyping(it)
        }
        setTyping(data)
    }

    override fun onPresenceUpdateEvent(user: User) {
        setPresenceUpdated(user)
    }

    //Ui elements listeners
    override fun onTitle(titleTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
        setChannelTitle(titleTextView, channel, replyMessage, replyInThread)
    }

    override fun onSubTitle(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
        setChannelSubTitle(subjectTextView, channel, replyMessage, replyInThread)
    }

    override fun onAvatar(avatar: SceytAvatarView, channel: SceytChannel, replyInThread: Boolean) {
        setAvatar(avatar, channel, replyInThread)
    }

    override fun onShowMessageActionsMenu(vararg messages: SceytMessage, @MenuRes menuResId: Int,
                                          listener: ((MenuItem) -> Unit)?): Menu? {
        return showMessageActionsInToolbar(*messages, resId = menuResId, listener = listener)
    }

    override fun onHideMessageActionsMenu() {
        hideMessageActions()
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
        if (isShowingMessageActions) {
            hideMessageActions()
            toolbarActionsHiddenCallback?.invoke()
        } else
            context.maybeComponentActivity()?.onBackPressedDispatcher?.onBackPressed()
    }
}