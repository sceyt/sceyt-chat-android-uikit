package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Direct
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Private
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Public
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.databinding.SceytActivityConversationInfoBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG_NAME
import com.sceyt.sceytchatuikit.extensions.animationListener
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.setOnClickListenerDisableClickViewForWhile
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog.Companion.showSceytDialog
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isDirect
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment.ClickActionsEnum.AudioCall
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment.ClickActionsEnum.CallOut
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment.ClickActionsEnum.Chat
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment.ClickActionsEnum.More
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment.ClickActionsEnum.Mute
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment.ClickActionsEnum.UnMute
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment.ClickActionsEnum.VideCall
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsPrivateChatFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsPrivateChatFragment.ClickActionsEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsPublicChannelFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsPublicChannelFragment.PublicChannelClickActionsEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.description.InfoDescriptionFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.BlockUser
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.ClearHistory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.Delete
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.UnBlockUser
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.GroupChatActionsDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteNotificationDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteTypeEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.editchannel.EditChannelFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.MemberTypeEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.membersbyrolebuttons.InfoMembersByRoleButtonsFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.photopreview.SceytPhotoPreviewActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.voice.ChannelVoiceFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date
import java.util.concurrent.TimeUnit

open class ConversationInfoActivity : AppCompatActivity(), SceytKoinComponent {
    private lateinit var channel: SceytChannel
    private lateinit var pagerAdapter: ViewPagerAdapter
    private var binding: SceytActivityConversationInfoBinding? = null
    protected val viewModel: ConversationInfoViewModel by viewModel()
    private var alphaAnimation: AlphaAnimation? = null
    private var showStartChatIcon: Boolean = false

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityContentView()
        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode)

        getBundleArguments()
        initViewModel()
        binding?.setupStyle()
        binding?.initViews()
        setChannelDetails(channel)
        viewModel.getChannelFromServer(channel.id)
        setupPagerAdapter(binding?.viewPager, binding?.tabLayout)
        addAppBarOffsetChangeListener(binding?.appbar)
        initButtons()
        observeToChannelUpdate()
        observeUserUpdateIfNeeded()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(intent.parcelable(CHANNEL))
        showStartChatIcon = intent.getBooleanExtra(SHOW_OPEN_CHAT_BUTTON, false)
    }

    private fun initViewModel() {
        viewModel.channelLiveData.observe(this) {
            channel = it
            onChannel(it)
        }

        viewModel.editChannelLiveData.observe(this) {
            channel = it
        }

        viewModel.leaveChannelLiveData.observe(this, ::onLeaveChannel)

        viewModel.deleteChannelLiveData.observe(this, ::onDeleteChannel)

        viewModel.clearHistoryLiveData.observe(this, ::onClearHistory)

        viewModel.blockUnblockUserLiveData.observe(this, ::onBlockUnblockUser)

        viewModel.joinLiveData.observe(this) {
            channel = it
            initButtons()
            onJoinChannel(it)
        }

        viewModel.muteUnMuteLiveData.observe(this) {
            channel.muted = it.muted
            initButtons()
            onMuteUnMuteChannel(it)
        }

        viewModel.channelAddMemberLiveData.observe(this, ::onAddMember)

        viewModel.findOrCreateChatLiveData.observe(this, ::onFindOrCreateChat)

        viewModel.pageStateLiveData.observe(this, ::onPageStateChange)
    }

    private fun initButtons() {
        val fragment: Fragment = when (channel.getChannelType()) {
            Direct -> {
                getInfoButtonsDirectChatFragment(channel, showStartChatIcon).also {
                    it.setClickActionsListener(::onButtonClick)
                }
            }

            Private -> {
                getInfoButtonsPrivateChatFragment(channel).also {
                    it.setClickActionsListener(::onGroupButtonClick)
                }
            }

            Public -> {
                getInfoButtonsPublicChannelFragment(channel).also {
                    it.setClickActionsListener(::onPublicButtonClick)
                }
            }
        }
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.layout_buttons, fragment, fragment.TAG_NAME)
        }
    }

    private fun observeToChannelUpdate() {
        ChannelsCache.channelUpdatedFlow
            .filter { it.channel.id == channel.id }
            .onEach {
                channel = it.channel
                onChannel(it.channel)
            }
            .launchIn(lifecycleScope)
    }

    private fun observeUserUpdateIfNeeded() {
        if (channel.isDirect()) {
            SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged()
                .onEach {
                    it.find { user -> user.user.id == getChannel().getFirstMember()?.id }?.let { presenceUser ->
                        with(getBinding() ?: return@onEach) {
                            val user = presenceUser.user
                            val userName = SceytKitConfig.userNameBuilder?.invoke(user)
                                    ?: user.getPresentableName()
                            avatar.setNameAndImageUrl(userName, user.avatarURL, UserStyle.userDefaultAvatar)
                            toolbarAvatar.setNameAndImageUrl(userName, user.avatarURL, UserStyle.userDefaultAvatar)
                        }
                    }
                }.launchIn(lifecycleScope)
        }
    }

    private fun onButtonClick(clickActionsEnum: InfoButtonsDirectChatFragment.ClickActionsEnum) {
        when (clickActionsEnum) {
            Chat -> onStartChatCallClick(channel)
            VideCall -> onVideoCallClick(channel)
            AudioCall -> onAudioCallClick(channel)
            CallOut -> onCallOutClick(channel)
            Mute -> onMuteUnMuteClick(channel, true)
            UnMute -> onMuteUnMuteClick(channel, false)
            More -> onMoreClick(channel)
        }
    }

    private fun onGroupButtonClick(clickActionsEnum: ClickActionsEnum) {
        when (clickActionsEnum) {
            ClickActionsEnum.Mute -> onMuteUnMuteClick(channel, true)
            ClickActionsEnum.UnMute -> onMuteUnMuteClick(channel, false)
            ClickActionsEnum.Call -> onAudioCallClick(channel)
            ClickActionsEnum.VideoCall -> onVideoCallClick(channel)
            ClickActionsEnum.More -> onMoreClick(channel)
        }
    }

    private fun onPublicButtonClick(clickActionsEnum: PublicChannelClickActionsEnum) {
        when (clickActionsEnum) {
            PublicChannelClickActionsEnum.Mute -> onMuteUnMuteClick(channel, true)
            PublicChannelClickActionsEnum.UnMute -> onMuteUnMuteClick(channel, false)
            PublicChannelClickActionsEnum.Leave -> onLeaveChatClick(channel)
            PublicChannelClickActionsEnum.Report -> onReportClick(channel)
            PublicChannelClickActionsEnum.Join -> onJoinClick(channel)
            PublicChannelClickActionsEnum.Add -> onAddSubscribersClick(channel)
            PublicChannelClickActionsEnum.More -> onMoreClick(channel)
        }
    }

    private fun SceytActivityConversationInfoBinding.initViews() {
        icBack.setOnClickListener {
            finish()
        }

        icEdit.setOnClickListenerDisableClickViewForWhile {
            onEditClick(channel)
        }

        avatar.setOnClickListener {
            onAvatarClick(channel)
        }
    }

    protected fun addAppBarOffsetChangeListener(appBar: AppBarLayout?) {
        var isShow = false
        var scrollRange = -1

        appBar?.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }
            if (scrollRange + verticalOffset == 0) {
                isShow = true
                showHideLayoutToolbarWithAnim(true, binding?.layoutToolbar)
            } else if (isShow) {
                showHideLayoutToolbarWithAnim(false, binding?.layoutToolbar)
                isShow = false
            }
        }
    }

    protected fun setupPagerAdapter(viewPager: ViewPager2?, tabLayout: TabLayout?) {
        val fragments = arrayListOf<Fragment>(
            getChannelMediaFragment(channel),
            getChannelFilesFragment(channel),
            getChannelVoiceFragment(channel),
            getChannelLinksFragment(channel),
        )

        pagerAdapter = ViewPagerAdapter(this, fragments)

        setPagerAdapter(pagerAdapter)
        setupTabLayout(tabLayout ?: return, viewPager ?: return)
    }

    protected fun setupTabLayout(tabLayout: TabLayout, viewPager: ViewPager2) {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getTagByPosition(position)
        }.attach()

        tabLayout.apply {
            val color = getCompatColor(SceytKitConfig.sceytColorAccent)
            setSelectedTabIndicatorColor(color)
            tabRippleColor = ColorStateList.valueOf(color)
        }
    }

    protected fun clearHistory(forEveryone: Boolean) {
        viewModel.clearHistory(channel.id, forEveryone)
    }

    protected fun leaveChannel() {
        viewModel.leaveChannel(channel.id)
    }

    protected fun blockAndLeaveChannel() {
        viewModel.blockAndLeaveChannel(channel.id)
    }

    protected fun blockUser(userId: String) {
        viewModel.blockUser(userId)
    }

    protected fun unblockUser(userId: String) {
        viewModel.unblockUser(userId)
    }

    protected fun deleteChannel() {
        viewModel.deleteChannel(channel.id)
    }

    protected fun muteChannel(until: Long) {
        viewModel.muteChannel(channel.id, until)
    }

    protected fun unMuteChannel() {
        viewModel.unMuteChannel(channel.id)
    }

    protected fun joinChannel() {
        viewModel.joinChannel(channel.id)
    }

    protected fun addMembers(members: List<SceytMember>) {
        viewModel.addMembersToChannel(channel.id, members as ArrayList)
    }

    protected fun getChannel() = channel.clone()

    protected fun getBinding() = binding

    protected fun getMembersType(): MemberTypeEnum {
        return if (::channel.isInitialized) {
            when (channel.getChannelType()) {
                Private, Direct -> MemberTypeEnum.Member
                Public -> MemberTypeEnum.Subscriber
            }
        } else MemberTypeEnum.Member
    }

    open fun setChannelDetails(channel: SceytChannel) {
        with(binding ?: return) {
            val myRole = channel.userRole
            val isOwnerOrAdmin = myRole == RoleTypeEnum.Owner.toString() || myRole == RoleTypeEnum.Admin.toString()

            icEdit.isVisible = !channel.isDirect() && isOwnerOrAdmin

            setChannelTitle(channel)
            setChannelMembersByRoleButtons(channel)
            setPresenceOrMembers(channel)
            setChannelDescription(channel)
            setChannelAvatar(channel)
            setChannelAdditionalInfoFragment(channel)
        }
    }

    open fun setActivityContentView() {
        setContentView(SceytActivityConversationInfoBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)
    }

    open fun onMembersClick(channel: SceytChannel) {
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            val fragment = getChannelMembersFragment(channel, getMembersType())
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    open fun onAdminsClick(channel: SceytChannel) {
        binding ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            val fragment = getChannelMembersFragment(channel, MemberTypeEnum.Admin)
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    open fun onEditClick(channel: SceytChannel) {
        binding ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            val fragment = getEditChannelFragment(channel)
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    open fun onAvatarClick(channel: SceytChannel) {
        val icon = channel.iconUrl
        if (!icon.isNullOrBlank()) {
            val title = if (channel.isDirect()) {
                val user = channel.getFirstMember()?.user
                if (user != null) SceytKitConfig.userNameBuilder?.invoke(user)
                        ?: user.getPresentableName() else null
            } else channel.subject

            SceytPhotoPreviewActivity.launchActivity(this, icon, title)
        }
    }

    open fun onClearHistoryClick(channel: SceytChannel) {
        val descId: Int = when (channel.getChannelType()) {
            Direct -> R.string.sceyt_clear_direct_history_desc
            Private -> R.string.sceyt_clear_private_chat_history_desc
            Public -> R.string.sceyt_clear_public_chat_history_desc
        }
        showSceytDialog(this, R.string.sceyt_clear_history_title, descId, R.string.sceyt_clear, positiveCb = {
            clearHistory(channel.isPublic())
        })
    }

    open fun onLeaveChatClick(channel: SceytChannel) {
        val titleId: Int
        val descId: Int
        when (channel.getChannelType()) {
            Private -> {
                titleId = R.string.sceyt_leave_group_title
                descId = R.string.sceyt_leave_group_desc
            }

            Public -> {
                titleId = R.string.sceyt_leave_channel_title
                descId = R.string.sceyt_leave_channel_desc
            }

            else -> return
        }
        showSceytDialog(this, titleId, descId, R.string.sceyt_leave, positiveCb = {
            leaveChannel()
        })
    }

    open fun onBlockUnBlockUserClick(channel: SceytChannel, block: Boolean) {
        val peer = channel.getFirstMember() ?: return
        if (block) {
            showSceytDialog(this, R.string.sceyt_block_user_title,
                R.string.sceyt_block_user_desc, R.string.sceyt_block, positiveCb = {
                    blockUser(peer.id)
                })
        } else unblockUser(peer.id)
    }

    open fun onDeleteChatClick(channel: SceytChannel) {
        val titleId: Int
        val descId: Int
        when (channel.getChannelType()) {
            Private -> {
                titleId = R.string.sceyt_delete_group_title
                descId = R.string.sceyt_delete_group_desc
            }

            Public -> {
                titleId = R.string.sceyt_delete_channel_title
                descId = R.string.sceyt_delete_channel_desc
            }

            Direct -> {
                titleId = R.string.sceyt_delete_p2p_title
                descId = R.string.sceyt_delete_p2p_desc
            }
        }
        showSceytDialog(this, titleId, descId, R.string.sceyt_delete, positiveCb = {
            deleteChannel()
        })
    }

    open fun onStartChatCallClick(channel: SceytChannel) {
        if (!channel.isDirect()) return
        val peer = channel.getFirstMember() ?: return
        viewModel.findOrCreateChat(peer.user)
    }

    open fun onVideoCallClick(channel: SceytChannel) {
    }

    open fun onAudioCallClick(channel: SceytChannel) {
    }

    open fun onCallOutClick(channel: SceytChannel) {
    }

    open fun onAddMember(data: ChannelMembersEventData) {
    }

    open fun onFindOrCreateChat(sceytChannel: SceytChannel) {
    }

    open fun onMoreClick(channel: SceytChannel) {
        if (channel.isGroup) {
            GroupChatActionsDialog.newInstance(this, channel).apply {
                setChooseTypeCb(::onGroupChatMoreActionClick)
            }.show()
        } else
            DirectChatActionsDialog.newInstance(this, channel, showStartChatIcon).apply {
                setChooseTypeCb(::onDirectChatMoreActionClick)
            }.show()
    }

    open fun onJoinClick(channel: SceytChannel) {
        joinChannel()
    }

    open fun onAddSubscribersClick(channel: SceytChannel) {
    }

    open fun onReportClick(channel: SceytChannel) {
    }

    open fun onDirectChatMoreActionClick(actionsEnum: DirectChatActionsDialog.ActionsEnum) {
        when (actionsEnum) {
            ClearHistory -> onClearHistoryClick(channel)
            BlockUser -> onBlockUnBlockUserClick(channel, true)
            UnBlockUser -> onBlockUnBlockUserClick(channel, false)
            Delete -> onDeleteChatClick(channel)
            DirectChatActionsDialog.ActionsEnum.Mute -> onMuteUnMuteClick(channel, true)
            DirectChatActionsDialog.ActionsEnum.UnMute -> onMuteUnMuteClick(channel, false)
        }
    }

    open fun onGroupChatMoreActionClick(actionsEnum: GroupChatActionsDialog.ActionsEnum) {
        when (actionsEnum) {
            GroupChatActionsDialog.ActionsEnum.ClearHistory -> onClearHistoryClick(channel)
            GroupChatActionsDialog.ActionsEnum.Leave -> onLeaveChatClick(channel)
            GroupChatActionsDialog.ActionsEnum.Delete -> onDeleteChatClick(channel)
        }
    }

    open fun onChannel(channel: SceytChannel) {
        lifecycleScope.launch {
            setChannelDetails(channel)
            initButtons()
            pagerAdapter.getFragment().find { fragment -> fragment is ChannelMembersFragment }?.let { membersFragment ->
                (membersFragment as ChannelMembersFragment).updateChannel(channel)
            }
        }
    }

    open fun onLeaveChannel(channelId: Long) {
        finish()
    }

    open fun onDeleteChannel(channelId: Long) {
        finish()
    }

    open fun onMuteUnMuteChannel(sceytChannel: SceytChannel) {
    }

    open fun onJoinChannel(sceytChannel: SceytChannel) {
    }

    open fun onClearHistory(channelId: Long) {
        pagerAdapter.historyCleared()
        customToastSnackBar(getString(R.string.sceyt_history_was_successfully_cleared))
    }

    open fun onBlockUnblockUser(users: List<User>) {
        val peer = channel.getFirstMember()
        users.find { user -> user.id == peer?.id }?.let { user ->
            peer?.user = user
        }
    }

    open fun onMuteUnMuteClick(sceytChannel: SceytChannel, mute: Boolean) {
        if (mute.not()) {
            unMuteChannel()
        } else {
            MuteNotificationDialog(this@ConversationInfoActivity) {
                val until = when (it) {
                    MuteTypeEnum.Mute1Hour -> TimeUnit.HOURS.toMillis(1)
                    MuteTypeEnum.Mute8Hour -> TimeUnit.HOURS.toMillis(8)
                    MuteTypeEnum.MuteForever -> 0L
                }
                muteChannel(until)
            }.show()
        }
    }

    open fun setPagerAdapter(pagerAdapter: ViewPagerAdapter) {
        binding?.viewPager?.adapter = pagerAdapter
    }

    open fun showHideLayoutToolbarWithAnim(show: Boolean, layoutToolbar: ViewGroup?) {
        if (show == layoutToolbar?.isVisible) return
        layoutToolbar?.let { layout ->
            val to = if (show) 1f else 0f
            val from = if (show) 0f else 1f
            if (show) layout.isVisible = true
            alphaAnimation?.cancel()
            alphaAnimation = AlphaAnimation(from, to).apply {
                duration = 100
                setAnimationListener(animationListener(onAnimationEnd = {
                    if (show.not())
                        layout.isInvisible = true
                }))
            }
            layout.startAnimation(alphaAnimation)
        }
    }

    open fun setChannelTitle(channel: SceytChannel) {
        with(binding ?: return) {
            if (channel.isPeerDeleted()) {
                subject.text = getString(R.string.sceyt_deleted_user)
                titleToolbar.text = getString(R.string.sceyt_deleted_user)
            } else {
                subject.text = channel.channelSubject
                titleToolbar.text = channel.channelSubject
            }
        }
    }

    private fun setChannelMembersByRoleButtons(channel: SceytChannel) {
        binding ?: return
        val fragment = getChannelMembersByRoleFragment(channel).also {
            it.setClickActionsListener { actionsEnum ->
                when (actionsEnum) {
                    InfoMembersByRoleButtonsFragment.ClickActionsEnum.Admins -> onAdminsClick(channel)
                    InfoMembersByRoleButtonsFragment.ClickActionsEnum.Members -> onMembersClick(channel)
                }
            }
        }
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.frame_layout_members_by_role, fragment, fragment.TAG_NAME)
        }
    }

    open fun setPresenceOrMembers(channel: SceytChannel) {
        with(binding ?: return) {
            if (channel.isPeerDeleted()) {
                tvPresenceOrMembers.isVisible = false
                subTitleToolbar.isVisible = false
                return
            }
            val title: String = when (channel.getChannelType()) {
                Direct -> {
                    val member = channel.getFirstMember() ?: return
                    if (member.user.presence?.state == PresenceState.Online) {
                        getString(R.string.sceyt_online)
                    } else {
                        member.user.presence?.lastActiveAt?.let {
                            if (it != 0L)
                                DateTimeUtil.getPresenceDateFormatData(this@ConversationInfoActivity, Date(it))
                            else ""
                        } ?: ""
                    }
                }

                Private -> {
                    val memberCount = channel.memberCount
                    if (memberCount > 1)
                        getString(R.string.sceyt_members_count, memberCount)
                    else getString(R.string.sceyt_member_count, memberCount)
                }

                Public -> {
                    val memberCount = channel.memberCount
                    if (memberCount > 1)
                        getString(R.string.sceyt_subscribers_count, memberCount)
                    else getString(R.string.sceyt_subscriber_count, memberCount)
                }
            }
            tvPresenceOrMembers.text = title
            subTitleToolbar.text = title
        }
    }

    open fun setChannelAvatar(channel: SceytChannel) {
        with(binding ?: return) {
            if (channel.isPeerDeleted()) {
                avatar.setImageUrl(null, UserStyle.deletedUserAvatar)
                toolbarAvatar.setImageUrl(null, UserStyle.deletedUserAvatar)
            } else {
                avatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl)
                toolbarAvatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl)
            }
        }
    }

    open fun setChannelDescription(channel: SceytChannel) {
        binding ?: return
        val fragment = getChannelDescriptionFragment(channel)
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.frame_layout_description, fragment, fragment.TAG_NAME)
        }
    }

    open fun setChannelAdditionalInfoFragment(channel: SceytChannel) {
        binding ?: return
        val fragment = getChannelAdditionalInfoFragment(channel) ?: return
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.frame_layout_additional_info, fragment, fragment.TAG_NAME)
        }
    }

    open fun getChannelMembersFragment(channel: SceytChannel, memberType: MemberTypeEnum) =
            ChannelMembersFragment.newInstance(channel, memberType)

    open fun getChannelMediaFragment(channel: SceytChannel) = ChannelMediaFragment.newInstance(channel)

    open fun getChannelFilesFragment(channel: SceytChannel) = ChannelFilesFragment.newInstance(channel)

    open fun getChannelLinksFragment(channel: SceytChannel) = ChannelLinksFragment.newInstance(channel)

    open fun getChannelVoiceFragment(channel: SceytChannel) = ChannelVoiceFragment.newInstance(channel)

    open fun getEditChannelFragment(channel: SceytChannel) = EditChannelFragment.newInstance(channel)


    //Buttons
    open fun getInfoButtonsDirectChatFragment(channel: SceytChannel, showOpenChatButton: Boolean) = InfoButtonsDirectChatFragment.newInstance(channel, showOpenChatButton)

    open fun getInfoButtonsPrivateChatFragment(channel: SceytChannel) = InfoButtonsPrivateChatFragment.newInstance(channel)

    open fun getInfoButtonsPublicChannelFragment(channel: SceytChannel) = InfoButtonsPublicChannelFragment.newInstance(channel)

    //Description
    open fun getChannelDescriptionFragment(channel: SceytChannel) = InfoDescriptionFragment.newInstance(channel)

    //Members by role buttons
    open fun getChannelMembersByRoleFragment(channel: SceytChannel) = InfoMembersByRoleButtonsFragment.newInstance(channel)

    //Additional info
    open fun getChannelAdditionalInfoFragment(channel: SceytChannel): Fragment? = null

    open fun onPageStateChange(pageState: PageState) {
        if (pageState is PageState.StateError) {
            setChannelDetails(channel)
            if (pageState.showMessage)
                customToastSnackBar(pageState.errorMessage.toString())
        }
    }

    open fun getViewPagerY(): Int {
        return (binding?.appbar?.height ?: 0)
    }

    open fun getRootFragmentId(): Int = R.id.rootFrameLayout

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    private fun SceytActivityConversationInfoBinding.setupStyle() {
        icBack.imageTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
        icEdit.imageTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    companion object {
        const val CHANNEL = "CHANNEL"
        const val SHOW_OPEN_CHAT_BUTTON = "SHOW_OPEN_CHAT_BUTTON"

        fun newInstance(context: Context, channel: SceytChannel, showOpenChatButton: Boolean = false) {
            context.launchActivity<ConversationInfoActivity> {
                putExtra(CHANNEL, channel)
                putExtra(SHOW_OPEN_CHAT_BUTTON, showOpenChatButton)
            }
            context.asActivity().overrideTransitions(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold, true)
        }
    }
}