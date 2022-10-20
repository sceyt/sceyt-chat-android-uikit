package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.databinding.ActivityConversationInfoBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteNotificationDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteTypeEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.genMemberBy
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import java.util.concurrent.TimeUnit


open class ConversationInfoActivity : AppCompatActivity() {
    private lateinit var channel: SceytChannel
    private lateinit var pagerAdapter: ViewPagerAdapter
    private var displayNameDefaultBg: Drawable? = null
    private var binding: ActivityConversationInfoBinding? = null
    private val viewModel: ConversationInfoViewModel by viewModels()
    private val chooseAttachmentHelper = ChooseAttachmentHelper(asComponentActivity())
    private var isEditMode = false
    private var isSaveLoading = false
    private var avatarUrl: String? = null

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
            onEditChannel(it)
        }

        viewModel.leaveChannelLiveData.observe(this, ::onLeaveChannel)

        viewModel.deleteChannelLiveData.observe(this, ::onDeleteChannel)

        viewModel.clearHistoryLiveData.observe(this, ::onClearHistory)

        viewModel.blockUnblockUserLiveData.observe(this, ::onBlockUnblockUser)

        viewModel.muteUnMuteLiveData.observe(this) {
            channel.muted = it.muted
            onMuteUnMuteChannel(it)
        }

        viewModel.pageStateLiveData.observe(this, ::onPageStateChange)
    }

    private fun ActivityConversationInfoBinding.initViews() {
        switchNotifications.setOnlyClickable()
        displayNameDefaultBg = subject.background
        setEditMode(isEditMode)

        avatar.setAvatarImageLoadListener {
            loadingProfileImage.isVisible = it
        }

        tvEditOrSave.setOnClickListener {
            val newSubject = subject.text?.trim().toString()
            val isEditedAvatar = avatarUrl != channel.iconUrl
            val isEditedDisplayName = newSubject != channel.channelSubject.trim()
            if (isEditMode) {
                if (isEditedAvatar || isEditedDisplayName) {
                    isLoadingEditChannel(true)
                    this@ConversationInfoActivity.isSaveLoading = true
                    editChannel(newSubject, avatarUrl)
                }
            }
            isEditMode = !isEditMode
            setEditMode(isEditMode)
        }

        icEditPhoto.setOnClickListener {
            EditAvatarTypeDialog(this@ConversationInfoActivity, avatarUrl.isNullOrBlank().not()) {
                when (it) {
                    EditAvatarTypeDialog.EditAvatarType.ChooseFromGallery -> {
                        chooseAttachmentHelper.chooseFromGallery(allowMultiple = false, onlyImages = true) { uris ->
                            if (uris.isNotEmpty())
                                setAvatarImage(uris[0])
                        }
                    }
                    EditAvatarTypeDialog.EditAvatarType.TakePhoto -> {
                        chooseAttachmentHelper.takePicture { uri ->
                            setAvatarImage(uri)
                        }
                    }
                    EditAvatarTypeDialog.EditAvatarType.Delete -> {
                        setAvatarImage(null)
                    }
                }
            }.show()
        }

        subject.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE)
                tvEditOrSave.callOnClick()
            false
        }

        clearHistory.setOnClickListener {
            showSceytDialog(R.string.sceyt_clear_history_title, R.string.sceyt_clear_history_desc, R.string.sceyt_clear) {
                clearHistory()
            }
        }

        leaveChannel.setOnClickListener {
            showSceytDialog(R.string.sceyt_leave_channel_title, R.string.sceyt_leave_channel_desc, R.string.sceyt_leave) {
                leaveChannel()
            }
        }

        blockAndLeaveChannel.setOnClickListener {
            showSceytDialog(R.string.sceyt_block_and_leave_channel_title, R.string.sceyt_block_and_leave_channel_desc, R.string.sceyt_leave) {
                blockAndLeaveChannel()
            }
        }

        blockUnblockUser.setOnClickListener {
            val user = (channel as SceytDirectChannel).peer ?: return@setOnClickListener
            val dialogTitleId: Int
            val dialogDescId: Int
            val positiveBtnTitleId: Int
            val callback: () -> Unit

            if (user.user.blocked) {
                dialogTitleId = R.string.sceyt_unblock_user_title
                dialogDescId = R.string.sceyt_unblock_user_desc
                positiveBtnTitleId = R.string.sceyt_unblock
                callback = { unblockUser(user.id) }
            } else {
                dialogTitleId = R.string.sceyt_block_user_title
                dialogDescId = R.string.sceyt_block_user_desc
                positiveBtnTitleId = R.string.sceyt_block
                callback = { blockUser(user.id) }
            }
            showSceytDialog(dialogTitleId, dialogDescId, positiveBtnTitleId, callback)
        }

        deleteChannel.setOnClickListener {
            showSceytDialog(R.string.sceyt_delete_channel_title, R.string.sceyt_delete_channel_desc, R.string.sceyt_delete) {
                deleteChannel()
            }
        }

        switchNotifications.setOnClickListener {
            if (channel.muted) {
                unMuteChannel()
                switchNotifications.isChecked = false
            } else {
                MuteNotificationDialog(this@ConversationInfoActivity) {
                    val until = when (it) {
                        MuteTypeEnum.Mute1Hour -> TimeUnit.HOURS.toMillis(1)
                        MuteTypeEnum.Mute2Hour -> TimeUnit.HOURS.toMillis(2)
                        MuteTypeEnum.Mute1Day -> TimeUnit.DAYS.toMillis(1)
                        MuteTypeEnum.MuteForever -> 0L
                    }
                    muteChannel(until)
                    switchNotifications.isChecked = true
                }.show()
            }
        }

        icBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupPagerAdapter(viewPager: ViewPager2?, tabLayout: TabLayout?) {
        val fragments = arrayListOf<Fragment>(
            getChannelMediaFragment(channel),
            getChannelFilesFragment(channel),
            getChannelLinksFragment(channel)
        )
        if (channel.channelType != ChannelTypeEnum.Direct)
            fragments.add(0, getChannelMembersFragment(channel))


        pagerAdapter = ViewPagerAdapter(this, fragments)

        setPagerAdapter(pagerAdapter)
        setupTabLayout(tabLayout ?: return, viewPager ?: return)
    }


    private fun ActivityConversationInfoBinding.setEditMode(isEditMode: Boolean) {
        subject.isEnabled = isEditMode
        icEditPhoto.isVisible = isEditMode

        if (isEditMode) {
            subject.background = displayNameDefaultBg
            subject.setSelection(subject.text?.length ?: 0)
            subject.setHint(R.string.sceyt_hint_channel_subject)
            tvEditOrSave.text = getString(R.string.sceyt_save)
            showSoftInput(subject)
        } else {
            subject.background = null
            subject.hint = ""
            tvEditOrSave.text = getString(R.string.sceyt_edit)
            hideKeyboard(subject)
        }
    }

    private fun setAvatarImage(filePath: String?) {
        avatarUrl = filePath
        binding?.avatar?.setImageUrl(filePath)
    }

    private fun getBlockText(blocked: Boolean) = if (blocked) getString(R.string.sceyt_unblock)
    else getString(R.string.sceyt_block)

    private fun ActivityConversationInfoBinding.isLoadingEditChannel(loading: Boolean) {
        tvEditOrSave.isVisible = !loading
        progressSave.isVisible = loading
    }

    private fun clearTaskAndOpenMainActivity() {
        /* val newIntent = Intent(this, MainActivity::class.java)
         newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
         startActivity(newIntent)*/
    }

    private fun setChannelDetails(channel: SceytChannel) {
        avatarUrl = channel.iconUrl
        with(binding ?: return) {
            avatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl)
            subject.setText(channel.channelSubject)
            switchNotifications.isChecked = channel.muted
            switchNotifications.jumpDrawablesToCurrentState()

            val isDirec = channel.channelType == ChannelTypeEnum.Direct
            tvEditOrSave.isVisible = !isDirec
            deleteChannel.isVisible = isDirec
            leaveChannel.isVisible = !isDirec
            blockAndLeaveChannel.isVisible = !isDirec
            blockUnblockUser.isVisible = isDirec
            if (isDirec)
                blockUnblockUser.text = getBlockText((channel as SceytDirectChannel).peer?.user?.blocked == true)
        }
    }

    protected fun setupTabLayout(tabLayout: TabLayout, viewPager: ViewPager2) {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getTagByPosition(position)
        }.attach()
    }

    protected fun editChannel(subject: String, avatarUrl: String?) {
        val data = EditChannelData(
            newSubject = subject,
            metadata = channel.metadata,
            label = channel.label,
            avatarUrl = avatarUrl,
            channelUrl = (channel as SceytGroupChannel).channelUrl,
            channelType = channel.channelType,
            avatarEdited = channel.getChannelAvatarUrl() == avatarUrl
        )
        viewModel.saveChanges(channel.id, data)
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

    protected fun showSceytDialog(@StringRes titleId: Int,
                                  @StringRes descId: Int,
                                  @StringRes positiveBtnTitleId: Int, positiveCb: () -> Unit) {
        SceytDialog(this@ConversationInfoActivity, positiveCb)
            .setTitle(getString(titleId))
            .setDescription(getString(descId))
            .setPositiveButtonTextColor(getCompatColor(R.color.sceyt_color_red))
            .setPositiveButtonTitle(getString(positiveBtnTitleId))
            .show()
    }

    protected fun getChannel() = channel.clone()

    open fun setActivityContentView() {
        setContentView(ActivityConversationInfoBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)
    }

    open fun onChannel(channel: SceytChannel) {
        setChannelDetails(channel)
        pagerAdapter.getFragment().find { fragment -> fragment is ChannelMembersFragment }?.let { membersFragment ->
            (membersFragment as ChannelMembersFragment).updateChannel(channel)
        }
    }

    open fun onEditChannel(sceytChannel: SceytChannel) {
        setChannelDetails(sceytChannel)
        binding?.isLoadingEditChannel(false)
        isSaveLoading = false
    }

    open fun onLeaveChannel(channelId: Long) {
        clearTaskAndOpenMainActivity()
    }

    open fun onDeleteChannel(channelId: Long) {
        clearTaskAndOpenMainActivity()
    }

    open fun onClearHistory(channelId: Long) {
        customToastSnackBar(getString(R.string.sceyt_history_was_successfully_cleared))
    }

    open fun onBlockUnblockUser(users: List<User>) {
        val peer = (channel as SceytDirectChannel).peer
        users.find { user -> user.id == peer?.id }?.let { user ->
            (channel as SceytDirectChannel).peer = genMemberBy(user).toSceytMember()
            binding?.blockUnblockUser?.text = getBlockText(user.blocked)
        }
    }

    open fun onMuteUnMuteChannel(sceytChannel: SceytChannel) {

    }

    open fun setPagerAdapter(pagerAdapter: ViewPagerAdapter) {
        binding?.viewPager?.adapter = pagerAdapter
    }

    open fun getChannelMembersFragment(channel: SceytChannel) = ChannelMembersFragment.newInstance(channel)

    open fun getChannelMediaFragment(channel: SceytChannel) = ChannelMediaFragment.newInstance(channel)

    open fun getChannelFilesFragment(channel: SceytChannel) = ChannelFilesFragment.newInstance(channel)

    open fun getChannelLinksFragment(channel: SceytChannel) = ChannelLinksFragment.newInstance(channel)

    open fun onPageStateChange(pageState: PageState) {
        if (pageState is PageState.StateError) {
            setChannelDetails(channel)
            binding?.setEditMode(false)
            binding?.isLoadingEditChannel(false)
            isSaveLoading = false
            customToastSnackBar(binding?.root, pageState.errorMessage.toString())
        }
    }

    open fun getViewPagerY(): Int {
        return (binding?.appbar?.height ?: 0) + (binding?.layoutToolbar?.height ?: 0)
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