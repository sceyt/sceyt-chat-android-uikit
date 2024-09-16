package com.sceyt.chatuikit.presentation.uicomponents.createchat.createchannel.fragments

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
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
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.presentation.uicomponents.createchat.createchannel.CreateChannelActivity
import com.sceyt.chatuikit.presentation.uicomponents.createchat.viewmodel.CreateChatViewModel
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.chatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.yalantis.ucrop.UCrop
import java.io.File

class CreateChannelDetailsFragment : Fragment() {
    private lateinit var binding: SceytFragmentCreateChannelDetailsBinding
    private val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    private val createChannelData by lazy { CreateChannelData(ChannelTypeEnum.Public.getString()) }
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
    }

    private fun initViewModel() {
        viewModel.isValidUrlLiveData.observe(viewLifecycleOwner) {
            urlIsValidByServer = it
            if (it)
                checkNextEnabled(false)
            binding.uriWarning.apply {
                if (!it) {
                    setUriStatusText(getString(R.string.sceyt_the_url_exist_title), R.color.sceyt_color_error)
                } else
                    setUriStatusText(getString(R.string.sceyt_valid_url_title), R.color.sceyt_color_green)
                isVisible = true
            }
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
                        chooseAttachmentHelper.chooseFromGallery(allowMultiple = false, onlyImages = true) { uris ->
                            if (uris.isNotEmpty())
                                cropImage(uris[0])
                        }
                    }

                    EditAvatarTypeDialog.EditAvatarType.TakePhoto -> {
                        chooseAttachmentHelper.takePicture { uri ->
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
                channelType = ChannelTypeEnum.Public.getString()
                uri = inputUri.text?.toString()?.lowercase()?.run { "@$this" } ?: ""
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

            val isValid = checkIsValidUrl(inputUri.text ?: run {
                fabNext.setEnabledOrNot(false)
                return
            })

            if (isValid && checkingUrl != inputUri.text.toString()) {
                checkingUrl = inputUri.text.toString()
                viewModel.checkIsValidUrl("@${inputUri.text.toString().lowercase()}")
            } else {
                fabNext.setEnabledOrNot(false)
            }
        }
    }

    private fun checkIsValidUrl(url: Editable?): Boolean {
        with(binding) {
            val isValidUrl = "^\\w{5,50}".toPattern().matcher(url
                    ?: return false).matches()
            if (!isValidUrl) {
                if (inputUri.text.toString().length < 5 || inputUri.text.toString().length > 50)
                    setUriStatusText(getString(R.string.sceyt_url_length_validation_text), R.color.sceyt_color_error)
                else
                    setUriStatusText(getString(R.string.sceyt_url_characters_validation_text), R.color.sceyt_color_error)
            }
            uriWarning.isVisible = true
            return isValidUrl
        }
    }

    private fun setUriStatusText(title: String, @ColorRes color: Int) {
        binding.uriWarning.apply {
            text = title
            setTextColor(requireContext().getCompatColor(color))
        }
    }

    private fun setAvatarImage(filePath: String?) {
        createChannelData.avatarUrl = filePath.let {
            resizeImage(requireContext(), it, 500).getOrNull() ?: ""
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
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColor))
        setTextViewsTextColor(listOf(inputSubject, inputUri, inputDescription, uriBegin),
            requireContext().getCompatColor(SceytChatUIKit.theme.textPrimaryColor))
        setTextViewsHintTextColorRes(listOf(inputSubject, inputUri, inputDescription),
            SceytChatUIKit.theme.textFootnoteColor)
        uriWarning.setTextColor(requireContext().getCompatColor(SceytChatUIKit.theme.errorColor))
    }
}