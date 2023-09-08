package com.sceyt.chat.demo.presentation.creategroup

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.sceyt.chat.demo.databinding.ActivityCreateGroupBinding
import com.sceyt.chat.demo.presentation.conversation.ConversationActivity
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toMember
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.hideSoftInput
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelableArrayList
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.creategroup.viewmodel.CreateChatViewModel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper

class CreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGroupBinding
    private val viewModel: CreateChatViewModel by viewModels()
    private val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    private val createChannelData by lazy { CreateChannelData("") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityCreateGroupBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode)

        initViewModel()
        binding.initViews()
    }

    private fun initViewModel() {
        viewModel.createChatLiveData.observe(this) {
            ConversationActivity.newInstance(this, it)

            val intent = Intent()
            setResult(RESULT_OK, intent)
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

        binding.icBack.setOnClickListener { finish() }

        icEditPhoto.setOnClickListener {
            EditAvatarTypeDialog(this@CreateGroupActivity, createChannelData.avatarUrl.isBlank().not()) {
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
            createChannelData.channelType = if (isChecked) ChannelTypeEnum.Private.getString()
            else ChannelTypeEnum.Public.getString()
            binding.groupURI.isVisible = !isChecked
        }

        icSave.setOnClickListener {
            with(createChannelData) {
                uri = uriInput.text.toString().trim()
                metadata = metaDataInput.text.toString().trim()
                subject = subjectInput.text.toString().trim()
                members = intent.parcelableArrayList<SceytMember>(MEMBERS)?.map {
                    it.toMember()
                } ?: arrayListOf()
            }

            viewModel.createChat(createChannelData)
            hideSoftInput()
        }
    }

    private fun setAvatarImage(filePath: String?) {
        createChannelData.avatarUrl = filePath ?: ""
        binding.avatar.setImageUrl(filePath)
    }

    override fun finish() {
        super.finish()
        overrideTransitions(anim.sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right, false)
    }

    companion object {
        private const val MEMBERS = "MEMBERS"

        fun newIntent(context: Context, members: ArrayList<SceytMember>): Intent {
            val intent = Intent(context, CreateGroupActivity::class.java)
            intent.putParcelableArrayListExtra(MEMBERS, members)
            return intent
        }
    }
}