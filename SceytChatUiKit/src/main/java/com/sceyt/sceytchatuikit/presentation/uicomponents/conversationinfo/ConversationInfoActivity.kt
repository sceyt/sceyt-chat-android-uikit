package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Broadcast
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Direct
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Group
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Private
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Public
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.databinding.SceytActivityConversationInfoBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG_NAME
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.changeAlphaWithAnimation
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.setOnClickListenerDisableClickViewForWhile
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog.Companion.showSceytDialog
import com.sceyt.sceytchatuikit.presentation.common.checkIsMemberInChannel
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.presentation.common.getDefaultAvatar
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isDirect
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.channelInfo.InfoDetailsFragment
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
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.settings.InfoSettingsFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.specifications.InfoSpecificationsFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.voice.ChannelVoiceFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.ConversationInfoStyle
import com.sceyt.sceytchatuikit.sceytstyles.UserStyle
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date
import java.util.concurrent.TimeUnit

open class ConversationInfoActivity : AppCompatActivity(), SceytKoinComponent {
    private lateinit var channel: SceytChannel
    private lateinit var pagerAdapter: ViewPagerAdapter
    private var binding: SceytActivityConversationInfoBinding? = null
    protected val viewModel: ConversationInfoViewModel by viewModel()
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
        viewModel.observeToChannelUpdate(channel.id)
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

        viewModel.channelUpdatedLiveData.observe(this) {
            channel = it.channel
            onChannel(it.channel)
        }

        viewModel.userPresenceUpdateLiveData.observe(this, ::onUserPresenceUpdated)

        viewModel.leaveChannelLiveData.observe(this, ::onLeftChannel)

        viewModel.deleteChannelLiveData.observe(this, ::onDeletedChannel)

        viewModel.clearHistoryLiveData.observe(this, ::onClearedHistory)

        viewModel.blockUnblockUserLiveData.observe(this, ::onBlockedOrUnblockedUser)

        viewModel.joinLiveData.observe(this) {
            channel = it
            onJoinedChannel(it)
        }

        viewModel.muteUnMuteLiveData.observe(this) {
            channel.muted = it.muted
            onMutedOrUnMutedChannel(it)
        }

        viewModel.channelAddMemberLiveData.observe(this, ::onAddedMember)

        viewModel.findOrCreateChatLiveData.observe(this, ::onFindOrCreateChat)

