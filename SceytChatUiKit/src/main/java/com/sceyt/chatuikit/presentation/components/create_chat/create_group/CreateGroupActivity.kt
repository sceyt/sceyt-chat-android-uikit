package com.sceyt.chatuikit.presentation.components.create_chat.create_group

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTextViewsTextColor
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
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class CreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityCreateGroupBinding
    private val viewModel: CreateChatViewModel by viewModels()
    private val filePickerHelper = FilePickerHelper(this)
    private val createChannelData by lazy { CreateChannelData(ChannelTypeEnum.Group.value) }
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
                ChannelActivity.newInstance(this@CreateGroupActivity, it)
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

        tvSubject.doAfterTextChanged {
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
                subject = tvSubject.text.toString().trim()
                channelType = ChannelTypeEnum.Group.value
                metadata = Gson().toJson(ChannelDescriptionData(tvDescription.text.toString().trim()))
                members = this@CreateGroupActivity.members
            }

            viewModel.createChat(createChannelData)
            hideSoftInput()
        }
    }

    private fun setMembersAdapter() {
        val data = members.map { UserItem.User(it.user) }
        binding.rvMembers.adapter = UsersAdapter(data, UserViewHolderFactory(this) {})
    }

    private fun cropImage(filePath: String?) {
        filePath?.let { path ->
            val uri = Uri.fromFile(File(path))

            val file = File(cacheDir.path, System.currentTimeMillis().toString())
            val options = UCrop.Options()
            options.setToolbarColor(Color.BLACK)
            options.setStatusBarColor(Color.BLACK)
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
            val reqSize = SceytChatUIKit.config.avatarResizeConfig.dimensionThreshold
            val quality = SceytChatUIKit.config.avatarResizeConfig.compressionQuality
            resizeImage(this@CreateGroupActivity, it, reqSize, quality).getOrNull() ?: ""
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
        root.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))
        toolbar.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.colors.primaryColor))
        toolbar.setIconsTint(SceytChatUIKit.theme.colors.accentColor)
        toolbar.setTitleColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
        tvContacts.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
        tvContacts.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.colors.surface1Color))
        setTextViewsTextColor(listOf(tvSubject, tvDescription), getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
    }

    companion object {
        private const val MEMBERS = "MEMBERS"

        fun newIntent(context: Context, members: List<SceytMember>): Intent {
            val intent = Intent(context, CreateGroupActivity::class.java)
            intent.putParcelableArrayListExtra(MEMBERS, members.toArrayList())
            return intent
        }
    }
}