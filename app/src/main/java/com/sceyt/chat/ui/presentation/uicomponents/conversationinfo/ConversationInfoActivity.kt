package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayoutMediator
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.databinding.ActivityConversationInfoBinding
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.presentation.common.SceytDialog
import com.sceyt.chat.ui.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.sceyt.chat.ui.presentation.mainactivity.MainActivity
import com.sceyt.chat.ui.presentation.mainactivity.profile.dialogs.EditAvatarTypeDialog
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.dialogs.MuteNotificationDialog
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.dialogs.MuteTypeEnum
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.genMemberBy
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import java.util.concurrent.TimeUnit


class ConversationInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationInfoBinding
    private lateinit var channel: SceytChannel
    private lateinit var pagerAdapter: ViewPagerAdapter
    private lateinit var displayNameDefaultBg: Drawable
    private val viewModel: ConversationInfoViewModel by viewModels()
    private val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    private var isEditMode = false
    private var isSaveLoading = false
    private var avatarUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusBarIconsColorWithBackground(isNightTheme())

        binding = ActivityConversationInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getBundleArguments()
        initViewModel()
        binding.initViews()
        setChannelDetails(channel)
        viewModel.getChannel(channel.id)
        setupPagerAdapter()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(intent.getParcelableExtra(CHANNEL))
    }

    private fun initViewModel() {
        viewModel.channelLiveData.observe(this) {
            channel = it
            setChannelDetails(channel)
            pagerAdapter.getFragment().find { fragment -> fragment is ChannelMembersFragment }?.let { membersFragment ->
                (membersFragment as ChannelMembersFragment).updateChannel(it)
            }
        }

        viewModel.editChannelLiveData.observe(this) {
            setChannelDetails(it)
            binding.isLoadingEditChannel(false)
            isSaveLoading = false
        }

        viewModel.leaveChannelLiveData.observe(this) {
            clearTaskAndOpenMainActivity()
        }

        viewModel.deleteChannelLiveData.observe(this) {
            clearTaskAndOpenMainActivity()
        }

        viewModel.clearHistoryLiveData.observe(this) {
            customToastSnackBar(getString(R.string.history_was_successfully_cleared))
        }

        viewModel.blockUnblockUserLiveData.observe(this) {
            val peer = (channel as SceytDirectChannel).peer
            it.find { user -> user.id == peer?.id }?.let { user ->
                (channel as SceytDirectChannel).peer = genMemberBy(user)
                binding.blockUnblockUser.text = getBlockText(user.blocked)
            }
        }

        viewModel.muteUnMuteLiveData.observe(this) {
            channel.muted = it.muted
        }

        viewModel.pageStateLiveData.observe(this) {
            if (it is PageState.StateError) {
                setChannelDetails(channel)
                binding.setEditMode(false)
                binding.isLoadingEditChannel(false)
                isSaveLoading = false
                customToastSnackBar(binding.root, it.errorMessage.toString())
            }
        }
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
                    viewModel.saveChanges(channel, newSubject, avatarUrl, isEditedAvatar)
                }
            }
            isEditMode = !isEditMode
            setEditMode(isEditMode)
        }

        icEditPhoto.setOnClickListener {
            EditAvatarTypeDialog(this@ConversationInfoActivity) {
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
            SceytDialog(this@ConversationInfoActivity, positiveClickListener = {
                viewModel.clearHistory(channel)
            }).setTitle(getString(R.string.sceyt_clear_history_title))
                .setDescription(getString(R.string.sceyt_clear_history_desc))
                .setPositiveButtonTextColor(getCompatColor(R.color.sceyt_color_red))
                .setPositiveButtonTitle(getString(R.string.sceyt_clear))
                .show()
        }

        leaveChannel.setOnClickListener {
            SceytDialog(this@ConversationInfoActivity, positiveClickListener = {
                viewModel.leaveChannel(channel)
            }).setTitle(getString(R.string.sceyt_leave_channel_title))
                .setDescription(getString(R.string.sceyt_leave_channel_desc))
                .setPositiveButtonTextColor(getCompatColor(R.color.sceyt_color_red))
                .setPositiveButtonTitle(getString(R.string.sceyt_leave))
                .show()
        }

        blockAndLeaveChannel.setOnClickListener {
            SceytDialog(this@ConversationInfoActivity, positiveClickListener = {
                viewModel.blockAndLeaveChannel(channel)
            }).setTitle(getString(R.string.sceyt_block_and_leave_channel_title))
                .setDescription(getString(R.string.sceyt_block_and_leave_channel_desc))
                .setPositiveButtonTextColor(getCompatColor(R.color.sceyt_color_red))
                .setPositiveButtonTitle(getString(R.string.sceyt_leave))
                .show()
        }

        blockUnblockUser.setOnClickListener {
            val user = (channel as SceytDirectChannel).peer ?: return@setOnClickListener
            val dialogTitle: String
            val dialogDesc: String
            val positiveBtnTitle: String
            val callback: () -> Unit

            if (user.blocked) {
                dialogTitle = getString(R.string.sceyt_unblock_user_title)
                dialogDesc = getString(R.string.sceyt_unblock_user_desc)
                positiveBtnTitle = getString(R.string.sceyt_unblock)
                callback = { viewModel.unblockUser(user.id) }
            } else {
                dialogTitle = getString(R.string.sceyt_block_user_title)
                dialogDesc = getString(R.string.sceyt_block_user_desc)
                positiveBtnTitle = getString(R.string.sceyt_block)
                callback = { viewModel.blockUser(user.id) }
            }
            SceytDialog(this@ConversationInfoActivity, positiveClickListener = callback)
                .setTitle(dialogTitle)
                .setDescription(dialogDesc)
                .setPositiveButtonTitle(positiveBtnTitle)
                .setPositiveButtonTextColor(getCompatColor(R.color.sceyt_color_red))
                .show()
        }

        deleteChannel.setOnClickListener {
            SceytDialog(this@ConversationInfoActivity, positiveClickListener = {
                viewModel.deleteChannel(channel)
            }).setTitle(getString(R.string.sceyt_delete_channel_title))
                .setDescription(getString(R.string.sceyt_delete_channel_desc))
                .setPositiveButtonTextColor(getCompatColor(R.color.sceyt_color_red))
                .setPositiveButtonTitle(getString(R.string.sceyt_delete))
                .show()
        }

        switchNotifications.setOnClickListener {
            if (channel.muted) {
                viewModel.unMuteChannel(channel)
                switchNotifications.isChecked = false
            } else {
                MuteNotificationDialog(this@ConversationInfoActivity) {
                    val until = when (it) {
                        MuteTypeEnum.Mute1Hour -> TimeUnit.HOURS.toMillis(1)
                        MuteTypeEnum.Mute2Hour -> TimeUnit.HOURS.toMillis(2)
                        MuteTypeEnum.Mute1Day -> TimeUnit.DAYS.toMillis(1)
                        MuteTypeEnum.MuteForever -> 0L
                    }
                    viewModel.muteChannel(channel, until)
                    switchNotifications.isChecked = true
                }.show()
            }
        }

        icBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setChannelDetails(channel: SceytChannel) {
        this.channel = channel
        avatarUrl = channel.iconUrl
        with(binding) {
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
                blockUnblockUser.text = getBlockText((channel as SceytDirectChannel).peer?.blocked == true)
        }
    }

    private fun setupPagerAdapter() {
        val fragments = arrayListOf(
            ChannelMediaFragment.newInstance(channel),
            ChannelFilesFragment.newInstance(channel),
            ChannelLinksFragment.newInstance(channel)
        )
        if (channel.channelType != ChannelTypeEnum.Direct)
            fragments.add(0, ChannelMembersFragment.newInstance(channel))


        pagerAdapter = ViewPagerAdapter(this, fragments)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.getTagByPosition(position)
        }.attach()
    }

    private fun ActivityConversationInfoBinding.setEditMode(isEditMode: Boolean) {
        subject.isEnabled = isEditMode
        icEditPhoto.isVisible = isEditMode

        if (isEditMode) {
            subject.background = displayNameDefaultBg
            subject.setSelection(subject.text?.length ?: 0)
            subject.setHint(R.string.hint_channel_subject)
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
        binding.avatar.setImageUrl(filePath)
    }

    private fun getBlockText(blocked: Boolean) = if (blocked) getString(R.string.sceyt_unblock)
    else getString(R.string.sceyt_block)

    private fun ActivityConversationInfoBinding.isLoadingEditChannel(loading: Boolean) {
        tvEditOrSave.isVisible = !loading
        progressSave.isVisible = loading
    }

    private fun clearTaskAndOpenMainActivity() {
        val newIntent = Intent(this, MainActivity::class.java)
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(newIntent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
    }

    companion object {
        private const val CHANNEL = "CHANNEL"

        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<ConversationInfoActivity> {
                putExtra(CHANNEL, channel)
            }
            context.asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }
}