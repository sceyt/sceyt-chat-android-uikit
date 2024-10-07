package com.sceyt.chatuikit.presentation.components.create_chat.create_channel.fragments

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.setTextViewsHintTextColorRes
import com.sceyt.chatuikit.extensions.setTextViewsTextColor
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.presentation.components.create_chat.create_channel.CreateChannelActivity
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.CreateChatViewModel
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.URIValidation
import com.sceyt.chatuikit.providers.defaults.URIValidationType
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import com.yalantis.ucrop.UCrop
import java.io.File

open class CreateChannelDetailsFragment : Fragment() {
    private lateinit var binding: SceytFragmentCreateChannelDetailsBinding
    private val filePickerHelper = FilePickerHelper(this)
    private val createChannelData by lazy { CreateChannelData(ChannelTypeEnum.Public.value) }
    private val viewModel: CreateChatViewModel by viewModels()
    private val debounceHelper by lazy { DebounceHelper(200, lifecycleScope) }
    private var urlIsValidByServer = false
    private var checkingUrl: String? = null

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
                channelType = ChannelTypeEnum.Public.value
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
                        viewModel.checkIsValidUrl(inputUri.text.toString().lowercase())
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
        val provider = SceytChatUIKit.providers.channelURIValidationMessageProvider
        val colorRes = when (type) {
            URIValidationType.AlreadyTaken,
            URIValidationType.TooLong,
            URIValidationType.TooShort,
            URIValidationType.InvalidCharacters -> R.color.sceyt_color_error

            URIValidationType.FreeToUse -> R.color.sceyt_color_success
        }
        binding.uriWarning.apply {
            text = provider.provide(requireContext(), type)
            setTextColor(requireContext().getCompatColor(colorRes))
            isVisible = true
        }
    }

    private fun setAvatarImage(filePath: String?) {
        createChannelData.avatarUrl = filePath.let {
            val reqSize = SceytChatUIKit.config.avatarResizeConfig.dimensionThreshold
            val quality = SceytChatUIKit.config.avatarResizeConfig.compressionQuality
            resizeImage(requireContext(), it, reqSize, quality).getOrNull() ?: ""
        }
        binding.avatar.setImageUrl(createChannelData.avatarUrl)
    }

    private fun cropImage(filePath: String?) {
        filePath?.let { path ->
            val uri = Uri.fromFile(File(path))

            val file = File(requireContext().cacheDir.path, System.currentTimeMillis().toString())
            val options = UCrop.Options()
            options.setToolbarColor(Color.BLACK)
            options.setStatusBarColor(Color.BLACK)
            options.setCircleDimmedLayer(true)
            options.setShowCropGrid(false)
            options.setShowCropFrame(false)
            options.setToolbarWidgetColor(requireContext().getCompatColor(R.color.sceyt_color_on_primary))
            options.setToolbarTitle(getString(R.string.sceyt_move_and_scale))
            options.setHideBottomControls(true)

            UCrop.of(uri, Uri.fromFile(file))
                .withOptions(options)
                .withAspectRatio(1f, 1f)
                .start(requireContext(), this)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

    private fun SceytFragmentCreateChannelDetailsBinding.applyStyle() {
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))
        setTextViewsTextColor(listOf(inputSubject, inputUri, inputDescription, uriPrefix),
            requireContext().getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
        setTextViewsHintTextColorRes(listOf(inputSubject, inputUri, inputDescription),
            SceytChatUIKit.theme.colors.textFootnoteColor)
        uriWarning.setTextColor(requireContext().getCompatColor(SceytChatUIKit.theme.colors.errorColor))
    }
}