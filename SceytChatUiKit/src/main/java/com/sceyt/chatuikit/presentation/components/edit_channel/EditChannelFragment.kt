package com.sceyt.chatuikit.presentation.components.edit_channel

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelDescriptionData
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentEditChannelBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.jsonToObject
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.common.SceytLoader.showLoading
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.presentation.components.channel_info.members.ChannelMembersFragment
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.URIValidation
import com.sceyt.chatuikit.presentation.components.edit_channel.viewmodel.EditChannelViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.providers.defaults.URIValidationType
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import com.sceyt.chatuikit.styles.EditChannelStyle
import com.sceyt.chatuikit.styles.ImageCropperStyle
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

open class EditChannelFragment : Fragment(), SceytKoinComponent {
    protected var binding: SceytFragmentEditChannelBinding? = null
    protected val viewModel: EditChannelViewModel by viewModels()
    protected val filePickerHelper = FilePickerHelper(this)
    protected var avatarUrl: String? = null
    private var urlIsValidByServer = false
    private var checkingUri: String? = null
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var style: EditChannelStyle
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = EditChannelStyle.Builder(context, null).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentEditChannelBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()
        binding?.applyStyle()
        binding?.initViews()
        setDetails()
    }

    protected open fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelMembersFragment.CHANNEL))
    }

    protected open fun initViewModel() {
        viewModel.editChannelLiveData.observe(viewLifecycleOwner) {
            SceytLoader.hideLoading()
            lifecycleScope.launch {
                delay(100)
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        viewModel.isValidUrlLiveData.observe(viewLifecycleOwner) { (isValid, url) ->
            if (url != binding?.inputUri?.text?.toString()) return@observe

            urlIsValidByServer = isValid
            if (isValid) {
                checkSaveEnabled(false)
                setUriStatusText(URIValidationType.FreeToUse)
            } else
                setUriStatusText(URIValidationType.AlreadyTaken)
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner) {
            if (it is PageState.StateError) {
                customToastSnackBar(it.errorMessage.toString())
                SceytLoader.hideLoading()
            }
        }
    }

    protected open fun SceytFragmentEditChannelBinding.initViews() {
        tvSubject.doAfterTextChanged { checkSaveEnabled(false) }

        inputUri.doAfterTextChanged {
            urlIsValidByServer = false
            binding?.uriWarning?.isVisible = false
            checkingUri = null
            checkSaveEnabled(true)
        }

        toolbar.setNavigationClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        avatar.setOnClickListener {
            onChangePhotoClick()
        }

        icSave.setOnClickListener {
            onSaveClick()
        }
    }

    protected open fun setDetails() {
        avatarUrl = channel.avatarUrl
        with(binding ?: return) {
            groupUrl.isVisible = channel.isPublic()
            avatar.setImageUrl(avatarUrl)
            inputUri.setText(channel.uri)
            tvSubject.setText(SceytChatUIKit.formatters.channelNameFormatter.format(requireContext(), channel))
            uriPrefix.text = SceytChatUIKit.config.channelURIConfig.prefix
            tvDescription.setText(channel.metadata.jsonToObject(ChannelDescriptionData::class.java)?.description?.trim())
        }
        checkSaveEnabled(true)
    }

    protected open fun checkSaveEnabled(checkUriFormat: Boolean) {
        with(binding ?: return) {
            val isValidSubject = tvSubject.text?.trim().isNotNullOrBlank()
            when {
                !isValidSubject -> {
                    icSave.setEnabledOrNot(false)
                    return
                }

                channel.isPublic() -> {
                    val inputUrl = inputUri.text?.trim().toString()

                    if (inputUrl == channel.uri) {
                        icSave.setEnabledOrNot(true)
                        return
                    }

                    icSave.setEnabledOrNot(urlIsValidByServer)

                    if (urlIsValidByServer)
                        return

                    if (!checkUriFormat) return

                    val isValid = viewModel.checkIsValidUrlFormat(inputUrl)
                    when (isValid) {
                        URIValidation.Valid -> {
                            if (checkingUri != inputUri.text.toString()) {
                                checkingUri = inputUri.text.toString()
                                viewModel.checkIsValidUri(inputUri.text.toString().lowercase())
                            }
                        }

                        URIValidation.TooShort -> disableWithError(URIValidationType.TooShort)
                        URIValidation.TooLong -> disableWithError(URIValidationType.TooLong)
                        URIValidation.InvalidCharacters -> disableWithError(URIValidationType.InvalidCharacters)
                    }
                }

                else -> icSave.setEnabledOrNot(true)
            }
        }
    }

    protected open fun disableWithError(type: URIValidationType) {
        with(binding ?: return) {
            uriWarning.isVisible = true
            icSave.setEnabledOrNot(false)
            setUriStatusText(type)
        }
    }

    protected open fun setUriStatusText(type: URIValidationType) {
        val provider = SceytChatUIKit.providers.channelURIValidationMessageProvider
        val style = when (type) {
            URIValidationType.AlreadyTaken,
            URIValidationType.TooLong,
            URIValidationType.TooShort,
            URIValidationType.InvalidCharacters,
            -> style.uriValidationStyle.errorTextStyle

            URIValidationType.FreeToUse -> style.uriValidationStyle.successTextStyle
        }
        binding?.uriWarning?.apply {
            text = provider.provide(requireContext(), type)
            style.apply(this)
            isVisible = true
        }
    }

    protected open fun setProfileImage(filePath: String?) {
        val reqSize = SceytChatUIKit.config.avatarResizeConfig.dimensionThreshold
        val quality = SceytChatUIKit.config.avatarResizeConfig.compressionQuality
        avatarUrl = resizeImage(
            path = filePath,
            parentDir = requireContext().filesDir,
            reqSize = reqSize,
            quality = quality
        ).getOrNull()
        binding?.avatar?.setImageUrl(avatarUrl)
        checkSaveEnabled(false)
    }

    protected open fun onChangePhotoClick() {
        EditAvatarTypeDialog(requireContext(), avatarUrl.isNullOrBlank().not()) {
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
                    setProfileImage(null)
                }
            }
        }.show()
    }

    protected open fun cropImage(filePath: String?) {
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
            } else customToastSnackBar(getString(R.string.sceyt_wrong_image))
        }
    }

    protected open fun onSaveClick() {
        val newSubject = binding?.tvSubject?.text?.trim().toString()
        val newDescription = binding?.tvDescription?.text?.trim().toString()
        val newUrl = binding?.inputUri?.text?.trim().toString()
        val isEditedAvatar = avatarUrl != channel.avatarUrl
        val oldDesc = channel.metadata.jsonToObject(ChannelDescriptionData::class.java)?.description?.trim()
        val isEditedSubjectOrDesc = newSubject != channel.subject?.trim() || newDescription != oldDesc
        val isEditedUrd = newUrl != channel.uri
        if (isEditedAvatar || isEditedSubjectOrDesc || isEditedUrd) {
            showLoading(requireContext())
            val data = EditChannelData(newSubject = newSubject,
                metadata = Gson().toJson(ChannelDescriptionData(newDescription)),
                avatarUrl = avatarUrl,
                channelUri = newUrl,
                channelType = channel.type,
                avatarEdited = isEditedAvatar)
            viewModel.editChannelChanges(channel.id, data)
        } else requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    protected open fun SceytFragmentEditChannelBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
        toolbar.setTitle(style.toolbarTitle)
        style.subjectTextInputStyle.apply(tvSubject, null)
        style.aboutTextInputStyle.apply(tvDescription, null)
        style.uriTextInputStyle.apply(inputUri, null)
        subjectDivider.setBackgroundColor(style.dividerColor)
        aboutDivider.setBackgroundColor(style.dividerColor)
        uriDivider.setBackgroundColor(style.dividerColor)
        style.uriTextInputStyle.textStyle.apply(uriPrefix)
        avatar.appearanceBuilder()
            .setDefaultAvatar(style.avatarPlaceholder)
            .setStyle(style.avatarStyle)
            .build()
            .applyToAvatar()

        style.saveButtonStyle.applyToCustomButton(icSave)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): EditChannelFragment {
            val fragment = EditChannelFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}