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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.databinding.FragmentEditProfileBinding
import com.sceyt.chat.demo.presentation.common.ui.handleUsernameValidation
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import com.vanniktech.ui.hideKeyboard
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModel()
    private val filePickerHelper = FilePickerHelper(this)
    private var currentUser: SceytUser? = null
    private var avatarUrl: String? = null
    private var currentUsername: String? = null
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

        viewModel.correctUsernameValidatorLiveData.observe(viewLifecycleOwner) {
            handleUsernameValidation(
                context = requireContext(),
                validationState = it,
                setAlert = { color, message ->
                    setUsernameAlert(color, message)
                },
                isUsernameCorrect = { isUsernameCorrect ->
                    viewModel.setUsernameValidState(
                        isUsernameCorrect && binding.etFirstName.text.isNotNullOrBlank()
                    )
                }
            )
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.usernameInput.collect { username ->
                    viewModel.updateUsernameInput(username)
                }
            }
        }

        viewModel.nextButtonEnabledLiveData.observe(viewLifecycleOwner) {
            binding.btnNext.setEnabledOrNot(it)
        }
    }

    private fun setUserDetails(user: SceytUser?) {
        viewModel.isFirstTime = true
        currentUsername = user?.username
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
        viewModel.isFirstTime = false
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

        etFirstName.doAfterTextChanged { viewModel.setFirstNameValidState(it.isNotNullOrBlank()) }
        etUserName.doAfterTextChanged {
            viewModel.updateUsernameInput(it.toString())
            tvUsernameAlert.isVisible = currentUsername != it.toString()
        }

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
            requireActivity().hideKeyboard()
        }
    }

    private fun isAvatarChanged() = avatarUrl != currentUser?.avatarURL

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
        binding.btnNext.setEnabledOrNot(isAvatarChanged())
        binding.avatar.setImageUrl(filePath)
        viewModel.setAvatarChangedState(filePath)
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

    private fun setUsernameAlert(color: Int, message: String) {
        binding.apply {
            tvUsernameAlert.text = message
            tvUsernameAlert.setTextColor(color)
        }
    }
}