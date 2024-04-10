package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Broadcast
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Direct
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Group
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Private
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.Public
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.databinding.SceytActivityConversationInfoBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG_NAME
import com.sceyt.sceytchatuikit.extensions.createIntent
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.common.ChannelActionConfirmationWithDialog
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog.Companion.showSceytDialog
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.presentation.common.getPeer
import com.sceyt.sceytchatuikit.presentation.common.isDirect
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.channelInfo.InfoDetailsFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.description.InfoDescriptionFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.BlockUser
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.ClearHistory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.Delete
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.Pin
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.UnBlockUser
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.UnPin
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.GroupChatActionsDialog
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
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.toolbar.InfoToolbarFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.toolbar.InfoToolbarFragment.ClickActionsEnum.Back
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.toolbar.InfoToolbarFragment.ClickActionsEnum.Edit
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.toolbar.InfoToolbarFragment.ClickActionsEnum.More
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.voice.ChannelVoiceFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker

open class ConversationInfoActivity : AppCompatActivity(), SceytKoinComponent {
    private lateinit var channel: SceytChannel
    private lateinit var pagerAdapter: ViewPagerAdapter
    private var binding: SceytActivityConversationInfoBinding? = null
    protected val viewModel: ConversationInfoViewModel by viewModels()

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityContentView()
        statusBarIconsColorWithBackground()

