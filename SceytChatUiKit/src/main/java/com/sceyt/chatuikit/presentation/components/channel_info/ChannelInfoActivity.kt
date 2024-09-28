package com.sceyt.chatuikit.presentation.components.channel_info

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
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Direct
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Group
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Public
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytActivityChannelInfoBinding
import com.sceyt.chatuikit.extensions.TAG_NAME
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.common.SceytDialog.Companion.showDialog
import com.sceyt.chatuikit.presentation.components.channel_info.description.ChannelInfoDescriptionFragment
import com.sceyt.chatuikit.presentation.components.channel_info.details.ChannelInfoDetailsFragment
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.DirectChatActionsDialog
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.DirectChatActionsDialog.ActionsEnum.BlockUser
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.DirectChatActionsDialog.ActionsEnum.ClearHistory
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.DirectChatActionsDialog.ActionsEnum.Delete
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.DirectChatActionsDialog.ActionsEnum.Pin
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.DirectChatActionsDialog.ActionsEnum.UnBlockUser
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.DirectChatActionsDialog.ActionsEnum.UnPin
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.GroupChatActionsDialog
import com.sceyt.chatuikit.presentation.components.channel_info.edit.ChannelEditFragment
import com.sceyt.chatuikit.presentation.components.channel_info.files.ChannelInfoFilesFragment
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.presentation.components.channel_info.media.ChannelInfoMediaFragment
import com.sceyt.chatuikit.presentation.components.channel_info.members.ChannelMembersFragment
import com.sceyt.chatuikit.presentation.components.channel_info.members.MemberTypeEnum
import com.sceyt.chatuikit.presentation.components.channel_info.options.ChannelInfoOptionsFragment
import com.sceyt.chatuikit.presentation.components.channel_info.preview.ImagePreviewActivity
import com.sceyt.chatuikit.presentation.components.channel_info.settings.ChannelInfoSettingsFragment
import com.sceyt.chatuikit.presentation.components.channel_info.toolbar.ChannelInfoToolbarFragment
import com.sceyt.chatuikit.presentation.components.channel_info.toolbar.ChannelInfoToolbarFragment.ClickActionsEnum.Back
import com.sceyt.chatuikit.presentation.components.channel_info.toolbar.ChannelInfoToolbarFragment.ClickActionsEnum.Edit
import com.sceyt.chatuikit.presentation.components.channel_info.toolbar.ChannelInfoToolbarFragment.ClickActionsEnum.More
import com.sceyt.chatuikit.presentation.components.channel_info.uri.ChannelInfoURIFragment
import com.sceyt.chatuikit.presentation.components.channel_info.viewmodel.ChannelInfoViewModel
import com.sceyt.chatuikit.presentation.components.channel_info.voice.ChannelInfoVoiceFragment
import com.sceyt.chatuikit.presentation.components.channel_list.channels.dialogs.ChannelActionConfirmationWithDialog
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.services.SceytPresenceChecker.PresenceUser
import com.sceyt.chatuikit.styles.ChannelInfoStyle

@Suppress("MemberVisibilityCanBePrivate")
open class ChannelInfoActivity : AppCompatActivity(), SceytKoinComponent {
    protected lateinit var pagerAdapter: ViewPagerAdapter
        private set
    protected val viewModel: ChannelInfoViewModel by viewModels()
    protected lateinit var style: ChannelInfoStyle
    protected var binding: SceytActivityChannelInfoBinding? = null
        private set
    lateinit var channel: SceytChannel
        private set

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = ChannelInfoStyle.Builder(this, null).build()
        setActivityContentView()
        statusBarIconsColorWithBackground()

        getBundleArguments()
        initViewModel()
        initViews()
        applyStyle()
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

        viewModel.leaveChannelLiveData.observe(this, ::onLeftChannel)

        viewModel.deleteChannelLiveData.observe(this, ::onDeletedChannel)

        viewModel.clearHistoryLiveData.observe(this, ::onClearedHistory)

        viewModel.blockUnblockUserLiveData.observe(this, ::onBlockedOrUnblockedUser)

        viewModel.userPresenceUpdateLiveData.observe(this) { (channel, presenceUser) ->
            this.channel = channel
            onUserPresenceUpdated(channel, presenceUser)
        }

        viewModel.joinLiveData.observe(this) {
            channel = it
            onJoinedChannel(it)
        }

        viewModel.muteUnMuteLiveData.observe(this) {
            channel = channel.copy(muted = it.muted)
            onMutedOrUnMutedChannel(it)
        }

