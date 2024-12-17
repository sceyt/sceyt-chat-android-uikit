package com.sceyt.chat.demo.presentation.main.profile.edit

import android.animation.LayoutTransition
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.demo.databinding.FragmentEditProfileBinding
import com.sceyt.chat.demo.presentation.common.ui.handleUsernameValidation
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import com.sceyt.chatuikit.styles.ImageCropperStyle
import com.vanniktech.ui.hideKeyboard
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

open class EditProfileFragment : Fragment() {
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
        binding.root.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }
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
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        viewModel.editProfileErrorLiveData.observe(viewLifecycleOwner) {
            SceytLoader.hideLoading()
            customToastSnackBar(it.toString())
        }

        viewModel.usernameValidationLiveData.observe(viewLifecycleOwner) {
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

        viewModel.usernameInput
            .onEach { username ->
                viewModel.updateUsernameInput(username)
            }.launchIn(lifecycleScope)

        viewModel.nextButtonEnabledLiveData.observe(viewLifecycleOwner) {
            binding.btnNext.setEnabledOrNot(it)
        }
    }

    private fun setUserDetails(user: SceytUser?) {
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
            clearFocus()
            val newUsername = etUserName.text?.trim().toString()
            val newFirstName = etFirstName.text?.trim().toString()
            val newLastName = etLastName.text?.trim().toString()
            val isEditedAvatar = isAvatarChanged()
            val isNeedToSave = newUsername != currentUser?.username
                    || newFirstName != currentUser?.firstName
                    || newLastName != currentUser?.lastName
                    || isEditedAvatar

            if (isNeedToSave) {
                SceytLoader.showLoading(requireContext())
                viewModel.saveProfile(
                    firstName = etFirstName.text.toString(),
                    lastName = etLastName.text.toString(),
                    username = etUserName.text.toString(),
                    avatarUrl = avatarUrl,
                    shouldUploadAvatar = isEditedAvatar
                )
            } else {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            requireActivity().hideKeyboard()
        }
    }

    private fun FragmentEditProfileBinding.clearFocus() {
        etUserName.clearFocus()
        etFirstName.clearFocus()
        etLastName.clearFocus()
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

    private fun isAvatarChanged() = avatarUrl != currentUser?.avatarURL

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
                    if (uris.isNotEmpty()) cropImage(uris[0])
                }
            }

            EditAvatarTypeDialog.EditAvatarType.TakePhoto -> {
                filePickerHelper.takePicture { uri ->
                    cropImage(uri)
                }
            }

            EditAvatarTypeDialog.EditAvatarType.Delete -> {
                setProfileImage(null)
            }
        }
    }

    private fun cropImage(filePath: String?) {
        filePath?.let { path ->
            val uri = Uri.fromFile(File(path))
            val file = File(requireContext().cacheDir.path, System.currentTimeMillis().toString())

            val intent = UCrop.of(uri, Uri.fromFile(file))
                .withOptions(ImageCropperStyle.default(requireContext()).createOptions())
                .withAspectRatio(1f, 1f)
                .getIntent(requireContext())

            cropperActivityResultLauncher.launch(intent)
        }
    }

    protected open val cropperActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val path = UCrop.getOutput(data)?.path
            if (path != null) {
                setProfileImage(path)
            } else customToastSnackBar(getString(com.sceyt.chatuikit.R.string.sceyt_wrong_image))
        }
    }

    private fun setUsernameAlert(color: Int, message: String) {
        binding.apply {
            tvUsernameAlert.text = message
            tvUsernameAlert.setTextColor(color)
        }
    }
}