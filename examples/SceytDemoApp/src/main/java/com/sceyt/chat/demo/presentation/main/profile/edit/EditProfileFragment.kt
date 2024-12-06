package com.sceyt.chat.demo.presentation.main.profile.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.databinding.FragmentEditProfileBinding
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<EditProfileViewModel>()
    private val filePickerHelper = FilePickerHelper(this)
    private var currentUser: SceytUser? = null
    private var avatarUrl: String? = null
    private var onBackPressCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnNext.setEnabledOrNot(false)
        handleBackPress()
        initViewModel()
        binding.initListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressCallback?.remove()
        _binding = null
    }

    private fun initViewModel() {
        viewModel.currentUserLiveData.observe(viewLifecycleOwner) {
            setUserDetails(user = it)
        }

        viewModel.editProfileLiveData.observe(viewLifecycleOwner) {
            setUserDetails(user = it)
            SceytLoader.hideLoading()
        }

        viewModel.editProfileErrorLiveData.observe(viewLifecycleOwner) {
            SceytLoader.hideLoading()
            customToastSnackBar(it.toString())
        }
    }

    private fun setUserDetails(user: SceytUser?) {
        currentUser = user
        avatarUrl = user?.avatarURL
        user?.apply {
            with(binding) {
                avatar.setImageUrl(user.avatarURL)
                etFirstName.setText(firstName)
                etLastName.setText(lastName)
                etUserName.setText(username)
            }
        }
    }

    private fun FragmentEditProfileBinding.initListeners() {
        toolbar.setNavigationClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        avatar.setAvatarImageLoadListener {
            loadingProfileImage.isVisible = it
        }

        avatar.setOnClickListener {
            EditAvatarTypeDialog(requireContext(), avatarUrl.isNullOrBlank().not()) { type ->
                handleAvatarEdit(type)
            }.show()
        }

        etFirstName.doAfterTextChanged { checkIfNextButtonShouldBeEnabled() }
        etLastName.doAfterTextChanged { checkIfNextButtonShouldBeEnabled() }
        etUserName.doAfterTextChanged { checkIfNextButtonShouldBeEnabled() }

        btnNext.setOnClickListener {
            val newUsername = binding.etUserName.text?.trim().toString()
            if (newUsername.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.username_is_empty),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val isEditedAvatar = isAvatarChanged()
            SceytLoader.showLoading(requireContext())

            viewModel.saveProfile(
                firstName = etFirstName.text.toString(),
                lastName = etLastName.text.toString(),
                username = etUserName.text.toString(),
                avatarUrl = avatarUrl,
                shouldUploadAvatar = isEditedAvatar
            )
        }
    }

    private fun isAvatarChanged() = avatarUrl != currentUser?.avatarURL

    private fun checkIfNextButtonShouldBeEnabled() {
        binding.apply {
            val firstNameChanged = etFirstName.text?.isNotEmpty() == true
                    && etFirstName.text.toString() != currentUser?.firstName
            val lastNameChanged = etLastName.text?.isNotEmpty() == true
                    && etLastName.text.toString() != currentUser?.lastName
            val usernameChanged = etUserName.text?.isNotEmpty() == true
            etUserName.text.toString().trimStart('@') != currentUser?.username
            val shouldEnable =
                firstNameChanged || lastNameChanged || usernameChanged || isAvatarChanged()
            btnNext.setEnabledOrNot(shouldEnable)
        }
    }

    private fun handleBackPress() {
        with(requireActivity()) {
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    supportFragmentManager.popBackStack()
                }
            }.also {
                onBackPressCallback = it
            }
            onBackPressedDispatcher.addCallback(this, callback)
        }
    }

    private fun setProfileImage(filePath: String?) {
        avatarUrl = filePath
        checkIfNextButtonShouldBeEnabled()
        binding.avatar.setImageUrl(filePath)
    }

    private fun handleAvatarEdit(type: EditAvatarTypeDialog.EditAvatarType) {
        when (type) {
            EditAvatarTypeDialog.EditAvatarType.ChooseFromGallery -> {
                filePickerHelper.chooseFromGallery(
                    allowMultiple = false,
                    onlyImages = true
                ) { uris ->
                    if (uris.isNotEmpty()) setProfileImage(uris[0])
                }
            }

            EditAvatarTypeDialog.EditAvatarType.TakePhoto -> {
                filePickerHelper.takePicture { uri ->
                    setProfileImage(uri)
                }
            }

            EditAvatarTypeDialog.EditAvatarType.Delete -> {
                setProfileImage(null)
            }
        }
    }
}