        viewModel.pageStateLiveData.observe(this, ::onPageStateChanged)
    }

    private fun observeUserUpdateIfNeeded() {
        if (channel.isDirect())
            viewModel.observeUserPresenceUpdate(channel)
    }

    private fun SceytActivityConversationInfoBinding.initViews() {
        (layoutDetails.layoutParams as? CollapsingToolbarLayout.LayoutParams)?.let {
            it.collapseMode = getLayoutDetailsCollapseMode()
        }

        icBack.setOnClickListener {
            finish()
        }

        icEdit.setOnClickListenerDisableClickViewForWhile {
            onEditClick(channel)
        }

        icMore.setOnClickListenerDisableClickViewForWhile {
            onMoreClick(channel)
        }
    }

    private fun <T : Fragment?> initOrUpdateFragmentChannel(container: FragmentContainerView,
                                                            fragmentProvider: () -> T) {
        container.getFragment<T>()?.let {
            if (it.isAdded)
                (it as? ChannelUpdateListener)?.onChannelUpdated(channel)
            it
        } ?: run {
            val fragment = fragmentProvider() ?: return@run
            supportFragmentManager.commit(allowStateLoss = true) {
                replace(container.id, fragment, fragment.TAG_NAME)
            }
        }
    }

    open fun getLayoutDetailsCollapseMode(): Int {
        return CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
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
                toggleToolbarViews(true, binding?.layoutToolbar)
            } else if (isShow) {
                toggleToolbarViews(false, binding?.layoutToolbar)
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
                Private, Direct, Group -> MemberTypeEnum.Member
                Public, Broadcast -> MemberTypeEnum.Subscriber
            }
        } else MemberTypeEnum.Member
    }

    open fun setChannelDetails(channel: SceytChannel) {
        with(binding ?: return) {
            val myRole = channel.userRole
            val isOwnerOrAdmin = myRole == RoleTypeEnum.Owner.toString() || myRole == RoleTypeEnum.Admin.toString()

            icEdit.isVisible = !channel.isDirect() && isOwnerOrAdmin
            icMore.isVisible = channel.checkIsMemberInChannel()

            setChannelToolbarTitle(channel)
            setChannelToolbarAvatar(channel)
            setToolbarSubtitle(channel)
            setChannelSettings(channel)
            setChannelMembersByRoleButtons(channel)
            setChannelDescription(channel)
            setChannelInfo(channel)
            setChannelAdditionalInfoFragment(channel)
            setChannelSpecifications(channel)
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
            Private, Group -> R.string.sceyt_clear_private_chat_history_desc
            Public, Broadcast -> R.string.sceyt_clear_public_chat_history_desc
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
            Private, Group -> {
                titleId = R.string.sceyt_delete_group_title
                descId = R.string.sceyt_delete_group_desc
            }

            Public, Broadcast -> {
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

    open fun onAddedMember(data: ChannelMembersEventData) {
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
            pagerAdapter.getFragment().find { fragment -> fragment is ChannelMembersFragment }?.let { membersFragment ->
                (membersFragment as ChannelMembersFragment).updateChannel(channel)
            }
        }
    }

    open fun onUserPresenceUpdated(presenceUser: SceytPresenceChecker.PresenceUser) {
        with(getBinding() ?: return) {
            val user = presenceUser.user
            val userName = SceytKitConfig.userNameBuilder?.invoke(user)
                    ?: user.getPresentableName()
            toolbarAvatar.setNameAndImageUrl(userName, user.avatarURL, UserStyle.userDefaultAvatar)
            (frameLayoutInfo.getFragment() as? InfoDetailsFragment)?.onUserPresenceUpdated(presenceUser)
        }
    }

    open fun onLeftChannel(channelId: Long) {
        finish()
    }

    open fun onDeletedChannel(channelId: Long) {
        finish()
    }

    open fun onMutedOrUnMutedChannel(sceytChannel: SceytChannel) {
    }

    open fun onJoinedChannel(sceytChannel: SceytChannel) {
    }

    open fun onClearedHistory(channelId: Long) {
        pagerAdapter.historyCleared()
        customToastSnackBar(getString(R.string.sceyt_history_was_successfully_cleared))
    }

    open fun onBlockedOrUnblockedUser(users: List<User>) {
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

    open fun toggleToolbarViews(showDetails: Boolean, view: View?) {
        val toolbarLayout = binding?.layoutToolbar ?: return
        if (showDetails == toolbarLayout.isVisible) return
        val to = if (showDetails) 1f else 0f
        if (showDetails) toolbarLayout.isVisible = true
        binding?.tvToolbarInfo?.isVisible = !showDetails
        toolbarLayout.changeAlphaWithAnimation(toolbarLayout.alpha, to, 100) {
            if (showDetails.not()) {
                toolbarLayout.isInvisible = true
            }
        }
    }

    open fun setChannelToolbarTitle(channel: SceytChannel) {
        with(binding ?: return) {
            titleToolbar.text = if (channel.isPeerDeleted()) {
                getString(R.string.sceyt_deleted_user)
            } else channel.channelSubject
        }
    }

    private fun setChannelMembersByRoleButtons(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutMembersByRole ?: return) {
            getChannelMembersByRoleFragment(channel) { actionsEnum ->
                when (actionsEnum) {
                    InfoMembersByRoleButtonsFragment.ClickActionsEnum.Admins -> onAdminsClick(channel)
                    InfoMembersByRoleButtonsFragment.ClickActionsEnum.Members -> onMembersClick(channel)
                }
            }
        }
    }

    open fun setToolbarSubtitle(channel: SceytChannel) {
        with(binding ?: return) {
            if (channel.isPeerDeleted()) {
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

                Private, Group -> {
                    val memberCount = channel.memberCount
                    if (memberCount > 1)
                        getString(R.string.sceyt_members_count, memberCount)
                    else getString(R.string.sceyt_member_count, memberCount)
                }

                Public, Broadcast -> {
                    val memberCount = channel.memberCount
                    if (memberCount > 1)
                        getString(R.string.sceyt_subscribers_count, memberCount)
                    else getString(R.string.sceyt_subscriber_count, memberCount)
                }
            }
            subTitleToolbar.text = title
        }
    }

    open fun setChannelSettings(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutSettings ?: return) {
            getChannelSettingsFragment(channel) { actionsEnum ->
                when (actionsEnum) {
                    InfoSettingsFragment.ClickActionsEnum.Mute -> onMuteUnMuteClick(channel, true)
                    InfoSettingsFragment.ClickActionsEnum.UnMute -> onMuteUnMuteClick(channel, false)
                    InfoSettingsFragment.ClickActionsEnum.AutoDeleteOn -> {}
                    InfoSettingsFragment.ClickActionsEnum.AutoDeleteOff -> {}
                }
            }
        }
    }

    open fun setChannelToolbarAvatar(channel: SceytChannel) {
        with(binding ?: return) {
            toolbarAvatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl, channel.getDefaultAvatar())
        }
    }

    open fun setChannelDescription(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutDescription ?: return) {
            getChannelDescriptionFragment(channel)
        }
    }

    open fun setChannelInfo(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutInfo ?: return) {
            getChannelDetailsFragment(channel) { actionsEnum ->
                when (actionsEnum) {
                    InfoDetailsFragment.ClickActionsEnum.Avatar -> onAvatarClick(channel)
                }
            }
        }
    }

    open fun setChannelSpecifications(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutSpecifications ?: return) {
            getChannelSpecificationsFragment(channel)
        }
    }

    open fun setChannelAdditionalInfoFragment(channel: SceytChannel) {
        binding ?: return
        val fragment = getChannelAdditionalInfoFragment(channel) ?: return
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.frame_layout_additional_info, fragment, fragment.TAG_NAME)
        }
    }

    open fun getChannelMembersFragment(channel: SceytChannel, memberType: MemberTypeEnum): Fragment =
            ChannelMembersFragment.newInstance(channel, memberType)

    open fun getChannelMediaFragment(channel: SceytChannel): Fragment = ChannelMediaFragment.newInstance(channel)

    open fun getChannelFilesFragment(channel: SceytChannel): Fragment = ChannelFilesFragment.newInstance(channel)

    open fun getChannelLinksFragment(channel: SceytChannel): Fragment = ChannelLinksFragment.newInstance(channel)

    open fun getChannelVoiceFragment(channel: SceytChannel): Fragment = ChannelVoiceFragment.newInstance(channel)

    open fun getEditChannelFragment(channel: SceytChannel): Fragment = EditChannelFragment.newInstance(channel)

    //Description
    open fun getChannelDescriptionFragment(channel: SceytChannel): Fragment = InfoDescriptionFragment.newInstance(channel)

    open fun getChannelDetailsFragment(channel: SceytChannel,
                                       listener: (InfoDetailsFragment.ClickActionsEnum) -> Unit): Fragment {
        return InfoDetailsFragment.newInstance(channel, listener)
    }

    open fun getChannelSettingsFragment(channel: SceytChannel,
                                        listener: (InfoSettingsFragment.ClickActionsEnum) -> Unit): Fragment {
        return InfoSettingsFragment.newInstance(channel, listener)
    }

    //Members by role buttons
    open fun getChannelMembersByRoleFragment(channel: SceytChannel,
                                             listener: (InfoMembersByRoleButtonsFragment.ClickActionsEnum) -> Unit): Fragment {
        return InfoMembersByRoleButtonsFragment.newInstance(channel, listener)
    }

    //Additional info
    open fun getChannelAdditionalInfoFragment(channel: SceytChannel): Fragment? = null

    //Specifications
    open fun getChannelSpecificationsFragment(channel: SceytChannel) = InfoSpecificationsFragment.newInstance(channel)

    open fun onPageStateChanged(pageState: PageState) {
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
        icBack.setImageResource(ConversationInfoStyle.navigationIcon)
        icEdit.setImageResource(ConversationInfoStyle.editIcon)
        icMore.setImageResource(ConversationInfoStyle.moreIcon)
        icBack.imageTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
        icEdit.imageTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
        icMore.imageTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
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