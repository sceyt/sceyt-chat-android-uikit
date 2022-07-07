package com.sceyt.chat.ui.presentation.uicomponents.creategroup

import android.animation.LayoutTransition
import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.CreateChannelData
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.toMember
import com.sceyt.chat.ui.databinding.ActivityCreateGroupBinding
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.sceyt.chat.ui.presentation.mainactivity.profile.dialogs.EditAvatarTypeDialog
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.uicomponents.conversation.ConversationActivity
import com.sceyt.chat.ui.presentation.uicomponents.creategroup.viewmodel.CreateGroupViewModel

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
        overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
    }

    companion object {
        private const val MEMBERS = "MEMBERS"

        fun launch(context: Context, members: ArrayList<SceytMember>) {
            context.launchActivity<CreateGroupActivity> {
                putParcelableArrayListExtra(MEMBERS, members)
            }
            context.asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right,
                R.anim.sceyt_anim_slide_hold)
        }
    }
}