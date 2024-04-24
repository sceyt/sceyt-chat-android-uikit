package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.editchannel

import android.os.Bundle
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
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentEditChannelBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.jsonToObject
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.extensions.resizeImage
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.common.SceytLoader.showLoading
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.editchannel.viewmodel.EditChannelViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.chatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class EditChannelFragment : Fragment(), SceytKoinComponent {
    protected var binding: SceytFragmentEditChannelBinding? = null
    protected val viewModel: EditChannelViewModel by viewModels()
    protected val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    protected var avatarUrl: String? = null
    private var urlIsValidByServer = false
    private var checkingUrl: String? = null
    protected lateinit var channel: SceytChannel
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentEditChannelBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()
        binding?.setupStyle()
        binding?.initViews()
        setDetails()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelMembersFragment.CHANNEL))
    }

    private fun initViewModel() {
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
            if (isValid)
                checkSaveEnabled(false)
            binding?.uriWarning?.apply {
                if (!isValid) {
                    setUriStatusText(getString(R.string.the_url_exist_title), R.color.sceyt_color_error)
                } else
                    setUriStatusText(getString(R.string.valid_url_title), R.color.sceyt_color_green)
                isVisible = true
            }
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner) {
            if (it is PageState.StateError) {
                customToastSnackBar(it.errorMessage.toString())
                SceytLoader.hideLoading()
            }
        }
    }

    private fun SceytFragmentEditChannelBinding.initViews() {
        tvSubject.doAfterTextChanged { checkSaveEnabled(false) }

        inputUri.doAfterTextChanged {
            urlIsValidByServer = false
            binding?.uriWarning?.isVisible = false
            checkingUrl = null
            checkSaveEnabled(true)
        }

        layoutToolbar.navigationIcon.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        avatar.setOnClickListener {
            onChangePhotoClick()
        }

        icSave.setOnClickListener {
            onSaveClick()
        }
    }

    open fun setDetails() {
        avatarUrl = channel.avatarUrl
        with(binding ?: return) {
            groupUrl.isVisible = channel.isPublic()
            avatar.setImageUrl(avatarUrl)
            inputUri.setText(channel.uri)
            tvSubject.setText(channel.channelSubject.trim())
            tvDescription.setText(channel.metadata.jsonToObject(ChannelDescriptionData::class.java)?.description?.trim())
        }
    }

    open fun checkSaveEnabled(checkUriFormat: Boolean) {
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

                    val isValid = checkIsValidUrlFormat(inputUrl)

                    if (isValid && checkingUrl != inputUri.text.toString()) {
                        checkingUrl = inputUri.text.toString()
                        viewModel.checkIsValidUrl(inputUri.text.toString().lowercase())
                    } else
                        icSave.setEnabledOrNot(false)
                }

                else -> icSave.setEnabledOrNot(true)
            }
        }
    }

    open fun checkIsValidUrlFormat(url: String): Boolean {
        with(binding ?: return false) {
            val isValidUrl = "^\\w{5,50}".toPattern().matcher(url).matches()
            if (!isValidUrl) {
                if (inputUri.text.toString().length < 5 || inputUri.text.toString().length > 50)
                    setUriStatusText(getString(R.string.url_length_validation_text), R.color.sceyt_color_error)
                else
                    setUriStatusText(getString(R.string.url_characters_validation_text), R.color.sceyt_color_error)
                uriWarning.isVisible = true
            }
            return isValidUrl
        }
    }

    open fun setUriStatusText(title: String, @ColorRes color: Int) {
        binding?.uriWarning?.apply {
            text = title
            setTextColor(requireContext().getCompatColor(color))
        }
    }

    open fun onAvatarImageSelected(filePath: String?) {
        if (filePath != null) {
            setProfileImage(filePath)
        } else customToastSnackBar("Wrong image")
    }

    open fun setProfileImage(filePath: String?) {
        avatarUrl = resizeImage(requireContext(), filePath, 500).getOrNull()
        binding?.avatar?.setImageUrl(avatarUrl)
        checkSaveEnabled(false)
    }

    open fun onChangePhotoClick() {
        EditAvatarTypeDialog(requireContext(), avatarUrl.isNullOrBlank().not()) {
            when (it) {
                EditAvatarTypeDialog.EditAvatarType.ChooseFromGallery -> {
                    chooseAttachmentHelper.chooseFromGallery(allowMultiple = false, onlyImages = true) { uris ->
                        if (uris.isNotEmpty())
                            onAvatarImageSelected(uris[0])
                    }
                }

                EditAvatarTypeDialog.EditAvatarType.TakePhoto -> {
                    chooseAttachmentHelper.takePicture { uri ->
                        onAvatarImageSelected(uri)
                    }
                }

                EditAvatarTypeDialog.EditAvatarType.Delete -> {
                    setProfileImage(null)
                }
            }
        }.show()
    }

    open fun onSaveClick() {
        val newSubject = binding?.tvSubject?.text?.trim().toString()
        val newDescription = binding?.tvDescription?.text?.trim().toString()
        val newUrl = binding?.inputUri?.text?.trim().toString()
        val isEditedAvatar = avatarUrl != channel.avatarUrl
        val oldDesc = channel.metadata.jsonToObject(ChannelDescriptionData::class.java)?.description?.trim()
        val isEditedSubjectOrDesc = newSubject != channel.channelSubject.trim() || newDescription != oldDesc
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

    private fun SceytFragmentEditChannelBinding.setupStyle() {
        layoutToolbar.setIconsTint(SceytChatUIKit.theme.accentColor)
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