package com.sceyt.chat.demo.presentation.mainactivity.profile

import com.sceyt.sceytchatuikit.R as SceytKitR
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sceyt.chat.models.user.User
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.databinding.FragmentProfileBinding
import com.sceyt.chat.demo.presentation.login.LoginActivity
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.hideKeyboard
import com.sceyt.sceytchatuikit.extensions.isNightTheme
import com.sceyt.sceytchatuikit.extensions.setOnlyClickable
import com.sceyt.sceytchatuikit.extensions.showSoftInput
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteNotificationDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.MuteTypeEnum
import com.sceyt.sceytchatuikit.presentation.uicomponents.profile.viewmodel.ProfileViewModel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.UserStyle
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var displayNameDefaultBg: Drawable
    private val viewModel by viewModels<ProfileViewModel>()
    private val preference by inject<AppSharedPreference>()
    private val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    private var currentUser: User? = null
    private var avatarUrl: String? = null
    private var isEditMode = false
    private var isSaveLoading = false
    private var muted: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentProfileBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpThemeSwitch()
        binding.initViews()
        initViewModel()
        viewModel.getSettings()
    }

    override fun onResume() {
        super.onResume()
        if (!isEditMode && !isSaveLoading)
            viewModel.getCurrentUser()
    }

    private fun initViewModel() {
        viewModel.currentUserLiveData.observe(viewLifecycleOwner) {
            setUserDetails(user = it)
        }

        viewModel.editProfileLiveData.observe(viewLifecycleOwner) {
            setUserDetails(user = it)
            binding.isSaveLoading = false
            isSaveLoading = false
        }

        viewModel.editProfileErrorLiveData.observe(viewLifecycleOwner) {
            binding.setEditMode(false)
            binding.isSaveLoading = false
            isSaveLoading = false
            customToastSnackBar(it.toString())
        }

        viewModel.settingsLiveData.observe(viewLifecycleOwner) {
            muted = it.mute.isMuted
            binding.switchNotifications.isChecked = it.mute.isMuted
        }

        viewModel.muteUnMuteLiveData.observe(viewLifecycleOwner) {
            muted = it
            binding.switchNotifications.isChecked = it
        }

        viewModel.logOutLiveData.observe(viewLifecycleOwner) {
            preference.setToken(null)
            preference.setUserId(null)
            LoginActivity.launch(requireContext())
            requireActivity().finish()
        }

        viewModel.logOutErrorLiveData.observe(viewLifecycleOwner) {
            customToastSnackBar(it.toString())
        }
    }

    private fun FragmentProfileBinding.initViews() {
        displayNameDefaultBg = binding.displayName.background
        setEditMode(isEditMode)
        switchNotifications.setOnlyClickable()

        avatar.setAvatarImageLoadListener {
            loadingProfileImage.isVisible = it
        }

        switchNotifications.setOnClickListener {
            if (muted) {
                viewModel.unMuteNotifications()
                switchNotifications.isChecked = false
            } else {
                MuteNotificationDialog(requireContext()) {
                    val until = when (it) {
                        MuteTypeEnum.Mute1Hour -> TimeUnit.HOURS.toMillis(1)
                        MuteTypeEnum.Mute8Hour -> TimeUnit.HOURS.toMillis(8)
                        MuteTypeEnum.MuteForever -> 0L
                    }
                    viewModel.muteNotifications(until)
                    switchNotifications.isChecked = true
                }.show()
            }
        }

        tvEditOrSave.setOnClickListener {
            val newDisplayName = binding.displayName.text?.trim().toString()
            val isEditedAvatar = avatarUrl != currentUser?.avatarURL
            val isEditedDisplayName = newDisplayName != currentUser?.fullName?.trim()
            if (isEditMode) {
                if (isEditedAvatar || isEditedDisplayName) {
                    binding.isSaveLoading = true
                    this@ProfileFragment.isSaveLoading = true
                    viewModel.saveProfile(newDisplayName, null, avatarUrl, isEditedAvatar)
                }
            }
            isEditMode = !isEditMode
            setEditMode(isEditMode)
        }

        icEditPhoto.setOnClickListener {
            EditAvatarTypeDialog(requireContext(), avatarUrl.isNullOrBlank().not()) {
                when (it) {
                    EditAvatarTypeDialog.EditAvatarType.ChooseFromGallery -> {
                        chooseAttachmentHelper.chooseFromGallery(allowMultiple = false, onlyImages = true) { uris ->
                            if (uris.isNotEmpty())
                                setProfileImage(uris[0])
                        }
                    }

                    EditAvatarTypeDialog.EditAvatarType.TakePhoto -> {
                        chooseAttachmentHelper.takePicture { uri ->
                            setProfileImage(uri)
                        }
                    }

                    EditAvatarTypeDialog.EditAvatarType.Delete -> {
                        setProfileImage(null)
                    }
                }
            }.show()
        }

        signOut.setOnClickListener {
            SceytDialog(requireContext()).setTitle(getString(R.string.sign_out_title))
                .setDescription(getString(R.string.sign_out_desc))
                .setPositiveButtonTitle(getString(R.string.sign_out))
                .setPositiveButtonTextColor(requireContext().getCompatColor(SceytKitR.color.sceyt_color_red))
                .setPositiveButtonClickListener {
                    viewModel.logout()
                }
                .show()
        }

        displayName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE)
                tvEditOrSave.callOnClick()
            false
        }
    }

    private fun setUserDetails(user: User?) {
        currentUser = user
        avatarUrl = user?.avatarURL
        user?.apply {
            binding.avatar.setNameAndImageUrl(fullName.trim(), avatarURL)
            binding.displayName.setText(fullName.trim())
        }
    }

    private fun FragmentProfileBinding.setEditMode(isEditMode: Boolean) {
        displayName.isEnabled = isEditMode
        icEditPhoto.isVisible = isEditMode

        if (isEditMode) {
            displayName.background = displayNameDefaultBg
            displayName.setSelection(displayName.text?.length ?: 0)
            displayName.setHint(R.string.display_name)
            tvEditOrSave.text = getString(R.string.save)
            requireContext().showSoftInput(displayName)
        } else {
            displayName.background = null
            displayName.hint = ""
            tvEditOrSave.text = getString(R.string.edit)
            requireContext().hideKeyboard(displayName)
        }
    }

    private fun setUpThemeSwitch() {
        binding.switchTheme.isChecked = requireContext().isNightTheme()
        binding.switchTheme.setOnClickListener {
            val oldIsDark = SceytKitConfig.SceytUITheme.isDarkMode
            SceytKitConfig.SceytUITheme.isDarkMode = !oldIsDark
            requireActivity().statusBarIconsColorWithBackground(!oldIsDark)
            if (oldIsDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun setProfileImage(filePath: String?) {
        avatarUrl = filePath
        binding.avatar.setImageUrl(filePath, UserStyle.userDefaultAvatar)
    }
}