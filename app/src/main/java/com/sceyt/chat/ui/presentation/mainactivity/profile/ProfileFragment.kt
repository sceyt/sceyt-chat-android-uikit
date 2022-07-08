package com.sceyt.chat.ui.presentation.mainactivity.profile

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
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.FragmentProfileBinding
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.sceyt.chat.ui.presentation.mainactivity.profile.dialogs.EditAvatarTypeDialog
import com.sceyt.chat.ui.presentation.mainactivity.profile.viewmodel.ProfileViewModel
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var displayNameDefaultBg: Drawable
    private val viewModel: ProfileViewModel by viewModels()
    private val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    private var currentUser: User? = null
    private var avatarUrl: String? = null
    private var isEditMode = false
    private var isSaveLoading = false

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
            customToastSnackBar(requireView(), it.toString())
        }
    }

    private fun FragmentProfileBinding.initViews() {
        displayNameDefaultBg = binding.displayName.background
        setEditMode(isEditMode)

        avatar.setAvatarImageLoadListener {
            loadingProfileImage.isVisible = it
        }

        tvEditOrSave.setOnClickListener {
            val newDisplayName = binding.displayName.text?.trim().toString()
            val isEditedAvatar = avatarUrl != currentUser?.avatarURL
            val isEditedDisplayName = newDisplayName != currentUser?.fullName?.trim()
            if (isEditMode) {
                if (isEditedAvatar || isEditedDisplayName) {
                    binding.isSaveLoading = true
                    this@ProfileFragment.isSaveLoading = true
                    viewModel.saveProfile(newDisplayName, avatarUrl, isEditedAvatar)
                }
            }
            isEditMode = !isEditMode
            setEditMode(isEditMode)
        }

        icEditPhoto.setOnClickListener {
            EditAvatarTypeDialog(requireContext()) {
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
            tvEditOrSave.text = getString(R.string.sceyt_save)
            requireContext().showSoftInput(displayName)
        } else {
            displayName.background = null
            displayName.hint = ""
            tvEditOrSave.text = getString(R.string.sceyt_edit)
            requireContext().hideKeyboard(displayName)
        }
    }

    private fun setUpThemeSwitch() {
        binding.switchTheme.isChecked = requireContext().isNightTheme()
        binding.switchTheme.setOnClickListener {
            val oldIsDark = SceytUIKitConfig.SceytUITheme.isDarkMode
            SceytUIKitConfig.SceytUITheme.isDarkMode = !oldIsDark
            requireActivity().statusBarIconsColorWithBackground(!oldIsDark)
            if (oldIsDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun setProfileImage(filePath: String?) {
        avatarUrl = filePath
        binding.avatar.setImageUrl(filePath)
    }
}