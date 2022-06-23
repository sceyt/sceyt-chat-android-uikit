package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo

import android.content.Context
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
import com.sceyt.chat.ui.databinding.ActivityConversationInfoBinding
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.presentation.common.chooseAttachment.ChooseAttachmentHelper
import com.sceyt.chat.ui.presentation.mainactivity.profile.dialogs.EditAvatarTypeDialog
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel

class ConversationInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationInfoBinding
    private lateinit var channel: SceytChannel
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
        setupPagerAdapter()
    }

    private fun getBundleArguments() {
        channel = intent.getParcelableExtra(CHANNEL)!!
    }

    private fun initViewModel() {
        viewModel.editChannelLiveData.observe(this) {
            setChannelDetails(it)
            binding.isLoadingEditChannel(false)
            isSaveLoading = false
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
        displayNameDefaultBg = binding.subject.background
        setEditMode(isEditMode)

        avatar.setAvatarImageLoadListener {
            loadingProfileImage.isVisible = it
        }

        tvEditOrSave.setOnClickListener {
            val newSubject = binding.subject.text?.trim().toString()
            val isEditedAvatar = avatarUrl != channel.iconUrl
            val isEditedDisplayName = newSubject != channel.channelSubject.trim()
            if (isEditMode) {
                if (isEditedAvatar || isEditedDisplayName) {
                    binding.isLoadingEditChannel(true)
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

        binding.icBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setChannelDetails(channel: SceytChannel) {
        this.channel = channel
        avatarUrl = channel.iconUrl
        binding.avatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl)
        binding.subject.setText(channel.channelSubject)
        binding.tvEditOrSave.isVisible = channel.channelType != ChannelTypeEnum.Direct
    }

    private fun setupPagerAdapter() {
        val fragments = arrayListOf(ChannelMediaFragment(), ChannelFilesFragment())
        if (channel.channelType != ChannelTypeEnum.Direct)
            fragments.add(0, ChannelMembersFragment())

        val adapter = ViewPagerAdapter(this, fragments)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.sceyt_members)
                1 -> tab.text = getString(R.string.sceyt_media)
                2 -> tab.text = getString(R.string.sceyt_files)
            }
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

    private fun ActivityConversationInfoBinding.isLoadingEditChannel(loading: Boolean) {
        tvEditOrSave.isVisible = !loading
        progressSave.isVisible = loading
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