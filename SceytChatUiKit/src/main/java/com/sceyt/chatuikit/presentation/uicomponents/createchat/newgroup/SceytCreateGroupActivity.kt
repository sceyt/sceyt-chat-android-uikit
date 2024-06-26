package com.sceyt.chatuikit.presentation.uicomponents.createchat.newgroup

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelDescriptionData
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytActivityCreateGroupBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTextViewsTextColor
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.presentation.common.SceytLoader.hideLoading
import com.sceyt.chatuikit.presentation.common.SceytLoader.showLoading
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.uicomponents.addmembers.adapters.UserItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.SceytConversationActivity
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.presentation.uicomponents.createchat.viewmodel.CreateChatViewModel
import com.sceyt.chatuikit.presentation.uicomponents.startchat.adapters.UserViewHolderFactory
import com.sceyt.chatuikit.presentation.uicomponents.startchat.adapters.UsersAdapter
import com.sceyt.chatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class SceytCreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityCreateGroupBinding
    private val viewModel: CreateChatViewModel by viewModels()
    private val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    private val createChannelData by lazy { CreateChannelData(ChannelTypeEnum.Private.getString()) }
    private lateinit var members: List<SceytMember>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(SceytActivityCreateGroupBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        binding.applyStyle()
        statusBarIconsColorWithBackground()

        getDataFromIntent()
        initViewModel()
        binding.initViews()
        setMembersAdapter()
    }

    private fun getDataFromIntent() {
        members = requireNotNull(intent.parcelableArrayList(MEMBERS))
    }

    private fun initViewModel() {
        viewModel.createChatLiveData.observe(this) {
            lifecycleScope.launch {
                SceytConversationActivity.newInstance(this@SceytCreateGroupActivity, it)
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        viewModel.pageStateLiveData.observe(this) {
            when (it) {
                is PageState.StateLoading -> showLoading(this@SceytCreateGroupActivity)
                is PageState.StateError -> {
                    customToastSnackBar(it.errorMessage)
                    hideLoading()
                }

                else -> hideLoading()
            }
        }
    }

    private fun SceytActivityCreateGroupBinding.initViews() {
        root.layoutTransition = LayoutTransition().apply { enableTransitionType(LayoutTransition.CHANGING) }

        tvSubject.doAfterTextChanged {
            btnCreate.setEnabledOrNot(it?.trim().isNullOrBlank().not())
        }

        toolbar.navigationIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        avatar.setOnClickListener {
            EditAvatarTypeDialog(this@SceytCreateGroupActivity, createChannelData.avatarUrl.isNotBlank()) {
                when (it) {
                    EditAvatarTypeDialog.EditAvatarType.ChooseFromGallery -> {
                        chooseAttachmentHelper.chooseFromGallery(allowMultiple = false, onlyImages = true) { uris ->
                            if (uris.isNotEmpty())
                                cropImage(uris[0])
                        }
                    }

                    EditAvatarTypeDialog.EditAvatarType.TakePhoto -> {
                        chooseAttachmentHelper.takePicture { uri ->
                            cropImage(uri)
                        }
                    }

                    EditAvatarTypeDialog.EditAvatarType.Delete -> {
                        setAvatarImage(null)
                    }
                }
            }.show()
        }

        btnCreate.setOnClickListener {
            with(createChannelData) {
                subject = tvSubject.text.toString().trim()
                channelType = ChannelTypeEnum.Private.getString()
                metadata = Gson().toJson(ChannelDescriptionData(tvDescription.text.toString().trim()))
                members = this@SceytCreateGroupActivity.members.map {
                    Member(Role(RoleTypeEnum.Member.toString()), User(it.id))
                }
            }

            viewModel.createChat(createChannelData)
            hideSoftInput()
        }
    }

    private fun setMembersAdapter() {
        val data: ArrayList<UserItem> = ArrayList(members.map { UserItem.User(it.user) })
        binding.rvMembers.adapter = UsersAdapter(data, UserViewHolderFactory(this) {})
    }

    private fun cropImage(filePath: String?) {
        filePath?.let { path ->
            val uri = Uri.fromFile(File(path))

            val file = File(cacheDir.path, System.currentTimeMillis().toString())
            val options = UCrop.Options()
            options.setToolbarColor(getCompatColor(R.color.sceyt_color_black))
            options.setStatusBarColor(getCompatColor(R.color.sceyt_color_black))
            options.setCircleDimmedLayer(true)
            options.setShowCropGrid(false)
            options.setShowCropFrame(false)
            options.setToolbarWidgetColor(getCompatColor(R.color.sceyt_color_on_primary))
            options.setToolbarTitle(getString(R.string.sceyt_move_and_scale))
            options.setHideBottomControls(true)

            UCrop.of(uri, Uri.fromFile(file))
                .withOptions(options)
                .withAspectRatio(1f, 1f)
                .start(this, UCrop.REQUEST_CROP)
        }
    }

    private fun setAvatarImage(filePath: String?) {
        createChannelData.avatarUrl = filePath.let {
            resizeImage(this@SceytCreateGroupActivity, it, 500).getOrNull() ?: ""
        }
        binding.avatar.setImageUrl(createChannelData.avatarUrl)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == UCrop.REQUEST_CROP) {
                if (data != null) {
                    val path = UCrop.getOutput(data)?.path
                    if (path != null) {
                        setAvatarImage(path)
                    } else customToastSnackBar(getString(R.string.sceyt_wrong_image))
                } else customToastSnackBar(getString(R.string.sceyt_wrong_image))
            }
        }
    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    private fun SceytActivityCreateGroupBinding.applyStyle() {
        root.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.backgroundColor))
        toolbar.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.primaryColor))
        toolbar.setIconsTint(SceytChatUIKit.theme.accentColor)
        toolbar.setTitleColorRes(SceytChatUIKit.theme.textPrimaryColor)
        tvContacts.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
        tvContacts.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.surface1Color))
        setTextViewsTextColor(listOf(tvSubject, tvDescription), getCompatColor(SceytChatUIKit.theme.textPrimaryColor))
    }

    companion object {
        private const val MEMBERS = "MEMBERS"

        fun newIntent(context: Context, members: ArrayList<SceytMember>): Intent {
            val intent = Intent(context, SceytCreateGroupActivity::class.java)
            intent.putParcelableArrayListExtra(MEMBERS, members)
            return intent
        }
    }
}