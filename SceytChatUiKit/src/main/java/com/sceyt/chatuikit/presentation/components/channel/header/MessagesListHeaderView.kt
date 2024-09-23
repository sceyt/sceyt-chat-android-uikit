package com.sceyt.chatuikit.presentation.components.channel.header

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.copy
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytConversationHeaderViewBinding
import com.sceyt.chatuikit.extensions.asActivity
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.hideKeyboard
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.maybeComponentActivity
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.setHintColorRes
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.extensions.showSoftInput
import com.sceyt.chatuikit.formatters.UserNameFormatter
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.HeaderTypingUsersHelper
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.click.HeaderClickListeners
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.click.HeaderClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.HeaderEventsListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.HeaderEventsListenerImpl
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui.HeaderUIElementsListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui.HeaderUIElementsListenerImpl
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.customviews.AvatarView
import com.sceyt.chatuikit.presentation.extensions.setChannelAvatar
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import com.sceyt.chatuikit.styles.MessagesListHeaderStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

@Suppress("MemberVisibilityCanBePrivate")
class MessagesListHeaderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppBarLayout(context, attrs, defStyleAttr), HeaderClickListeners.ClickListeners,
        HeaderEventsListener.EventListeners, HeaderUIElementsListener.ElementsListeners {

    private val binding: SceytConversationHeaderViewBinding
    private val style: MessagesListHeaderStyle
    private var clickListeners = HeaderClickListenersImpl(this)
    private var eventListeners = HeaderEventsListenerImpl(this)
    internal var uiElementsListeners = HeaderUIElementsListenerImpl(this)
    private lateinit var channel: SceytChannel
    private var replyMessage: SceytMessage? = null
    private var isReplyInThread: Boolean = false
    private var isGroup = false
    private var userNameFormatter: UserNameFormatter? = SceytChatUIKit.formatters.userNameFormatter
    private var enablePresence: Boolean = true
    private val typingUsersHelper by lazy { initTypingUsersHelper() }
    private var toolbarActionsHiddenCallback: (() -> Unit)? = null
    private var toolbarSearchModeChangeListener: ((Boolean) -> Unit)? = null
    private var addedMenu: Menu? = null
    private var onSearchQueryChangeListener: ((String) -> Unit)? = null
    var isShowingMessageActions = false
        private set
    var isShowingSearchBar = false
        private set

    init {
        binding = SceytConversationHeaderViewBinding.inflate(LayoutInflater.from(context), this)
        style = MessagesListHeaderStyle.Builder(context, attrs).build()
        init()
    }

    private fun init() {
        stateListAnimator = null
        with(binding) {
            applyStyle()
            layoutToolbarRoot.layoutTransition = LayoutTransition().apply {
                disableTransitionType(LayoutTransition.DISAPPEARING)
                setDuration(200)
            }

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

            icClear.setOnClickListener {
                inputSearch.text?.clear()
            }

            inputSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                    inputSearch.text?.let { text ->
                        onSearchQueryChangeListener?.invoke(text.toString())
                    }
                return@setOnEditorActionListener false
            }

            inputSearch.doAfterTextChanged {
                binding.icClear.isVisible = it?.isNotEmpty() == true
            }
        }

        if (!isInEditMode) {
            updatePresenceEveryOneMin()
            initTypingUsersHelper()
        }
    }

    private fun updatePresenceEveryOneMin() {
        context.asComponentActivity().lifecycleScope.launch {
            while (isActive) {
                delay(1000 * 60)
                uiElementsListeners.onSubTitle(binding.subTitle, channel, replyMessage, isReplyInThread)
            }
        }
    }

    private fun SceytConversationHeaderViewBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        toolbarMessageActions.popupTheme = style.menuStyle
        toolbarMessageActions.setTitleTextAppearance(context, style.menuTitleAppearance)
        icBack.setImageDrawable(style.navigationIcon)
        title.setTextColor(style.titleColor)
        subTitle.setTextColor(style.subTitleColor)
        toolbarUnderline.background = ColorDrawable(style.underlineColor)
        toolbarUnderline.isVisible = style.showUnderline
        layoutSearch.setBackgroundTintColorRes(SceytChatUIKit.theme.surface1Color)
        inputSearch.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        inputSearch.setHintColorRes(SceytChatUIKit.theme.textFootnoteColor)
        icSearch.setTintColorRes(SceytChatUIKit.theme.accentColor)
        icBack.setTintColorRes(SceytChatUIKit.theme.accentColor)
        icClear.setTintColorRes(SceytChatUIKit.theme.iconSecondaryColor)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun setChannelTitle(titleTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage? = null, replyInThread: Boolean = false) {
        if (replyInThread) {
            with(titleTextView) {
                text = getString(R.string.sceyt_thread_reply)
                (layoutParams as MarginLayoutParams).setMargins(binding.avatar.marginLeft, marginTop, marginRight, marginBottom)
            }
        } else {
            val title = when {
                isGroup -> channel.channelSubject
                channel.isSelf() -> getString(R.string.sceyt_self_notes)
                else -> {
                    val member = channel.getPeer() ?: return
                    userNameFormatter?.format(member.user)
                            ?: member.user.getPresentableNameCheckDeleted(context)
                }
            }
            if (titleTextView.text.equals(title)) return
            titleTextView.text = title
        }
    }

    private fun setChannelSubTitle(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage? = null, replyInThread: Boolean = false) {
        if (enablePresence.not() || channel.isPeerDeleted() || channel.isSelf()) {
            subjectTextView.isVisible = false
            return
        }
        post {
            if (!replyInThread) {
                val title = when (channel.getChannelType()) {
                    ChannelTypeEnum.Direct -> {
                        val member = channel.getPeer() ?: return@post
                        if (member.user.blocked) {
                            ""
                        } else {
                            if (member.user.presence?.state == PresenceState.Online) {
                                getString(R.string.sceyt_online)
                            } else {
                                member.user.presence?.lastActiveAt?.let {
                                    if (it != 0L) {
                                        DateTimeUtil.getPresenceDateFormatData(context, Date(it))
                                    } else null
                                }
                            }
                        }
                    }

                    ChannelTypeEnum.Group -> {
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
                setSubTitleText(subjectTextView, title, !title.isNullOrBlank() && !typingUsersHelper.isTyping)
            } else {
                val fullName = replyMessage?.user?.fullName
                val subTitleText = String.format(getString(R.string.sceyt_with), fullName)
                setSubTitleText(subjectTextView, subTitleText, !fullName.isNullOrBlank() && !typingUsersHelper.isTyping)
            }
        }
    }

    private fun setSubTitleText(textView: TextView, title: String?, visible: Boolean) {
        if (!visible) {
            textView.isVisible = false
            return
        }
        if (textView.text.equals(title) && textView.isVisible)
            return

        textView.text = title
        textView.isVisible = !typingUsersHelper.isTyping
    }

    private fun setAvatar(avatar: AvatarView, channel: SceytChannel, replyInThread: Boolean = false) {
        binding.avatar.isVisible = !replyInThread
        if (!replyInThread)
            avatar.setChannelAvatar(channel)
    }

    private fun showMessageActionsInToolbar(vararg messages: SceytMessage, @MenuRes resId: Int,
                                            listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?): Menu? {
        val menu: Menu?
        with(binding) {
            toolbarMessageActions.setToolbarIconsVisibilityInitializer { messages, menu ->
                uiElementsListeners.onInitToolbarActionsMenu(*messages, menu = menu)
            }
            menu = toolbarMessageActions.setupMenuWithMessages(resId, *messages)
            toolbarMessageActions.isVisible = true
            layoutToolbarDetails.isVisible = false
            isShowingMessageActions = true
            addedMenu?.forEach { it.isVisible = false }

            toolbarMessageActions.setMenuItemClickListener {
                listener?.invoke(it) {
                    binding.hideMessageActions()
                    toolbarActionsHiddenCallback?.invoke()
                }
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

    private fun initTypingUsersHelper(): HeaderTypingUsersHelper {
        return HeaderTypingUsersHelper(context, isGroup, typingTextUpdatedListener = {
            binding.tvTyping.text = it
        }, typingStateUpdated = {
            setTypingState(it)
        })
    }

    private fun setTypingState(typing: Boolean) {
        if ((typing && isGroup.not()) || (isGroup && typingUsersHelper.typingUsers.isNotEmpty())) {
            binding.subTitle.isVisible = false
            binding.groupTyping.isVisible = true
        } else {
            binding.groupTyping.isVisible = false
            binding.subTitle.isVisible = true
        }
    }

    private fun setPresenceUpdated(user: User) {
        if (::channel.isInitialized.not() || channel.isGroup || enablePresence.not()) return
        channel.getPeer()?.let { member ->
            if (member.user.id == user.id) {
                channel = channel.copy(members = channel.members?.map {
                    if (it.user.id == user.id) it.copy(user = user.copy()) else it
                })
                if (!typingUsersHelper.isTyping)
                    uiElementsListeners.onSubTitle(binding.subTitle, channel, replyMessage, isReplyInThread)
            }
        }
    }

    private fun SceytConversationHeaderViewBinding.hideMessageActions() {
        toolbarMessageActions.isVisible = false
        layoutToolbarDetails.isVisible = true
        isShowingMessageActions = false
        addedMenu?.forEach { item -> item.isVisible = true }
    }

    private fun SceytConversationHeaderViewBinding.toggleSearch(showSearch: Boolean) {
        hideMessageActions()
        layoutSearch.isVisible = showSearch
        layoutToolbarDetails.isVisible = !showSearch
        root.setBackgroundColor(if (showSearch) context.getCompatColor(R.color.sceyt_color_background)
        else context.getCompatColor(R.color.sceyt_color_primary))
        isShowingSearchBar = showSearch
        if (showSearch)
            context.showSoftInput(inputSearch)
        else {
            inputSearch.text?.clear()
            context.hideKeyboard(inputSearch)
        }
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

    internal fun setSearchModeChangeListener(listener: (Boolean) -> Unit) {
        toolbarSearchModeChangeListener = listener
    }

    fun isTyping() = typingUsersHelper.isTyping

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

    fun setSearchQueryChangeListener(listener: (String) -> Unit) {
        onSearchQueryChangeListener = listener
    }

    fun setTypingTextBuilder(builder: (SceytMember) -> String) {
        typingUsersHelper.setTypingTextBuilder(builder)
    }

    fun setUserNameFormatter(formatter: UserNameFormatter) {
        userNameFormatter = formatter
        typingUsersHelper.setUserNameFormatter(formatter)
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
            addedMenu = menu
        }
    }

    fun getToolbarMenu(): Menu = binding.headerToolbar.menu

    fun enableDisableToShowPresence(enable: Boolean) {
        enablePresence = enable
    }

    private val channelInfoLauncher = if (isInEditMode) null else context.maybeComponentActivity()?.run {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            registerForActivityResult(StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.getBooleanExtra(ChannelInfoActivity.ACTION_SEARCH_MESSAGES, false)?.let { search ->
                        if (search)
                            showSearchMessagesBar(MessageCommandEvent.SearchMessages(true))
                    }
                }
            }
        } else null
    }

    //Event listeners
    override fun onTypingEvent(data: ChannelTypingEventData) {
        typingUsersHelper.onTypingEvent(data)
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

    override fun onAvatar(avatar: AvatarView, channel: SceytChannel, replyInThread: Boolean) {
        setAvatar(avatar, channel, replyInThread)
    }

    override fun onShowMessageActionsMenu(vararg messages: SceytMessage, @MenuRes menuResId: Int,
                                          listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?): Menu? {
        return showMessageActionsInToolbar(*messages, resId = menuResId, listener = listener)
    }

    override fun onHideMessageActionsMenu() {
        binding.hideMessageActions()
    }

    override fun onInitToolbarActionsMenu(vararg messages: SceytMessage, menu: Menu) {
        val isSingleMessage = messages.size == 1
        val newSelectedMessage = messages.getOrNull(0)

        newSelectedMessage?.let { message ->
            val isPending = message.deliveryStatus == DeliveryStatus.Pending
            menu.findItem(R.id.sceyt_reply).isVisible = isSingleMessage && !isPending
            //menu.findItem(R.id.sceyt_reply_in_thread).isVisible = isSingleMessage && !isPending
            menu.findItem(R.id.sceyt_forward).isVisible = !isPending
            val expiredEditMessage = (System.currentTimeMillis() - message.createdAt) >
                    SceytChatUIKit.config.messageEditTimeout
            menu.findItem(R.id.sceyt_edit_message).isVisible = isSingleMessage &&
                    !message.incoming && message.body.isNotNullOrBlank() && !expiredEditMessage
            menu.findItem(R.id.sceyt_message_info).isVisible = isSingleMessage && !message.incoming && !isPending
            menu.findItem(R.id.sceyt_copy_message).isVisible = messages.any { it.body.isNotNullOrBlank() }
        }
    }

    override fun showSearchMessagesBar(event: MessageCommandEvent.SearchMessages) {
        binding.toggleSearch(true)
        toolbarSearchModeChangeListener?.invoke(true)
    }

    //Click listeners
    override fun onAvatarClick(view: View) {
        if (::channel.isInitialized)
            channelInfoLauncher?.let { ChannelInfoActivity.startHandleSearchClick(context, channel, it) }
    }

    override fun onToolbarClick(view: View) {
        if (::channel.isInitialized)
            channelInfoLauncher?.let { ChannelInfoActivity.startHandleSearchClick(context, channel, it) }
    }

    override fun onBackClick(view: View) {
        when {
            isShowingMessageActions -> {
                binding.hideMessageActions()
                toolbarActionsHiddenCallback?.invoke()
            }

            isShowingSearchBar -> {
                binding.toggleSearch(false)
                toolbarSearchModeChangeListener?.invoke(false)
            }

            else -> context.maybeComponentActivity()?.onBackPressedDispatcher?.onBackPressed()
                    ?: context.asActivity().finish()
        }
    }
}