        viewModel.autoDeleteLiveData.observe(this) {
            channel = channel.copy(messageRetentionPeriod = it.messageRetentionPeriod)
            onAutoDeletedModeOnOrOff(it)
        }

        viewModel.pinUnpinLiveData.observe(this) {
            channel = channel.copy(pinnedAt = it.pinnedAt)
            onPinnedOrUnPinnedChannel(it)
        }

        viewModel.channelAddMemberLiveData.observe(this, ::onAddedMember)

        viewModel.pageStateLiveData.observe(this, ::onPageStateChanged)
    }

    private fun observeUserUpdateIfNeeded() {
        if (channel.isDirect())
            viewModel.observeUserPresenceUpdate(channel)
    }

    private fun initViews() {
        (binding?.layoutDetails?.layoutParams as? CollapsingToolbarLayout.LayoutParams)?.let {
            it.collapseMode = getLayoutDetailsCollapseMode()
        }
    }

    private fun <T : Fragment?> initOrUpdateFragment(container: FragmentContainerView,
                                                     fragmentProvider: () -> T): T? {
        val (wasAdded, fragment) = getOrAddFragment(container, fragmentProvider)
        if (wasAdded && fragment?.isAdded == true)
            (fragment as? ChannelUpdateListener)?.onChannelUpdated(channel)

        (fragment as? ChannelInfoStyleApplier)?.setStyle(style)
        return fragment
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

    protected open fun getLayoutDetailsCollapseMode(): Int {
        return CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
    }

    protected open fun addAppBarOffsetChangeListener(appBar: AppBarLayout?) {
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

    protected open fun setupPagerAdapter(viewPager: ViewPager2?, tabLayout: TabLayout?) {
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

    protected open fun setupTabLayout(tabLayout: TabLayout, viewPager: ViewPager2) {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getTagByPosition(position)
        }.attach()

        tabLayout.apply {
            val color = getCompatColor(SceytChatUIKit.theme.colors.accentColor)
            setSelectedTabIndicatorColor(color)
            tabRippleColor = ColorStateList.valueOf(color)
        }
    }

    protected open fun clearHistory(forEveryone: Boolean) {
        viewModel.clearHistory(channel.id, forEveryone)
    }

    protected open fun leaveChannel() {
        viewModel.leaveChannel(channel.id)
    }

    protected open fun blockAndLeaveChannel() {
        viewModel.blockAndLeaveChannel(channel.id)
    }

    protected open fun blockUser(userId: String) {
        viewModel.blockUser(userId)
    }

    protected open fun unblockUser(userId: String) {
        viewModel.unblockUser(userId)
    }

    protected open fun deleteChannel() {
        viewModel.deleteChannel(channel.id)
    }

    protected open fun muteChannel(until: Long) {
        viewModel.muteChannel(channel.id, until)
    }

    protected open fun unMuteChannel() {
        viewModel.unMuteChannel(channel.id)
    }

    protected open fun enableAutoDelete(period: Long) {
        viewModel.enableAutoDelete(channel.id, period)
    }

    protected open fun disableAutoDelete() {
        viewModel.disableAutoDelete(channel.id)
    }

    protected open fun addMembers(members: List<SceytMember>) {
        viewModel.addMembersToChannel(channel.id, members)
    }

    protected open fun getMembersType(): MemberTypeEnum {
        return if (::channel.isInitialized) {
            when (channel.getChannelType()) {
                Direct, Group -> MemberTypeEnum.Member
                Public -> MemberTypeEnum.Subscriber
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

    protected open fun setActivityContentView() {
        setContentView(SceytActivityChannelInfoBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)
    }

    protected open fun onMembersClick(channel: SceytChannel) {
        val fragment = getChannelMembersFragment(channel, getMembersType()) ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    protected open fun onAdminsClick(channel: SceytChannel) {
        binding ?: return
        val fragment = getChannelMembersFragment(channel, MemberTypeEnum.Admin) ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    protected open fun onSearchMessagesClick(channel: SceytChannel) {
        val intent = Intent()
        intent.putExtra(ACTION_SEARCH_MESSAGES, true)
        setResult(RESULT_OK, intent)
        finish()
    }

    protected open fun onEditClick(channel: SceytChannel) {
        binding ?: return
        val fragment = getEditChannelFragment(channel) ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(fragment.TAG_NAME)
            replace(getRootFragmentId(), fragment, fragment.TAG_NAME)
        }
    }

    protected open fun onAvatarClick(channel: SceytChannel) {
        val icon = channel.iconUrl
        if (!icon.isNullOrBlank()) {
            val title = if (channel.isDirect()) {
                val user = channel.getPeer()?.user
                if (user != null) SceytChatUIKit.formatters.userNameFormatter?.format(user)
                        ?: user.getPresentableName() else null
            } else channel.subject

            ImagePreviewActivity.launchActivity(this, icon, title)
        }
    }

    protected open fun onClearHistoryClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmClearHistoryAction(this, channel) {
            clearHistory(channel.isPublic())
        }
    }

    protected open fun onLeaveChatClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmLeaveAction(this, channel) {
            leaveChannel()
        }
    }

    protected open fun onBlockUnBlockUserClick(channel: SceytChannel, block: Boolean) {
        val peer = channel.getPeer() ?: return
        if (block) {
            showDialog(this, R.string.sceyt_block_user_title,
                R.string.sceyt_block_user_desc, R.string.sceyt_block, positiveCb = {
                    blockUser(peer.id)
                })
        } else unblockUser(peer.id)
    }

    protected open fun onDeleteChatClick(channel: SceytChannel) {
        ChannelActionConfirmationWithDialog.confirmDeleteChatAction(this, channel) {
            deleteChannel()
        }
    }

    protected open fun onPinUnpinChatClick(channel: SceytChannel, pin: Boolean) {
        if (pin) {
            viewModel.pinChannel(channel.id)
        } else {
            viewModel.unpinChannel(channel.id)
        }
    }

    protected open fun onAddedMember(data: ChannelMembersEventData) {
    }

    protected open fun onMoreClick(channel: SceytChannel) {
        if (channel.isGroup) {
            GroupChatActionsDialog.newInstance(this, channel).apply {
                setChooseTypeCb(::onGroupChatMoreActionClick)
            }.show()
        } else
            DirectChatActionsDialog.newInstance(this, channel).apply {
                setChooseTypeCb(::onDirectChatMoreActionClick)
            }.show()
    }

    protected open fun onReportClick(channel: SceytChannel) {
    }

    protected open fun onDirectChatMoreActionClick(actionsEnum: DirectChatActionsDialog.ActionsEnum) {
        when (actionsEnum) {
            ClearHistory -> onClearHistoryClick(channel)
            BlockUser -> onBlockUnBlockUserClick(channel, true)
            UnBlockUser -> onBlockUnBlockUserClick(channel, false)
            Delete -> onDeleteChatClick(channel)
            Pin -> onPinUnpinChatClick(channel, true)
            UnPin -> onPinUnpinChatClick(channel, false)
        }
    }

    protected open fun onGroupChatMoreActionClick(actionsEnum: GroupChatActionsDialog.ActionsEnum) {
        when (actionsEnum) {
            GroupChatActionsDialog.ActionsEnum.ClearHistory -> onClearHistoryClick(channel)
            GroupChatActionsDialog.ActionsEnum.Leave -> onLeaveChatClick(channel)
            GroupChatActionsDialog.ActionsEnum.Delete -> onDeleteChatClick(channel)
            GroupChatActionsDialog.ActionsEnum.Pin -> onPinUnpinChatClick(channel, true)
            GroupChatActionsDialog.ActionsEnum.Unpin -> onPinUnpinChatClick(channel, false)
        }
    }

    protected open fun onChannel(channel: SceytChannel) {
        setChannelDetails(channel)
    }

    protected open fun onUserPresenceUpdated(channel: SceytChannel, presenceUser: PresenceUser) {
        with(binding ?: return) {
            (frameLayoutInfo.getFragment() as? ChannelInfoDetailsFragment)?.onUserPresenceUpdated(presenceUser)
            (frameLayoutToolbar.getFragment() as? ChannelInfoToolbarFragment)?.onUserPresenceUpdated(presenceUser)
        }
    }

    protected open fun onLeftChannel(channelId: Long) {
        finish()
    }

    protected open fun onDeletedChannel(channelId: Long) {
        finish()
    }

    protected open fun onMutedOrUnMutedChannel(sceytChannel: SceytChannel) {
        setChannelSettings(sceytChannel)
    }

    protected open fun onAutoDeletedModeOnOrOff(sceytChannel: SceytChannel) {
        setChannelSettings(sceytChannel)
    }

    protected open fun onPinnedOrUnPinnedChannel(sceytChannel: SceytChannel) {
    }

    protected open fun onJoinedChannel(sceytChannel: SceytChannel) {
        setChannelDetails(sceytChannel)
    }

    protected open fun onClearedHistory(channelId: Long) {
        pagerAdapter.historyCleared()
        customToastSnackBar(getString(R.string.sceyt_history_was_successfully_cleared))
    }

    protected open fun onBlockedOrUnblockedUser(users: List<SceytUser>) {
        val members = channel.members?.toArrayList() ?: return
        users.forEach { user ->
            members.findIndexed { it.id == user.id }?.let { (index, member) ->
                members[index] = member.copy(user = user.copy())
            }
        }
        channel = channel.copy(members = members)
    }

    protected open fun onMuteUnMuteClick(sceytChannel: SceytChannel, mute: Boolean) {
        if (mute.not()) {
            unMuteChannel()
        } else {
            ChannelActionConfirmationWithDialog.confirmMuteUntilAction(this) {
                muteChannel(it)
            }
        }
    }

    protected open fun onAutoDeleteOnOffClick(sceytChannel: SceytChannel, autoDelete: Boolean) {
        if (autoDelete.not()) {
            disableAutoDelete()
        } else {
            ChannelActionConfirmationWithDialog.confirmAutoDeleteMessages(this) {
                enableAutoDelete(it)
            }
        }
    }

    protected open fun setPagerAdapter(pagerAdapter: ViewPagerAdapter) {
        binding?.viewPager?.adapter = pagerAdapter
    }

    protected open fun toggleToolbarViews(showDetails: Boolean) {
        (binding?.frameLayoutToolbar?.getFragment() as? ChannelInfoToolbarFragment)?.toggleToolbarViews(showDetails)
        binding?.viewTopTabLayout?.isVisible = showDetails
    }

    protected open fun setChannelToolbar(channel: SceytChannel) {
        initOrUpdateFragment(binding?.frameLayoutToolbar ?: return) {
            getChannelToolbarDetailsFragment(channel)
        }.also {
            (it as? ChannelInfoToolbarFragment)?.setClickActionsListener { actionsEnum ->
                when (actionsEnum) {
                    Back -> finish()
                    Edit -> onEditClick(this.channel)
                    More -> onMoreClick(this.channel)
                }
            }
        }
    }

    protected open fun setChannelSettings(channel: SceytChannel) {
        initOrUpdateFragment(binding?.frameLayoutSettings ?: return) {
            getChannelSettingsFragment(channel)
        }.also {
            (it as? ChannelInfoSettingsFragment)?.setClickActionsListener { actionsEnum ->
                when (actionsEnum) {
                    ChannelInfoSettingsFragment.ClickActionsEnum.Mute -> onMuteUnMuteClick(this.channel, true)
                    ChannelInfoSettingsFragment.ClickActionsEnum.UnMute -> onMuteUnMuteClick(this.channel, false)
                    ChannelInfoSettingsFragment.ClickActionsEnum.AutoDeleteOn -> onAutoDeleteOnOffClick(this.channel, true)
                    ChannelInfoSettingsFragment.ClickActionsEnum.AutoDeleteOff -> onAutoDeleteOnOffClick(this.channel, false)
                }
            }
        }
    }

    protected open fun setChannelMembersByRoleButtons(channel: SceytChannel) {
        initOrUpdateFragment(binding?.frameLayoutMembersByRole ?: return) {
            getChannelMembersByRoleFragment(channel)
        }.also {
            (it as? ChannelInfoOptionsFragment)?.setClickActionsListener { actionsEnum ->
                when (actionsEnum) {
                    ChannelInfoOptionsFragment.ClickActionsEnum.Admins -> onAdminsClick(this.channel)
                    ChannelInfoOptionsFragment.ClickActionsEnum.Members -> onMembersClick(this.channel)
                    ChannelInfoOptionsFragment.ClickActionsEnum.SearchMessages -> onSearchMessagesClick(this.channel)
                }
            }
        }
    }

    protected open fun setChannelDescription(channel: SceytChannel) {
        initOrUpdateFragment(binding?.frameLayoutDescription ?: return) {
            getChannelDescriptionFragment(channel)
        }
    }

    protected open fun setChannelInfo(channel: SceytChannel) {
        initOrUpdateFragment(binding?.frameLayoutInfo ?: return) {
            getChannelDetailsFragment(channel)
        }.also {
            (it as? ChannelInfoDetailsFragment)?.setClickActionsListener { actionsEnum ->
                when (actionsEnum) {
                    ChannelInfoDetailsFragment.ClickActionsEnum.Avatar -> onAvatarClick(this.channel)
                }
            }
        }
    }

    protected open fun setChannelSpecifications(channel: SceytChannel) {
        initOrUpdateFragment(binding?.frameLayoutSpecifications ?: return) {
            getChannelSpecificationsFragment(channel)
        }
    }

    protected open fun setChannelAdditionalInfoFragment(channel: SceytChannel) {
        binding ?: return
        val fragment = getChannelAdditionalInfoFragment(channel) ?: return
        supportFragmentManager.commit(allowStateLoss = true) {
            replace(R.id.frame_layout_additional_info, fragment, fragment.TAG_NAME)
        }
    }

    //Toolbar
    protected open fun getChannelToolbarDetailsFragment(channel: SceytChannel): Fragment? = ChannelInfoToolbarFragment.newInstance(channel)

    protected open fun getChannelMembersFragment(channel: SceytChannel, memberType: MemberTypeEnum): Fragment? =
            ChannelMembersFragment.newInstance(channel, memberType)

    protected open fun getChannelMediaFragment(channel: SceytChannel): Fragment? = ChannelInfoMediaFragment.newInstance(channel)

    protected open fun getChannelFilesFragment(channel: SceytChannel): Fragment? = ChannelInfoFilesFragment.newInstance(channel)

    protected open fun getChannelLinksFragment(channel: SceytChannel): Fragment? = ChannelInfoLinksFragment.newInstance(channel)

    protected open fun getChannelVoiceFragment(channel: SceytChannel): Fragment? = ChannelInfoVoiceFragment.newInstance(channel)

    protected open fun getEditChannelFragment(channel: SceytChannel): Fragment? = ChannelEditFragment.newInstance(channel)

    //Description
    protected open fun getChannelDescriptionFragment(channel: SceytChannel): Fragment? = ChannelInfoDescriptionFragment.newInstance(channel)

    protected open fun getChannelDetailsFragment(channel: SceytChannel): Fragment? = ChannelInfoDetailsFragment.newInstance(channel)

    protected open fun getChannelSettingsFragment(channel: SceytChannel): Fragment? = ChannelInfoSettingsFragment.newInstance(channel)

    //Members by role buttons
    protected open fun getChannelMembersByRoleFragment(channel: SceytChannel): Fragment? =
            ChannelInfoOptionsFragment.newInstance(channel, intent.getBooleanExtra(ENABLE_SEARCH_MESSAGES, false))

    //Additional info
    protected open fun getChannelAdditionalInfoFragment(channel: SceytChannel): Fragment? = null

    //Specifications
    protected open fun getChannelSpecificationsFragment(channel: SceytChannel): Fragment? = ChannelInfoURIFragment.newInstance(channel)

    protected open fun onPageStateChanged(pageState: PageState) {
        if (pageState is PageState.StateError) {
            setChannelDetails(channel)
            if (pageState.showMessage)
                customToastSnackBar(pageState.errorMessage.toString())
        }
    }

    open fun getViewPagerY(): Int {
        return (binding?.appbar?.height ?: 0)
    }

    protected open fun getRootFragmentId(): Int = R.id.rootFrameLayout

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    protected open fun applyStyle() {
        with(binding ?: return) {
            val theme = SceytChatUIKit.theme.colors
            root.setBackgroundColor(getCompatColor(theme.backgroundColorSecondary))
            viewTopTabLayout.setBackgroundTintColorRes(theme.borderColor)
            underlineTab.setBackgroundTintColorRes(theme.borderColor)
            tabLayout.setBackgroundColor(getCompatColor(theme.backgroundColorSections))
            tabLayout.setTabTextColors(getCompatColor(theme.textSecondaryColor), getCompatColor(theme.textPrimaryColor))
        }
    }

    companion object {
        const val CHANNEL = "CHANNEL"
        const val ACTION_SEARCH_MESSAGES = "ACTION_SEARCH_MESSAGES"
        private const val ENABLE_SEARCH_MESSAGES = "ACTION_SEARCH_MESSAGES"

        fun launch(context: Context, channel: SceytChannel) {
            context.launchActivity<ChannelInfoActivity>(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold) {
                putExtra(CHANNEL, channel)
                putExtra(ENABLE_SEARCH_MESSAGES, false)
            }
        }

        fun startHandleSearchClick(context: Context, channel: SceytChannel, launcher: ActivityResultLauncher<Intent>) {
            val intent = context.createIntent<ChannelInfoActivity>().apply {
                putExtra(CHANNEL, channel)
                putExtra(ENABLE_SEARCH_MESSAGES, true)
            }
            val animOptions = ActivityOptionsCompat.makeCustomAnimation(context, R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
            launcher.launch(intent, animOptions)
        }
    }
}