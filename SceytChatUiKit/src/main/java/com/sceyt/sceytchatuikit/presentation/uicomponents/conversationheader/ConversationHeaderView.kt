package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
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
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.message.DeliveryStatus
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
import com.sceyt.sceytchatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.extensions.hideKeyboard
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.maybeComponentActivity
import com.sceyt.sceytchatuikit.extensions.showSoftInput
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.presentation.common.getPeer
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.common.isSelf
import com.sceyt.sceytchatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners.HeaderEventsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners.HeaderEventsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.ConversationHeaderViewStyle
import com.sceyt.sceytchatuikit.sceytstyles.UserStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
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
    private var isReplyInThread: Boolean = false
    private var isGroup = false
    private var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
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
        binding = SceytConversationHeaderViewBinding.inflate(LayoutInflater.from(context), this, true)

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

    private fun SceytConversationHeaderViewBinding.setUpStyle() {
        icBack.setImageResource(ConversationHeaderViewStyle.backIcon)
        title.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.titleColor))
        subTitle.setTextColor(context.getCompatColor(ConversationHeaderViewStyle.subTitleColor))
        toolbarUnderline.background = ColorDrawable(context.getCompatColor(ConversationHeaderViewStyle.underlineColor))
        toolbarUnderline.isVisible = ConversationHeaderViewStyle.enableUnderline
        icSearch.imageTintList = ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
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
                channel.isSelf() -> getString(R.string.self_notes)
                else -> {
                    val member = channel.getPeer() ?: return
                    userNameBuilder?.invoke(member.user)
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

                    ChannelTypeEnum.Private, ChannelTypeEnum.Group -> {
                        val memberCount = channel.memberCount
                        if (memberCount > 1)
                            getString(R.string.sceyt_members_count, memberCount)
                        else getString(R.string.sceyt_member_count, memberCount)
                    }

                    ChannelTypeEnum.Public, ChannelTypeEnum.Broadcast -> {
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

    private fun setAvatar(avatar: SceytAvatarView, channel: SceytChannel, replyInThread: Boolean = false) {
        binding.avatar.isVisible = !replyInThread
        if (!replyInThread) {
            when {
                channel.isPeerDeleted() -> avatar.setImageUrl(null, UserStyle.deletedUserAvatar)
                channel.isSelf() -> avatar.setImageUrl(null, UserStyle.notesAvatar)
                else -> {
                    val subjAndSUrl = channel.getSubjectAndAvatarUrl()
                    avatar.setNameAndImageUrl(subjAndSUrl.first, subjAndSUrl.second, if (isGroup) 0 else UserStyle.userDefaultAvatar)
                }
            }
        }
    }

    private fun showMessageActionsInToolbar(vararg messages: SceytMessage, @MenuRes resId: Int,
                                            listener: ((MenuItem, actionFinish: () -> Unit) -> Unit)?): Menu? {
        val menu: Menu?
        with(binding) {
            toolBarMessageActions.setToolbarIconsVisibilityInitializer { messages, menu ->
                uiElementsListeners.onInitToolbarActionsMenu(*messages, menu = menu)
            }
            menu = toolBarMessageActions.setupMenuWithMessages(resId, *messages)
            toolBarMessageActions.isVisible = true
            layoutToolbarDetails.isVisible = false
            isShowingMessageActions = true
            addedMenu?.forEach { it.isVisible = false }

            toolBarMessageActions.setMenuItemClickListener {
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
                member.user = user
                if (!typingUsersHelper.isTyping)
                    uiElementsListeners.onSubTitle(binding.subTitle, channel, replyMessage, isReplyInThread)
            }
        }
    }

    private fun SceytConversationHeaderViewBinding.hideMessageActions() {
        toolBarMessageActions.isVisible = false
        layoutToolbarDetails.isVisible = true
        isShowingMessageActions = false
        addedMenu?.forEach { item -> item.isVisible = true }
    }

    private fun SceytConversationHeaderViewBinding.toggleSearch(showSearch: Boolean) {
        hideMessageActions()
        layoutSearch.isVisible = showSearch
        layoutToolbarDetails.isVisible = !showSearch
        root.setBackgroundColor(if (showSearch) context.getCompatColor(R.color.sceyt_color_bg)
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

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
        typingUsersHelper.setUserNameBuilder(builder)
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

    fun getToolbarMenu() = binding.headerToolbar.menu

    fun enableDisableToShowPresence(enable: Boolean) {
        enablePresence = enable
    }

    private val conversationInfoLauncher = context.asComponentActivity().registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getBooleanExtra(ConversationInfoActivity.ACTION_SEARCH_MESSAGES, false)?.let { search ->
                if (search)
                    showSearchMessagesBar(MessageCommandEvent.SearchMessages(true))
            }
        }
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

    override fun onAvatar(avatar: SceytAvatarView, channel: SceytChannel, replyInThread: Boolean) {
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
            menu.findItem(R.id.sceyt_forward).isVisible = !isPending
            menu.findItem(R.id.sceyt_edit_message).isVisible = isSingleMessage && !message.incoming && message.body.isNotNullOrBlank()
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
            ConversationInfoActivity.startWithLauncher(context, channel, conversationInfoLauncher)
    }

    override fun onToolbarClick(view: View) {
        if (::channel.isInitialized)
            ConversationInfoActivity.startWithLauncher(context, channel, conversationInfoLauncher)
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
        }
    }
}