        getBundleArguments()
        initViewModel()
        binding?.initViews()
        setChannelDetails(channel)
        viewModel.getChannelFromServer(channel.id)
        setupPagerAdapter(binding?.viewPager, binding?.tabLayout)
        addAppBarOffsetChangeListener(binding?.appbar)
        viewModel.observeToChannelUpdate(channel.id)
        viewModel.onChannelEvent(channel.id)
        observeUserUpdateIfNeeded()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(intent.parcelable(CHANNEL))
    }

    private fun initViewModel() {
        viewModel.channelLiveData.observe(this) {
            channel = it
            onChannel(it)
        }

        viewModel.channelUpdatedLiveData.observe(this) {
            channel = it
            onChannel(it)
        }

        viewModel.onChannelDeletedLiveData.observe(this) {
            finish()
        }

        viewModel.onChannelLeftLiveData.observe(this) { data ->
            data.channel?.let {
                channel = it
                setChannelDetails(it)
            }
            if (!channel.isPublic())
                finish()
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

        viewModel.pinUnpinLiveData.observe(this) {
            channel.pinnedAt = it.pinnedAt
            onPinnedOrUnPinnedChannel(it)
        }

        viewModel.channelAddMemberLiveData.observe(this, ::onAddedMember)

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
    }

    private fun <T : Fragment?> initOrUpdateFragmentChannel(container: FragmentContainerView,
                                                            fragmentProvider: () -> T) {
        val (wasAdded, fragment) = getOrAddFragment(container, fragmentProvider)
        if (wasAdded && fragment?.isAdded == true)
            (fragment as? ChannelUpdateListener)?.onChannelUpdated(channel)
    }

    /** Generic function to either retrieve an existing fragment from a container or add a new one if not present.
     * The function returns a Pair<Boolean, T?> indicating whether the fragment was already present and the fragment instance.*/
    private fun <T : Fragment?> getOrAddFragment(container: FragmentContainerView,
                                                 fragmentProvider: () -> T): Pair<Boolean, T?> {
        val containerFragment = container.getFragment<T>()
        if (containerFragment != null) {
            return true to containerFragment
        } else {
            val fragment = fragmentProvider() ?: return false to null
            supportFragmentManager.commit(allowStateLoss = true) {
                replace(container.id, fragment, fragment.TAG_NAME)
            }
            return false to fragment
        }
    }

    open fun getLayoutDetailsCollapseMode(): Int {
        return CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
    }

    protected fun addAppBarOffsetChangeListener(appBar: AppBarLayout?) {
        var isShow = false
        var scrollRange = -1

        appBar?.post {
            appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                if (scrollRange + verticalOffset == 0) {
                    isShow = true
                    toggleToolbarViews(true)
                } else if (isShow) {
                    toggleToolbarViews(false)
                    isShow = false
                }
            }
        }
    }

    protected fun setupPagerAdapter(viewPager: ViewPager2?, tabLayout: TabLayout?) {
        val fragments = arrayListOf(
            getChannelMediaFragment(channel),
            getChannelFilesFragment(channel),
            getChannelVoiceFragment(channel),
            getChannelLinksFragment(channel),
        ).filterNotNull()

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
        setChannelToolbar(channel)
        setChannelSettings(channel)
        setChannelMembersByRoleButtons(channel)
        setChannelDescription(channel)
        setChannelInfo(channel)
        setChannelAdditionalInfoFragment(channel)
        setChannelSpecifications(channel)
    }

    open fun setActivityContentView() {
        setContentView(SceytActivityConversationInfoBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)
    }

    open fun onMembersClick(channel: SceytChannel) {
        val fragment = getChannelMembersFragment(channel, getMembersType()) ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    open fun onAdminsClick(channel: SceytChannel) {
        binding ?: return
        val fragment = getChannelMembersFragment(channel, MemberTypeEnum.Admin) ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    open fun onSearchMessagesClick(channel: SceytChannel) {
        val intent = Intent()
        intent.putExtra(ACTION_SEARCH_MESSAGES, true)
        setResult(RESULT_OK, intent)
        finish()
    }

    open fun onEditClick(channel: SceytChannel) {
        binding ?: return
        val fragment = getEditChannelFragment(channel) ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    open fun onAvatarClick(channel: SceytChannel) {
        val icon = channel.iconUrl
        if (!icon.isNullOrBlank()) {
            val title = if (channel.isDirect()) {
                val user = channel.getPeer()?.user
                if (user != null) SceytKitConfig.userNameBuilder?.invoke(user)
                        ?: user.getPresentableName() else null
            } else channel.subject

            SceytPhotoPreviewActivity.launchActivity(this, icon, title)
        }
    }

    open fun onClearHistoryClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmClearHistoryAction(this, channel) {
            clearHistory(channel.isPublic())
        }
    }

    open fun onLeaveChatClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmLeaveAction(this, channel) {
            leaveChannel()
        }
    }

    open fun onBlockUnBlockUserClick(channel: SceytChannel, block: Boolean) {
        val peer = channel.getPeer() ?: return
        if (block) {
            showSceytDialog(this, R.string.sceyt_block_user_title,
                R.string.sceyt_block_user_desc, R.string.sceyt_block, positiveCb = {
                    blockUser(peer.id)
                })
        } else unblockUser(peer.id)
    }

    open fun onDeleteChatClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmDeleteChatAction(this, channel) {
            deleteChannel()
        }
    }

    open fun onPinUnpinChatClick(channel: SceytChannel, pin: Boolean) {
        if (pin) {
            viewModel.pinChannel(channel.id)
        } else {
            viewModel.unpinChannel(channel.id)
        }
    }

    open fun onAddedMember(data: ChannelMembersEventData) {
    }

    open fun onMoreClick(channel: SceytChannel) {
        if (channel.isGroup) {
            GroupChatActionsDialog.newInstance(this, channel).apply {
                setChooseTypeCb(::onGroupChatMoreActionClick)
            }.show()
        } else
            DirectChatActionsDialog.newInstance(this, channel).apply {
                setChooseTypeCb(::onDirectChatMoreActionClick)
            }.show()
    }

    open fun onReportClick(channel: SceytChannel) {
    }

    open fun onDirectChatMoreActionClick(actionsEnum: DirectChatActionsDialog.ActionsEnum) {
        when (actionsEnum) {
            ClearHistory -> onClearHistoryClick(channel)
            BlockUser -> onBlockUnBlockUserClick(channel, true)
            UnBlockUser -> onBlockUnBlockUserClick(channel, false)
            Delete -> onDeleteChatClick(channel)
            Pin -> onPinUnpinChatClick(channel, true)
            UnPin -> onPinUnpinChatClick(channel, false)
        }
    }

    open fun onGroupChatMoreActionClick(actionsEnum: GroupChatActionsDialog.ActionsEnum) {
        when (actionsEnum) {
            GroupChatActionsDialog.ActionsEnum.ClearHistory -> onClearHistoryClick(channel)
            GroupChatActionsDialog.ActionsEnum.Leave -> onLeaveChatClick(channel)
            GroupChatActionsDialog.ActionsEnum.Delete -> onDeleteChatClick(channel)
            GroupChatActionsDialog.ActionsEnum.Pin -> onPinUnpinChatClick(channel, true)
            GroupChatActionsDialog.ActionsEnum.Unpin -> onPinUnpinChatClick(channel, false)
        }
    }

    open fun onChannel(channel: SceytChannel) {
        setChannelDetails(channel)
    }

    open fun onUserPresenceUpdated(presenceUser: SceytPresenceChecker.PresenceUser) {
        with(getBinding() ?: return) {
            (frameLayoutInfo.getFragment() as? InfoDetailsFragment)?.onUserPresenceUpdated(presenceUser)
            (frameLayoutToolbar.getFragment() as? InfoToolbarFragment)?.onUserPresenceUpdated(presenceUser)
        }
    }

    open fun onLeftChannel(channelId: Long) {
        finish()
    }

    open fun onDeletedChannel(channelId: Long) {
        finish()
    }

    open fun onMutedOrUnMutedChannel(sceytChannel: SceytChannel) {
        setChannelSettings(sceytChannel)
    }

    open fun onPinnedOrUnPinnedChannel(sceytChannel: SceytChannel) {
    }

    open fun onJoinedChannel(sceytChannel: SceytChannel) {
        setChannelDetails(sceytChannel)
    }

    open fun onClearedHistory(channelId: Long) {
        pagerAdapter.historyCleared()
        customToastSnackBar(getString(R.string.sceyt_history_was_successfully_cleared))
    }

    open fun onBlockedOrUnblockedUser(users: List<User>) {
        val peer = channel.getPeer()
        users.find { user -> user.id == peer?.id }?.let { user ->
            peer?.user = user
        }
    }

    open fun onMuteUnMuteClick(sceytChannel: SceytChannel, mute: Boolean) {
        if (mute.not()) {
            unMuteChannel()
        } else {
            ChannelActionConfirmationWithDialog.confirmMuteUntilAction(this) {
                muteChannel(it)
            }
        }
    }

    open fun setPagerAdapter(pagerAdapter: ViewPagerAdapter) {
        binding?.viewPager?.adapter = pagerAdapter
    }

    open fun toggleToolbarViews(showDetails: Boolean) {
        (binding?.frameLayoutToolbar?.getFragment() as? InfoToolbarFragment)?.toggleToolbarViews(showDetails)
        binding?.viewTopTabLayout?.isVisible = showDetails
    }

    open fun setChannelToolbar(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutToolbar ?: return) {
            getChannelToolbarDetailsFragment(channel).also {
                (it as? InfoToolbarFragment)?.setClickActionsListener { actionsEnum ->
                    when (actionsEnum) {
                        Back -> finish()
                        Edit -> onEditClick(this.channel)
                        More -> onMoreClick(this.channel)
                    }
                }
            }
        }
    }

    open fun setChannelSettings(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutSettings ?: return) {
            getChannelSettingsFragment(channel).also {
                (it as? InfoSettingsFragment)?.setClickActionsListener { actionsEnum ->
                    when (actionsEnum) {
                        InfoSettingsFragment.ClickActionsEnum.Mute -> onMuteUnMuteClick(this.channel, true)
                        InfoSettingsFragment.ClickActionsEnum.UnMute -> onMuteUnMuteClick(this.channel, false)
                        InfoSettingsFragment.ClickActionsEnum.AutoDeleteOn -> {}
                        InfoSettingsFragment.ClickActionsEnum.AutoDeleteOff -> {}
                    }
                }
            }
        }
    }

    open fun setChannelMembersByRoleButtons(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutMembersByRole ?: return) {
            getChannelMembersByRoleFragment(channel).also {
                (it as? InfoMembersByRoleButtonsFragment)?.setClickActionsListener { actionsEnum ->
                    when (actionsEnum) {
                        InfoMembersByRoleButtonsFragment.ClickActionsEnum.Admins -> onAdminsClick(this.channel)
                        InfoMembersByRoleButtonsFragment.ClickActionsEnum.Members -> onMembersClick(this.channel)
                        InfoMembersByRoleButtonsFragment.ClickActionsEnum.SearchMessages -> onSearchMessagesClick(this.channel)
                    }
                }
            }
        }
    }

    open fun setChannelDescription(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutDescription ?: return) {
            getChannelDescriptionFragment(channel)
        }
    }

    open fun setChannelInfo(channel: SceytChannel) {
        initOrUpdateFragmentChannel(binding?.frameLayoutInfo ?: return) {
            getChannelDetailsFragment(channel).also {
                (it as? InfoDetailsFragment)?.setClickActionsListener { actionsEnum ->
                    when (actionsEnum) {
                        InfoDetailsFragment.ClickActionsEnum.Avatar -> onAvatarClick(this.channel)
                    }
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

    //Toolbar
    open fun getChannelToolbarDetailsFragment(channel: SceytChannel): Fragment? = InfoToolbarFragment.newInstance(channel)

    open fun getChannelMembersFragment(channel: SceytChannel, memberType: MemberTypeEnum): Fragment? =
            ChannelMembersFragment.newInstance(channel, memberType)

    open fun getChannelMediaFragment(channel: SceytChannel): Fragment? = ChannelMediaFragment.newInstance(channel)

    open fun getChannelFilesFragment(channel: SceytChannel): Fragment? = ChannelFilesFragment.newInstance(channel)

    open fun getChannelLinksFragment(channel: SceytChannel): Fragment? = ChannelLinksFragment.newInstance(channel)

    open fun getChannelVoiceFragment(channel: SceytChannel): Fragment? = ChannelVoiceFragment.newInstance(channel)

    open fun getEditChannelFragment(channel: SceytChannel): Fragment? = EditChannelFragment.newInstance(channel)

    //Description
    open fun getChannelDescriptionFragment(channel: SceytChannel): Fragment? = InfoDescriptionFragment.newInstance(channel)

    open fun getChannelDetailsFragment(channel: SceytChannel): Fragment? = InfoDetailsFragment.newInstance(channel)

    open fun getChannelSettingsFragment(channel: SceytChannel): Fragment? = InfoSettingsFragment.newInstance(channel)

    //Members by role buttons
    open fun getChannelMembersByRoleFragment(channel: SceytChannel): Fragment? = InfoMembersByRoleButtonsFragment.newInstance(channel)

    //Additional info
    open fun getChannelAdditionalInfoFragment(channel: SceytChannel): Fragment? = null

    //Specifications
    open fun getChannelSpecificationsFragment(channel: SceytChannel): Fragment? = InfoSpecificationsFragment.newInstance(channel)

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

    companion object {
        const val CHANNEL = "CHANNEL"
        const val ACTION_SEARCH_MESSAGES = "ACTION_SEARCH_MESSAGES"

        fun launch(context: Context, channel: SceytChannel) {
            context.launchActivity<ConversationInfoActivity>(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold) {
                putExtra(CHANNEL, channel)
            }
        }

        fun startWithLauncher(context: Context, channel: SceytChannel, launcher: ActivityResultLauncher<Intent>) {
            val intent = context.createIntent<ConversationInfoActivity>().apply {
                putExtra(CHANNEL, channel)
            }
            val animOptions = ActivityOptionsCompat.makeCustomAnimation(context, R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
            launcher.launch(intent, animOptions)
        }
    }
}