package com.sceyt.chatuikit.presentation.components.channel.header

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
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
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytMessagesListHeaderViewBinding
import com.sceyt.chatuikit.extensions.asActivity
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.hideKeyboard
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.maybeComponentActivity
import com.sceyt.chatuikit.extensions.showSoftInput
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.ActiveUser
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.ActivityState
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.HeaderUserActivityChangeHelper
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.click.MessageListHeaderClickListeners
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.click.MessageListHeaderClickListeners.ClickListeners
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.click.MessageListHeaderClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.click.setListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.MessageListHeaderEventsListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.MessageListHeaderEventsListener.EventListeners
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.MessageListHeaderEventsListenerImpl
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.setListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui.MessageListHeaderUIElementsListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui.MessageListHeaderUIElementsListener.ElementsListeners
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui.MessageListHeaderUIElementsListenerImpl
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.ui.setListener
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.MessagesListHeaderStyle
import com.sceyt.chatuikit.styles.common.MenuStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Suppress("MemberVisibilityCanBePrivate", "JoinDeclarationAndAssignment")
class MessagesListHeaderView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : AppBarLayout(context, attrs, defStyleAttr), ClickListeners,
        EventListeners, ElementsListeners {

    private val binding: SceytMessagesListHeaderViewBinding
    private var clickListeners: ClickListeners = MessageListHeaderClickListenersImpl(this)
    private var eventListeners: EventListeners = MessageListHeaderEventsListenerImpl(this)
    internal var uiElementsListeners: ElementsListeners = MessageListHeaderUIElementsListenerImpl(this)
    private lateinit var channel: SceytChannel
    private var replyMessage: SceytMessage? = null
    private var isReplyInThread: Boolean = false
    private var isGroup = false
    private var enablePresence: Boolean = true
    private var activityChangeHelper: HeaderUserActivityChangeHelper? = null
    private var toolbarActionsHiddenCallback: (() -> Unit)? = null
    private var toolbarSearchModeChangeListener: ((Boolean) -> Unit)? = null
    private var addedMenu: Menu? = null
    private var onSearchQueryChangeListener: ((String) -> Unit)? = null
    val style: MessagesListHeaderStyle
    var isShowingMessageActions = false
        private set
    var isShowingSearchBar = false
        private set

    init {
        binding = SceytMessagesListHeaderViewBinding.inflate(LayoutInflater.from(context), this)
        style = MessagesListHeaderStyle.Builder(context, attrs).build()
        init()
    }

    private fun init() {
        stateListAnimator = null
        with(binding) {
            applyStyle()
            val animation = LayoutTransition().apply {
                disableTransitionType(LayoutTransition.DISAPPEARING)
                setDuration(200)
            }
            layoutToolbarRoot.layoutTransition = animation
            layoutSubTitle.layoutTransition = animation

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

    @Suppress("unused")
    private fun setChannelTitle(
            titleTextView: TextView,
            channel: SceytChannel,
            replyMessage: SceytMessage? = null,
            replyInThread: Boolean = false
    ) {
        if (replyInThread) {
            with(titleTextView) {
                text = getString(R.string.sceyt_thread_reply)
                (layoutParams as? MarginLayoutParams)?.marginStart = binding.avatar.marginLeft
            }
        } else {
            val title = style.titleFormatter.format(context, channel)
            if (titleTextView.text.equals(title)) return
            titleTextView.text = title
        }
    }

    private fun setChannelSubTitle(
            subjectTextView: TextView,
            channel: SceytChannel,
            replyMessage: SceytMessage? = null,
            replyInThread: Boolean = false
    ) {
        if (enablePresence.not() || channel.isPeerDeleted() || channel.isSelf) {
            subjectTextView.isVisible = false
            return
        }
        post {
            if (!ConnectionEventManager.isConnected) return@post
            if (!replyInThread) {
                val title = style.subtitleFormatter.format(context, channel)
                setSubTitleText(subjectTextView, title, title.isNotBlank() && !haveUserAction)
            } else {
                val fullName = replyMessage?.user?.fullName
                val subTitleText = String.format(getString(R.string.sceyt_with), fullName)
                setSubTitleText(subjectTextView, subTitleText, !fullName.isNullOrBlank() && !haveUserAction)
            }
        }
    }

    private fun setSubTitleText(textView: TextView, title: CharSequence, visible: Boolean) {
        if (!visible) {
            textView.isVisible = false
            return
        }
        if (textView.text.equals(title) && textView.isVisible)
            return

        textView.text = title
        textView.isVisible = !haveUserAction
    }

    private fun setAvatar(avatar: AvatarView, channel: SceytChannel, replyInThread: Boolean = false) {
        binding.avatar.isVisible = !replyInThread
        if (!replyInThread)
            style.channelAvatarRenderer.render(context, channel, style.avatarStyle, avatar)
    }

    private fun showMessageActionsInToolbar(
            vararg messages: SceytMessage,
            menuStyle: MenuStyle,
            listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?,
    ) {
        with(binding) {
            toolbarMessageActions.setToolbarIconsVisibilityInitializer { messages, menu ->
                uiElementsListeners.onInitToolbarActionsMenu(*messages, menu = menu)
            }
            toolbarMessageActions.setupMenuWithMessages(menuStyle, *messages)
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
    }

    internal fun setChannel(channel: SceytChannel) {
        this.channel = channel
        isGroup = channel.isGroup
        if (activityChangeHelper == null)
            activityChangeHelper = initUserActivityChangeHelper(channel)

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

    private fun initUserActivityChangeHelper(channel: SceytChannel): HeaderUserActivityChangeHelper {
        return HeaderUserActivityChangeHelper(context,
            channel = channel,
            userActivityTitleFormatter = style.typingTitleFormatter,
            userActivityTextUpdatedListener = {
                binding.tvUserActivity.text = it
            },
            activityStateUpdated = {
                setTypingState(it)
            },
            showActiveUsersInSequence = style.showTypingUsersInSequence
        )
    }

    private fun setTypingState(state: ActivityState) {
      /*  when(state){
            ActivityState.Typing -> {

            }
            ActivityState.Recording -> {
            }
            ActivityState.None -> TODO()
        }*/
        val active = state != ActivityState.None
        binding.subTitle.isVisible = !active
        binding.lottieUserActivity.isVisible = active && style.enableUserActivityIndicator
        binding.tvUserActivity.isVisible = active
    }

    private fun setPresenceUpdated(user: SceytUser) {
        if (::channel.isInitialized.not() || channel.isGroup || enablePresence.not()) return
        channel.getPeer()?.let { member ->
            if (member.user.id == user.id) {
                channel = channel.copy(members = channel.members?.map {
                    if (it.user.id == user.id) it.copy(user = user.copy()) else it
                })
                if (!haveUserAction)
                    uiElementsListeners.onSubTitle(binding.subTitle, channel, replyMessage, isReplyInThread)
            }
        }
    }

    private fun SceytMessagesListHeaderViewBinding.hideMessageActions() {
        toolbarMessageActions.isVisible = false
        layoutToolbarDetails.isVisible = true
        isShowingMessageActions = false
        addedMenu?.forEach { item -> item.isVisible = true }
    }

    private fun SceytMessagesListHeaderViewBinding.toggleSearch(showSearch: Boolean) {
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

    internal fun handleMemberActivityEvent(data: ChannelMemberActivityEvent) {
        eventListeners.onActivityEvent(data)
    }

    internal fun onPresenceUpdate(user: SceytUser) {
        eventListeners.onPresenceUpdateEvent(user)
    }

    internal fun onConnectionStateUpdate(state: ConnectionState) = post {
        if (state == ConnectionState.Connected) {
            val title = style.subtitleFormatter.format(context, channel)
            setSubTitleText(
                textView = binding.subTitle,
                title = title,
                visible = title.isNotBlank() && !haveUserAction
            )
            return@post
        }
        val title = SceytChatUIKit.formatters.connectionStateTitleFormatter.format(
            context = context,
            from = state
        )
        setSubTitleText(
            textView = binding.subTitle,
            title = title,
            visible = true
        )
    }

    internal fun setToolbarActionHiddenCallback(callback: () -> Unit) {
        toolbarActionsHiddenCallback = callback
    }

    internal fun setSearchModeChangeListener(listener: (Boolean) -> Unit) {
        toolbarSearchModeChangeListener = listener
    }

    val haveUserAction: Boolean
        get() = activityChangeHelper?.haveUserAction == true

    @Suppress("unused")
    val activeUsers: List<ActiveUser>
        get() = activityChangeHelper?.activeUsers.orEmpty()

    @Suppress("unused")
    fun getChannel() = if (::channel.isInitialized) channel else null

    @Suppress("unused")
    fun getReplyMessage() = replyMessage

    @Suppress("unused")
    fun setCustomClickListener(listeners: ClickListeners) {
        clickListeners = (listeners as? MessageListHeaderClickListenersImpl)
            ?.withDefaultListeners(this) ?: listeners
    }

    fun setClickListener(listeners: MessageListHeaderClickListeners) {
        clickListeners.setListener(listeners)
    }

    @Suppress("unused")
    fun setEventListener(listener: MessageListHeaderEventsListener) {
        eventListeners.setListener(listener)
    }

    @Suppress("unused")
    fun setCustomEventListener(listener: EventListeners) {
        eventListeners = (listener as? MessageListHeaderEventsListenerImpl)
            ?.withDefaultListeners(this) ?: listener
    }

    @Suppress("unused")
    fun setUiElementsListener(listener: MessageListHeaderUIElementsListener) {
        uiElementsListeners.setListener(listener)
    }

    @Suppress("unused")
    fun setCustomUiElementsListener(listener: ElementsListeners) {
        uiElementsListeners = (listener as? MessageListHeaderUIElementsListenerImpl)
            ?.withDefaultListeners(this) ?: listener
    }

    fun setSearchQueryChangeListener(listener: (String) -> Unit) {
        onSearchQueryChangeListener = listener
    }

    @Suppress("unused")
    fun invalidateUi() {
        with(binding) {
            uiElementsListeners.onTitle(title, channel, replyMessage, isReplyInThread)
            uiElementsListeners.onSubTitle(subTitle, channel, replyMessage, isReplyInThread)
            uiElementsListeners.onAvatar(avatar, channel, isReplyInThread)
        }
    }

    @Suppress("unused")
    fun setToolbarMenu(@MenuRes resId: Int, listener: Toolbar.OnMenuItemClickListener) {
        with(binding.headerToolbar) {
            inflateMenu(resId)
            setOnMenuItemClickListener(listener)
            addedMenu = menu
        }
    }

    @Suppress("unused")
    fun getToolbarMenu(): Menu = binding.headerToolbar.menu

    @Suppress("unused")
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
    override fun onActivityEvent(event: ChannelMemberActivityEvent) {
        activityChangeHelper?.onActivityEvent(event)
    }

    override fun onPresenceUpdateEvent(user: SceytUser) {
        setPresenceUpdated(user)
    }

    //Ui elements listeners
    override fun onTitle(
            titleTextView: TextView,
            channel: SceytChannel,
            replyMessage: SceytMessage?,
            replyInThread: Boolean
    ) {
        setChannelTitle(titleTextView, channel, replyMessage, replyInThread)
    }

    override fun onSubTitle(
            subjectTextView: TextView,
            channel: SceytChannel,
            replyMessage: SceytMessage?,
            replyInThread: Boolean
    ) {
        setChannelSubTitle(subjectTextView, channel, replyMessage, replyInThread)
    }

    override fun onAvatar(avatar: AvatarView, channel: SceytChannel, replyInThread: Boolean) {
        setAvatar(avatar, channel, replyInThread)
    }

    override fun onShowMessageActionsMenu(
            vararg messages: SceytMessage,
            menuStyle: MenuStyle,
            listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?,
    ) {
        showMessageActionsInToolbar(*messages, menuStyle = menuStyle, listener = listener)
    }

    override fun onHideMessageActionsMenu() {
        binding.hideMessageActions()
    }

    override fun onInitToolbarActionsMenu(vararg messages: SceytMessage, menu: Menu) {
        if (messages.isEmpty()) return
        val isSingleMessage = messages.size == 1
        val fistMessage = messages.first()
        val existPendingMessages = messages.any { it.deliveryStatus == DeliveryStatus.Pending }

        menu.findItem(R.id.sceyt_reply)?.isVisible = isSingleMessage && !existPendingMessages
        //menu.findItem(R.id.sceyt_reply_in_thread).isVisible = isSingleMessage && !isPending
        menu.findItem(R.id.sceyt_forward)?.isVisible = !existPendingMessages
        val expiredEditMessage = (System.currentTimeMillis() - fistMessage.createdAt) >
                SceytChatUIKit.config.messageEditTimeout
        menu.findItem(R.id.sceyt_edit_message)?.isVisible = isSingleMessage &&
                !fistMessage.incoming && fistMessage.body.isNotNullOrBlank() && !expiredEditMessage
        menu.findItem(R.id.sceyt_message_info)?.isVisible = isSingleMessage
                && !fistMessage.incoming && !existPendingMessages
        menu.findItem(R.id.sceyt_copy_message)?.isVisible = messages.any {
            it.body.isNotNullOrBlank()
        }
    }

    override fun showSearchMessagesBar(event: MessageCommandEvent.SearchMessages) {
        binding.toggleSearch(true)
        toolbarSearchModeChangeListener?.invoke(true)
    }

    //Click listeners
    override fun onAvatarClick(view: View) {
        clickListeners.onToolbarClick(view)
    }

    override fun onToolbarClick(view: View) {
        if (::channel.isInitialized)
            channelInfoLauncher?.let {
                ChannelInfoActivity.startHandleSearchClick(context, channel, it)
            }
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

    private fun SceytMessagesListHeaderViewBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        toolbarUnderline.background = style.underlineColor.toDrawable()
        toolbarUnderline.isVisible = style.showUnderline
        lottieUserActivity.isVisible = style.enableUserActivityIndicator
        icBack.setImageDrawable(style.navigationIcon)
        style.titleTextStyle.apply(title)
        style.subTitleStyle.apply(subTitle)
        style.searchInputStyle.apply(
            editText = inputSearch,
            inputRoot = layoutSearch,
            searchIconImage = icSearch,
            clearIconImage = icClear)
        style.messageActionsMenuStyle.apply(toolbarMessageActions)
        style.avatarStyle.apply(avatar)
    }
}