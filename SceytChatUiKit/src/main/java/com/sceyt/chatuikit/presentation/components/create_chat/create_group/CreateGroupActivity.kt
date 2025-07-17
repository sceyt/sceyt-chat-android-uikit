package com.sceyt.chatuikit.presentation.components.create_chat.create_group

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelDescriptionData
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytActivityCreateGroupBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.common.SceytLoader.hideLoading
import com.sceyt.chatuikit.presentation.common.SceytLoader.showLoading
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.CreateChatViewModel
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.startchat.adapters.UsersAdapter
import com.sceyt.chatuikit.presentation.components.startchat.adapters.holders.UserViewHolderFactory
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import com.sceyt.chatuikit.styles.CreateGroupStyle
import com.sceyt.chatuikit.styles.ImageCropperStyle
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class CreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityCreateGroupBinding
    private lateinit var style: CreateGroupStyle
    private val viewModel: CreateChatViewModel by viewModels()
    private val filePickerHelper = FilePickerHelper(this)
    private val createChannelData by lazy { CreateChannelData(ChannelTypeEnum.Group.value) }
    private lateinit var members: List<SceytMember>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        style = CreateGroupStyle.Builder(this, null).build()
        setContentView(SceytActivityCreateGroupBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        binding.applyStyle()
        applyInsetsAndWindowColor(binding.root)
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
                ChannelActivity.launch(this@CreateGroupActivity, it)
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        viewModel.pageStateLiveData.observe(this) {
            when (it) {
                is PageState.StateLoading -> showLoading(this@CreateGroupActivity)
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
        btnCreate.setEnabledOrNot(false)

        inputSubject.doAfterTextChanged {
            btnCreate.setEnabledOrNot(it?.trim().isNullOrBlank().not())
        }

        toolbar.setNavigationClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        avatar.setOnClickListener {
            EditAvatarTypeDialog(this@CreateGroupActivity, createChannelData.avatarUrl.isNotBlank()) {
                when (it) {
                    EditAvatarTypeDialog.EditAvatarType.ChooseFromGallery -> {
                        filePickerHelper.chooseFromGallery(allowMultiple = false, onlyImages = true) { uris ->
                            if (uris.isNotEmpty())
                                cropImage(uris[0])
                        }
                    }

                    EditAvatarTypeDialog.EditAvatarType.TakePhoto -> {
                        filePickerHelper.takePicture { uri ->
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
                subject = inputSubject.text.toString().trim()
                type = ChannelTypeEnum.Group.value
                metadata = Gson().toJson(ChannelDescriptionData(inputDescription.text.toString().trim()))
                members = this@CreateGroupActivity.members
            }

            viewModel.createChat(createChannelData)
            hideSoftInput()
        }
    }

    private fun setMembersAdapter() {
        val data = members.map { UserItem.User(it.user) }
        binding.rvMembers.adapter = UsersAdapter(data,
            UserViewHolderFactory(this, style.userItemStyle, listeners = {})
        )
    }

    private fun setAvatarImage(filePath: String?) {
        createChannelData.avatarUrl = filePath.let {
            val reqSize = SceytChatUIKit.config.avatarResizeConfig.dimensionThreshold
            val quality = SceytChatUIKit.config.avatarResizeConfig.compressionQuality
            resizeImage(
                path = it,
                parentDir = filesDir,
                reqSize = reqSize,
                quality = quality
            ).getOrNull() ?: ""
        }
        binding.avatar.setImageUrl(createChannelData.avatarUrl)
    }

    private fun cropImage(filePath: String?) {
        filePath?.let { path ->
            val uri = Uri.fromFile(File(path))
            val file = File(cacheDir.path, System.currentTimeMillis().toString())

            val intent = UCrop.of(uri, Uri.fromFile(file))
                .withOptions(ImageCropperStyle.default(this).createOptions())
                .withAspectRatio(1f, 1f)
                .getIntent(this)

            cropperActivityResultLauncher.launch(intent)
        }
    }

    private val cropperActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val path = UCrop.getOutput(data)?.path
            if (path != null) {
                setAvatarImage(path)
            } else customToastSnackBar(getString(R.string.sceyt_wrong_image))
        }
    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    private fun SceytActivityCreateGroupBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
        avatar.appearanceBuilder()
            .setDefaultAvatar(style.avatarDefaultIcon)
            .setStyle(AvatarStyle(avatarBackgroundColor = style.avatarBackgroundColor))
            .build()
            .applyToAvatar()
        style.nameTextFieldStyle.apply(inputSubject, null)
        style.aboutTextFieldStyle.apply(inputDescription, null)
        style.actionButtonStyle.applyToCustomButton(btnCreate)
        style.separatorTextStyle.apply(tvSeparator)
    }

    companion object {
        private const val MEMBERS = "MEMBERS"

        fun launch(context: Context, members: List<SceytMember>) {
            context.launchActivity<CreateGroupActivity>(
                enterAnimResId = R.anim.sceyt_anim_slide_in_right,
                exitAnimResId = R.anim.sceyt_anim_slide_hold
            ) {
                putParcelableArrayListExtra(MEMBERS, members.toArrayList())
            }
        }

        fun newIntent(context: Context, members: List<SceytMember>): Intent {
            val intent = Intent(context, CreateGroupActivity::class.java)
            intent.putParcelableArrayListExtra(MEMBERS, members.toArrayList())
            return intent
        }
    }
}