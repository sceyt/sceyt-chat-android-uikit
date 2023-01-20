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
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.*
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.databinding.ActivityConversationInfoBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog.Companion.showSceytDialog
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsDirectChatFragment.ClickActionsEnum.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsPrivateChatFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsPrivateChatFragment.ClickActionsEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsPublicChannelFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments.InfoButtonsPublicChannelFragment.PublicChannelClickActionsEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.DirectChatActionsDialog.ActionsEnum.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.GroupChatActionsDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteNotificationDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteTypeEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.editchannel.EditChannelFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.MemberTypeEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.genMemberBy
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.voice.ChannelVoiceFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*
import java.util.concurrent.TimeUnit

open class ConversationInfoActivity : AppCompatActivity(), SceytKoinComponent {
    private lateinit var channel: SceytChannel
    private lateinit var pagerAdapter: ViewPagerAdapter
    private var binding: ActivityConversationInfoBinding? = null
    protected val viewModel: ConversationInfoViewModel by viewModel()
    private val preferences: SceytSharedPreference by inject()
    private var isSaveLoading = false
    private var avatarUrl: String? = null
    private var alphaAnimation: AlphaAnimation? = null

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
    }

    private fun getBundleArguments() {
        channel = requireNotNull(intent.getParcelableExtra(CHANNEL))
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

        viewModel.pageStateLiveData.observe(this, ::onPageStateChange)
    }

    private fun initButtons() {
        val fragment: Fragment = when (channel.channelType) {
            Direct -> {
                getInfoButtonsDirectChatFragment(channel).also {
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
        supportFragmentManager.commit {
            replace(R.id.layout_buttons, fragment)
        }
    }

    private fun observeToChannelUpdate() {
        ChannelsCash.channelUpdatedFlow
            .filter { it.channel.id == channel.id }
            .onEach {
                channel = it.channel
                onChannel(it.channel)
            }
            .launchIn(lifecycleScope)
    }

    private fun onButtonClick(clickActionsEnum: InfoButtonsDirectChatFragment.ClickActionsEnum) {
        when (clickActionsEnum) {
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

    private fun ActivityConversationInfoBinding.initViews() {
        icBack.setOnClickListener {
            onBackPressed()
        }

        members.setOnClickListener {
            onMembersClick(channel)
        }

        admins.setOnClickListener {
            onAdminsClick(channel)
        }

        icEdit.setOnClickListener {
            onEditClick(channel)
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

    private fun setupPagerAdapter(viewPager: ViewPager2?, tabLayout: TabLayout?) {
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

    private fun setChannelDetails(channel: SceytChannel) {
        avatarUrl = channel.iconUrl
        with(binding ?: return) {
            members.text = if (channel.channelType == Public)
                getString(R.string.sceyt_subscribers) else getString(R.string.sceyt_members)

            val isGroupOwner = (channel as? SceytGroupChannel)?.members?.find {
                it.id == preferences.getUserId()
            }?.role?.name == RoleTypeEnum.Owner.toString()

            admins.isVisible = isGroupOwner || channel.channelType == Private
            groupChannelMembers.isVisible = isGroupOwner || channel.channelType == Private
            icEdit.isVisible = isGroupOwner

            setChannelTitle(channel)
            setPresenceOrMembers(channel)
            setChannelDescription(channel)
            setChannelAvatar(channel)
        }
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

    protected fun clearHistory() {
        viewModel.clearHistory(channel.id)
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
            when (channel.channelType) {
                Private, Direct -> MemberTypeEnum.Member
                Public -> MemberTypeEnum.Subscriber
            }
        } else MemberTypeEnum.Member
    }

    open fun setActivityContentView() {
        setContentView(ActivityConversationInfoBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)
    }

    open fun onMembersClick(channel: SceytChannel) {
        binding ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(ChannelMembersFragment::class.java.simpleName)
            replace(R.id.rootFrameLayout, getChannelMembersFragment(channel, getMembersType()))
        }
    }

    open fun onAdminsClick(channel: SceytChannel) {
        binding ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(ChannelMembersFragment::class.java.simpleName)
            replace(R.id.rootFrameLayout, getChannelMembersFragment(channel, MemberTypeEnum.Admin))
        }
    }

    open fun onEditClick(channel: SceytChannel) {
        binding ?: return
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.sceyt_anim_slide_in_right, 0, 0, R.anim.sceyt_anim_slide_out_right)
            addToBackStack(EditChannelFragment::class.java.simpleName)
            replace(R.id.rootFrameLayout, getEditChannelFragment(channel))
        }
    }

    open fun onClearHistoryClick(channel: SceytChannel) {
        val descId: Int = when (channel.channelType) {
            Direct -> R.string.sceyt_clear_direct_history_desc
            else -> R.string.sceyt_clear_group_history_desc
        }
        showSceytDialog(this, R.string.sceyt_clear_history_title, descId, R.string.sceyt_clear) {
            clearHistory()
        }
    }

    open fun onLeaveChatClick(channel: SceytChannel) {
        val titleId: Int
        val descId: Int
        when (channel.channelType) {
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
        showSceytDialog(this, titleId, descId, R.string.sceyt_leave) {
            leaveChannel()
        }
    }

    open fun onBlockUnBlockUserClick(channel: SceytChannel, block: Boolean) {
        val peer = (channel as? SceytDirectChannel)?.peer ?: return
        if (block) {
            showSceytDialog(this, R.string.sceyt_block_user_title, R.string.sceyt_block_user_desc, R.string.sceyt_block) {
                blockUser(peer.id)
            }
        } else unblockUser(peer.id)
    }

    open fun onDeleteChatClick(channel: SceytChannel) {
        val titleId: Int
        val descId: Int
        when (channel.channelType) {
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
        showSceytDialog(this, titleId, descId, R.string.sceyt_delete) {
            deleteChannel()
        }
    }

    open fun onVideoCallClick(channel: SceytChannel) {

    }

    open fun onAudioCallClick(channel: SceytChannel) {

    }

    open fun onCallOutClick(channel: SceytChannel) {

    }

    open fun onMoreClick(channel: SceytChannel) {
        if (channel.isGroup) {
            GroupChatActionsDialog.newInstance(this, channel).apply {
                setChooseTypeCb(::onGroupChatMoreActionClick)
            }.show()
        } else
            DirectChatActionsDialog.newInstance(this, (channel as SceytDirectChannel)).apply {
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
        setChannelDetails(channel)
        initButtons()
        pagerAdapter.getFragment().find { fragment -> fragment is ChannelMembersFragment }?.let { membersFragment ->
            (membersFragment as ChannelMembersFragment).updateChannel(channel)
        }
    }

    open fun onLeaveChannel(channelId: Long) {
        onBackPressed()
    }

    open fun onDeleteChannel(channelId: Long) {
        onBackPressed()
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
        val peer = (channel as SceytDirectChannel).peer
        users.find { user -> user.id == peer?.id }?.let { user ->
            (channel as SceytDirectChannel).peer = genMemberBy(user).toSceytMember()
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
            subject.text = channel.channelSubject
            titleToolbar.text = channel.channelSubject
        }
    }

    open fun setPresenceOrMembers(channel: SceytChannel) {
        with(binding ?: return) {
            val title: String = if (channel is SceytDirectChannel) {
                val member = channel.peer ?: return
                if (member.user.presence?.state == PresenceState.Online) {
                    getString(R.string.sceyt_online)
                } else {
                    member.user.presence?.lastActiveAt?.let {
                        if (it != 0L)
                            DateTimeUtil.getPresenceDateFormatData(this@ConversationInfoActivity, Date(it))
                        else ""
                    } ?: ""
                }
            } else {
                if (channel.channelType == Private)
                    getString(R.string.sceyt_members_count, (channel as SceytGroupChannel).memberCount)
                else getString(R.string.sceyt_subscribers_count, (channel as SceytGroupChannel).memberCount)
            }
            tvPresenceOrMembers.text = title
            subTitleToolbar.text = title
        }
    }

    open fun setChannelAvatar(channel: SceytChannel) {
        with(binding ?: return) {
            avatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl)
            toolbarAvatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl)
        }
    }

    open fun setChannelDescription(channel: SceytChannel) {
        with(binding ?: return) {
            if (channel is SceytDirectChannel) {
                val status = channel.peer?.user?.presence?.status
                        ?: SceytKitConfig.presenceStatusText
                if (status.isNotNullOrBlank()) {
                    tvTitle.text = getString(R.string.sceyt_about)
                    tvDescription.text = status
                } else groupChannelDescription.isVisible = false
            } else {
                if (channel.label.isNotNullOrBlank()) {
                    tvTitle.text = getString(R.string.sceyt_description)
                    tvDescription.text = channel.label
                    groupChannelDescription.isVisible = true
                } else groupChannelDescription.isVisible = false
            }
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
    open fun getInfoButtonsDirectChatFragment(channel: SceytChannel) = InfoButtonsDirectChatFragment.newInstance(channel)

    open fun getInfoButtonsPrivateChatFragment(channel: SceytChannel) = InfoButtonsPrivateChatFragment.newInstance(channel)

    open fun getInfoButtonsPublicChannelFragment(channel: SceytChannel) = InfoButtonsPublicChannelFragment.newInstance(channel)

    open fun onPageStateChange(pageState: PageState) {
        if (pageState is PageState.StateError) {
            setChannelDetails(channel)
            isSaveLoading = false
            customToastSnackBar(binding?.root, pageState.errorMessage.toString())
        }
    }

    open fun getViewPagerY(): Int {
        return (binding?.appbar?.height ?: 0)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
    }

    private fun ActivityConversationInfoBinding.setupStyle() {
        icBack.imageTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
        icEdit.imageTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<ConversationInfoActivity> {
                putExtra(CHANNEL, channel)
            }
            context.asActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }
}