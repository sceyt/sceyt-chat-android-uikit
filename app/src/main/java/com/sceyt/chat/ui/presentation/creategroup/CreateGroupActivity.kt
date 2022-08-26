package com.sceyt.chat.ui.presentation.creategroup

import android.animation.LayoutTransition
import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ActivityCreateGroupBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.chat.ui.presentation.creategroup.viewmodel.CreateGroupViewModel
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toMember
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.chat.ui.presentation.conversation.ConversationActivity
import com.sceyt.sceytchatuikit.R.*
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper

class CreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGroupBinding
    private val viewModel: CreateGroupViewModel by viewModels()
    private val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    private val createChannelData by lazy { CreateChannelData() }

    override fun onCreate(savedInstanceState: Bundle?) {
        statusBarIconsColorWithBackground(isNightTheme())
        super.onCreate(savedInstanceState)

        setContentView(ActivityCreateGroupBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        initViews()
        initViewModel()
        binding.initViews()
    }

    private fun initViewModel() {
        viewModel.createChannelLiveData.observe(this) {
            ConversationActivity.newInstance(this, it)
            finish()
        }

        viewModel.pageStateLiveData.observe(this) {
            binding.icSave.isVisible = it !is PageState.StateLoading
            binding.loadingSave.isVisible = it is PageState.StateLoading
            if (it is PageState.StateError)
                customToastSnackBar(it.errorMessage)
        }
    }

    private fun initViews() {
        binding.icBack.setOnClickListener { onBackPressed() }
    }

    private fun ActivityCreateGroupBinding.initViews() {
        layoutDetails.layoutTransition = LayoutTransition().apply { enableTransitionType(LayoutTransition.CHANGING) }

        icEditPhoto.setOnClickListener {
            EditAvatarTypeDialog(this@CreateGroupActivity) {
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

        switchChannelMode.setOnCheckedChangeListener { _, isChecked ->
            createChannelData.channelType = if (isChecked) Channel.Type.Private else Channel.Type.Public
            binding.groupURI.isVisible = !isChecked
        }

        icSave.setOnClickListener {
            with(createChannelData) {
                uri = uriInput.text.toString().trim()
                label = labelInput.text.toString().trim()
                metadata = metaDataInput.text.toString().trim()
                subject = subjectInput.text.toString().trim()
                members = intent.getParcelableArrayListExtra<SceytMember>(MEMBERS)?.map {
                    it.toMember()
                } ?: arrayListOf()
            }

            viewModel.createChannel(createChannelData)
            hideSoftInput()
        }
    }

    private fun setAvatarImage(filePath: String?) {
        createChannelData.avatarUrl = filePath
        binding.avatar.setImageUrl(filePath)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(anim.sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right)
    }

    companion object {
        private const val MEMBERS = "MEMBERS"

        fun launch(context: Context, members: ArrayList<SceytMember>) {
            context.launchActivity<CreateGroupActivity> {
                putParcelableArrayListExtra(MEMBERS, members)
            }
            context.asAppCompatActivity().overridePendingTransition(anim.sceyt_anim_slide_in_right,
                anim.sceyt_anim_slide_hold)
        }
    }
}