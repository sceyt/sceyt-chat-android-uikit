package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.databinding.ActivityConversationInfoBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog.Companion.showSceytDialog
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteNotificationDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteTypeEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments.DirectChantButtonsActionsDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments.DirectChantButtonsActionsDialog.ActionsEnum.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments.GroupChantButtonsActionsDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments.InfoButtonsDirectChatFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments.InfoButtonsDirectChatFragment.ClickActionsEnum.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments.InfoButtonsGroupChatFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments.InfoButtonsGroupChatFragment.ClickActionsEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.genMemberBy
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.voice.ChannelVoiceFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.util.*
import java.util.concurrent.TimeUnit


open class ConversationInfoActivity : AppCompatActivity() {
    private lateinit var channel: SceytChannel
    private lateinit var pagerAdapter: ViewPagerAdapter
    private var binding: ActivityConversationInfoBinding? = null
    private val viewModel: ConversationInfoViewModel by viewModels()
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
        binding?.initViews()
        setChannelDetails(channel)
        viewModel.getChannelFromServer(channel.id)
        setupPagerAdapter(binding?.viewPager, binding?.tabLayout)
        addAppBarOffsetChangeListener(binding?.appbar)
        initButtons()
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

        viewModel.muteUnMuteLiveData.observe(this) {
            channel.muted = it.muted
            initButtons()
            onMuteUnMuteChannel(it)
        }

        viewModel.pageStateLiveData.observe(this, ::onPageStateChange)
    }

    private fun initButtons() {
        val fragment: Fragment = when (channel) {
            is SceytGroupChannel -> {
                InfoButtonsGroupChatFragment.newInstance(channel).also {
                    it.setClickActionsListener(::onGroupButtonClick)
                }
            }
            else -> {
                InfoButtonsDirectChatFragment.newInstance(channel).also {
                    it.setClickActionsListener(::onButtonClick)
                }
            }
        }
        supportFragmentManager.commit {
            replace(R.id.layout_buttons, fragment)
        }
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
            ClickActionsEnum.Report -> onReportClick(channel)
            ClickActionsEnum.Join -> onJoinClick(channel)
            ClickActionsEnum.Add -> onAddClick(channel)
            ClickActionsEnum.More -> onMoreClick(channel)
        }
    }

    private fun ActivityConversationInfoBinding.initViews() {
        icBack.imageTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))

        /*
         leaveChannel.setOnClickListener {
             showSceytDialog(com.sceyt.sceytchatuikit.R.string.sceyt_leave_channel_title, com.sceyt.sceytchatuikit.R.string.sceyt_leave_channel_desc, com.sceyt.sceytchatuikit.R.string.sceyt_leave) {
                 leaveChannel()
             }
         }*/

        /*blockAndLeaveChannel.setOnClickListener {
            showSceytDialog(com.sceyt.sceytchatuikit.R.string.sceyt_block_and_leave_channel_title, com.sceyt.sceytchatuikit.R.string.sceyt_block_and_leave_channel_desc, com.sceyt.sceytchatuikit.R.string.sceyt_leave) {
                blockAndLeaveChannel()
            }
        }
*/
        icBack.setOnClickListener {
            onBackPressed()
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
            getChannelLinksFragment(channel),
            getChannelVoiceFragment(channel),
        )
        /*  if (channel.channelType != ChannelTypeEnum.Direct)
              fragments.add(0, getChannelMembersFragment(channel))*/

        pagerAdapter = ViewPagerAdapter(this, fragments)

        setPagerAdapter(pagerAdapter)
        setupTabLayout(tabLayout ?: return, viewPager ?: return)
    }

    private fun setAvatarImage(filePath: String?) {
        avatarUrl = filePath
        binding?.avatar?.setImageUrl(filePath)
    }

    private fun setChannelDetails(channel: SceytChannel) {
        avatarUrl = channel.iconUrl
        with(binding ?: return) {
            avatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl)
            toolbarAvatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl)

            setChannelTitle(channel)
            setPresenceOrMembers(channel)
            setChannelDescription(channel)
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

    protected fun getChannel() = channel.clone()

    open fun setActivityContentView() {
        setContentView(ActivityConversationInfoBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)
    }

    open fun onClearHistoryClick(channel: SceytChannel) {
        showSceytDialog(this, R.string.sceyt_clear_history_title, R.string.sceyt_clear_history_desc, R.string.sceyt_clear) {
            clearHistory()
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
        showSceytDialog(this, R.string.sceyt_delete_channel_title, R.string.sceyt_delete_channel_desc, R.string.sceyt_delete) {
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
            GroupChantButtonsActionsDialog.newInstance(this, channel).apply {
                setChooseTypeCb(::onGroupChatMoreActionClick)
            }.show()
        } else
            DirectChantButtonsActionsDialog.newInstance(this, (channel as SceytDirectChannel)).apply {
                setChooseTypeCb(::onDirectChatMoreActionClick)
            }.show()
    }

    open fun onJoinClick(channel: SceytChannel) {

    }

    open fun onAddClick(channel: SceytChannel) {

    }

    open fun onReportClick(channel: SceytChannel) {

    }

    open fun onDirectChatMoreActionClick(actionsEnum: DirectChantButtonsActionsDialog.ActionsEnum) {
        when (actionsEnum) {
            ClearHistory -> onClearHistoryClick(channel)
            BlockUser -> onBlockUnBlockUserClick(channel, true)
            UnBlockUser -> onBlockUnBlockUserClick(channel, false)
            Delete -> onDeleteChatClick(channel)
        }
    }

    open fun onGroupChatMoreActionClick(actionsEnum: GroupChantButtonsActionsDialog.ActionsEnum) {
        when (actionsEnum) {
            GroupChantButtonsActionsDialog.ActionsEnum.ClearHistory -> onClearHistoryClick(channel)
            GroupChantButtonsActionsDialog.ActionsEnum.Delete -> onDeleteChatClick(channel)
        }
    }

    open fun onChannel(channel: SceytChannel) {
        setChannelDetails(channel)
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
                if (channel.channelType == ChannelTypeEnum.Private)
                    getString(R.string.sceyt_members_count, (channel as SceytGroupChannel).memberCount)
                else getString(R.string.sceyt_subscribers_count, (channel as SceytGroupChannel).memberCount)
            }
            tvPresenceOrMembers.text = title
            subTitleToolbar.text = title
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
                    tvTitle.text = getString(R.string.sceyt_channel_description)
                    tvDescription.text = channel.label
                } else groupChannelDescription.isVisible = false
            }
        }
    }

    open fun getChannelMembersFragment(channel: SceytChannel) = ChannelMembersFragment.newInstance(channel)

    open fun getChannelMediaFragment(channel: SceytChannel) = ChannelMediaFragment.newInstance(channel)

    open fun getChannelFilesFragment(channel: SceytChannel) = ChannelFilesFragment.newInstance(channel)

    open fun getChannelLinksFragment(channel: SceytChannel) = ChannelLinksFragment.newInstance(channel)

    open fun getChannelVoiceFragment(channel: SceytChannel) = ChannelVoiceFragment.newInstance(channel)

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