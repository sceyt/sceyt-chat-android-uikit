package com.sceyt.chatuikit.presentation.components.create_chat.create_channel.fragments

import android.animation.LayoutTransition
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
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.databinding.SceytFragmentCreateChannelDetailsBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.presentation.components.create_chat.create_channel.CreateChannelActivity
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.CreateChatViewModel
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.URIValidation
import com.sceyt.chatuikit.providers.defaults.URIValidationType
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import com.sceyt.chatuikit.styles.create_channel.CreateChannelStyle
import com.sceyt.chatuikit.styles.cropper.ImageCropperStyle
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.yalantis.ucrop.UCrop
import java.io.File

open class CreateChannelDetailsFragment : Fragment() {
    private lateinit var binding: SceytFragmentCreateChannelDetailsBinding
    private lateinit var style: CreateChannelStyle
    private val filePickerHelper = FilePickerHelper(this)
    private val createChannelData by lazy { CreateChannelData(ChannelTypeEnum.Public.value) }
    private val viewModel: CreateChatViewModel by viewModels()
    private val debounceHelper by lazy { DebounceHelper(200, lifecycleScope) }
    private var urlIsValidByServer = false
    private var checkingUrl: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = CreateChannelStyle.Builder(context, null).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentCreateChannelDetailsBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        binding.initViews()
        binding.applyStyle()
        checkNextEnabled(false)
    }

    private fun initViewModel() {
        viewModel.isValidUrlLiveData.observe(viewLifecycleOwner) { isValid ->
            urlIsValidByServer = isValid
            if (isValid) {
                checkNextEnabled(false)
                setUriStatusText(URIValidationType.FreeToUse)
            } else
                setUriStatusText(URIValidationType.AlreadyTaken)
        }
    }

    private fun SceytFragmentCreateChannelDetailsBinding.initViews() {
        binding.root.layoutTransition = LayoutTransition().apply { enableTransitionType(LayoutTransition.CHANGING) }

        inputSubject.doAfterTextChanged {
            checkNextEnabled(false)
        }

        inputUri.doAfterTextChanged {
            debounceHelper.submit {
                urlIsValidByServer = false
                checkingUrl = null
                checkNextEnabled(true)
            }
        }

        avatar.setOnClickListener {
            EditAvatarTypeDialog(requireContext(), createChannelData.avatarUrl.isNotBlank()) {
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

        fabNext.setOnClickListener {
            with(createChannelData) {
                subject = inputSubject.text.toString().trim()
                type = ChannelTypeEnum.Public.value
                uri = inputUri.text?.toString()?.trim()?.lowercase().toString()
                metadata = Gson().toJson(ChannelDescriptionData(inputDescription.text.toString().trim()))
                members = arrayListOf()
            }

            (requireActivity() as CreateChannelActivity).createChannel(createChannelData)
            requireActivity().hideSoftInput()
        }
    }

    private fun checkNextEnabled(checkUri: Boolean) {
        with(binding) {
            val isValidSubject = inputSubject.text?.trim().isNotNullOrBlank()
            if (!isValidSubject) {
                fabNext.setEnabledOrNot(false)
            } else {
                if (urlIsValidByServer) {
                    fabNext.setEnabledOrNot(true)
                    return
                }
            }

            if (!checkUri) return

            val isValid = viewModel.checkIsValidUrlFormat(inputUri.text?.toString() ?: run {
                fabNext.setEnabledOrNot(false)
                return
            })
            when (isValid) {
                URIValidation.Valid -> {
                    if (checkingUrl != inputUri.text.toString()) {
                        checkingUrl = inputUri.text.toString()
                        viewModel.checkIsValidUri(inputUri.text.toString().lowercase())
                    }
                }

                URIValidation.TooShort -> disableWithError(URIValidationType.TooShort)
                URIValidation.TooLong -> disableWithError(URIValidationType.TooLong)
                URIValidation.InvalidCharacters -> disableWithError(URIValidationType.InvalidCharacters)
            }
        }
    }

    open fun disableWithError(type: URIValidationType) {
        with(binding) {
            uriWarning.isVisible = true
            fabNext.setEnabledOrNot(false)
            setUriStatusText(type)
        }
    }

    open fun setUriStatusText(type: URIValidationType) {
        val textStyle = when (type) {
            URIValidationType.AlreadyTaken,
            URIValidationType.TooLong,
            URIValidationType.TooShort,
            URIValidationType.InvalidCharacters,
                -> style.uriValidationStyle.errorTextStyle

            URIValidationType.FreeToUse -> style.uriValidationStyle.successTextStyle
        }
        binding.uriWarning.apply {
            text = style.uriValidationStyle.messageProvider.provide(requireContext(), type)
            textStyle.apply(this)
            isVisible = true
        }
    }

    private fun setAvatarImage(filePath: String?) {
        createChannelData.avatarUrl = filePath.let {
            val reqSize = SceytChatUIKit.config.avatarResizeConfig.dimensionThreshold
            val quality = SceytChatUIKit.config.avatarResizeConfig.compressionQuality
            resizeImage(
                path = it,
                parentDir = requireContext().cacheDir,
                reqSize = reqSize,
                quality = quality
            ).getOrNull() ?: ""
        }
        binding.avatar.setImageUrl(createChannelData.avatarUrl)
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

    private val cropperActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val path = UCrop.getOutput(data)?.path
            if (path != null) {
                setAvatarImage(path)
            } else customToastSnackBar(getString(R.string.sceyt_wrong_image))
        }
    }

    private fun SceytFragmentCreateChannelDetailsBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        avatar.appearanceBuilder()
            .setDefaultAvatar(style.avatarDefaultIcon)
            .setStyle(AvatarStyle(avatarBackgroundColor = style.avatarBackgroundColor))
            .build()
            .applyToAvatar()
        style.nameTextFieldStyle.apply(inputSubject, null)
        style.aboutTextFieldStyle.apply(inputDescription, null)
        style.uriTextFieldStyle.apply(inputUri, null)
        style.captionTextStyle.apply(uriPrefix)
        style.captionTextStyle.apply(uriWarning)
        style.actionButtonStyle.applyToCustomButton(fabNext)
        uriUnderline.setBackgroundColor(style.dividerColor)
    }
}