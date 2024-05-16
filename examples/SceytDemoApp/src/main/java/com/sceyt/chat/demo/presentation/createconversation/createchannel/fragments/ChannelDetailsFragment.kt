package com.sceyt.chat.demo.presentation.createconversation.createchannel.fragments

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
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
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.databinding.FragmentCreateChannelDetailsBinding
import com.sceyt.chat.demo.presentation.createconversation.createchannel.CreateChannelActivity
import com.sceyt.chatuikit.R.*
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.chat.demo.presentation.createconversation.viewmodel.CreateChatViewModel
import com.sceyt.chatuikit.data.models.channels.ChannelDescriptionData
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.chatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import com.yalantis.ucrop.UCrop
import java.io.File

class ChannelDetailsFragment : Fragment(R.layout.fragment_create_channel_details) {
    private lateinit var binding: FragmentCreateChannelDetailsBinding
    private val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    private val createChannelData by lazy { CreateChannelData(ChannelTypeEnum.Public.getString()) }
    private val viewModel: CreateChatViewModel by viewModels()
    private val debounceHelper by lazy { DebounceHelper(200, lifecycleScope) }
    private var urlIsValidByServer = false
    private var checkingUrl: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentCreateChannelDetailsBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        binding.initViews()
    }

    private fun initViewModel() {
        viewModel.isValidUrlLiveData.observe(viewLifecycleOwner) {
            urlIsValidByServer = it
            if (it)
                checkNextEnabled(false)
            binding.uriWarning.apply {
                if (!it) {
                    setUriStatusText(getString(string.the_url_exist_title), color.sceyt_color_error)
                } else
                    setUriStatusText(getString(string.valid_url_title), color.sceyt_color_green)
                isVisible = true
            }
        }
    }

    private fun FragmentCreateChannelDetailsBinding.initViews() {
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
                uri = inputUri.text?.toString()?.lowercase() ?: ""
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
                viewModel.checkIsValidUrl(inputUri.text.toString().lowercase())
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
                    setUriStatusText(getString(string.url_length_validation_text), color.sceyt_color_error)
                else
                    setUriStatusText(getString(string.url_characters_validation_text),color.sceyt_color_error)
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
            options.setToolbarColor(requireContext().getCompatColor(R.color.black))
            options.setStatusBarColor(requireContext().getCompatColor(R.color.black))
            options.setCircleDimmedLayer(true)
            options.setShowCropGrid(false)
            options.setShowCropFrame(false)
            options.setToolbarWidgetColor(requireContext().getCompatColor(R.color.white))
            options.setToolbarTitle(getString(R.string.move_and_scale))
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
                    } else customToastSnackBar(getString(R.string.wrong_image))
                } else customToastSnackBar(getString(R.string.wrong_image))
            }
        }
    }
}