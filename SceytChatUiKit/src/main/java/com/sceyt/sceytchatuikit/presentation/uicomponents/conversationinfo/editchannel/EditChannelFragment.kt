package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.editchannel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.data.models.channels.EditChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.databinding.FragmentEditChannelBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.persistence.extensions.resizeImage
import com.sceyt.sceytchatuikit.presentation.common.SceytLoader
import com.sceyt.sceytchatuikit.presentation.common.SceytLoader.showLoading
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.dialogs.EditAvatarTypeDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment.ChooseAttachmentHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

open class EditChannelFragment : Fragment(), SceytKoinComponent {
    protected var binding: FragmentEditChannelBinding? = null
    protected val viewModel: ConversationInfoViewModel by viewModel()
    protected val chooseAttachmentHelper = ChooseAttachmentHelper(this)
    protected var avatarUrl: String? = null
    protected lateinit var channel: SceytChannel
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentEditChannelBinding.inflate(inflater, container, false)
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
                requireActivity().finish()
            }
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner) {
            if (it is PageState.StateError) {
                customToastSnackBar(requireView(), it.errorMessage.toString())
                SceytLoader.hideLoading()
            }
        }
    }

    private fun FragmentEditChannelBinding.initViews() {
        tvSubject.doAfterTextChanged { checkSaveState() }
        tvDescription.doAfterTextChanged { checkSaveState() }

        layoutToolbar.navigationIcon.setOnClickListener {
            requireActivity().finish()
        }

        icChangePhoto.setOnClickListener {
            onChangePhotoClick()
        }

        icSave.setOnClickListener {
            onSaveClick()
        }
    }

    private fun setDetails() {
        avatarUrl = channel.getChannelAvatarUrl()
        with(binding ?: return) {
            avatar.setImageUrl(avatarUrl)
            tvSubject.setText(channel.channelSubject.trim())
            tvDescription.setText(channel.label?.trim())
        }
    }

    private fun checkSaveState() {
        with(binding ?: return) {
            val enable = tvSubject.text?.trim().isNotNullOrBlank() && tvDescription.text?.trim().isNotNullOrBlank()
            icSave.setEnabledOrNot(enable)
        }
    }

    open fun onAvatarImageSelected(filePath: String?) {
        if (filePath != null) {
            setProfileImage(filePath)
        } else customToastSnackBar(binding?.root, "Wrong image")
    }

    open fun setProfileImage(filePath: String?) {
        avatarUrl = resizeImage(requireContext(), filePath, 500).getOrNull()
        binding?.avatar?.setImageUrl(avatarUrl)
        checkSaveState()
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
        val isEditedAvatar = avatarUrl != channel.getChannelAvatarUrl()
        val isEditedSubjectOrDesc = newSubject != channel.channelSubject.trim() || newDescription != channel.label?.trim()
        if (isEditedAvatar || isEditedSubjectOrDesc) {
            showLoading(requireContext())
            val data = EditChannelData(newSubject = newSubject,
                metadata = channel.metadata,
                label = newDescription,
                avatarUrl = avatarUrl,
                channelUrl = (channel as? SceytGroupChannel)?.channelUrl,
                channelType = channel.channelType,
                avatarEdited = isEditedAvatar)
            viewModel.saveChanges(channel.id, data)
        } else requireActivity().finish()
    }

    private fun FragmentEditChannelBinding.setupStyle() {
        layoutToolbar.setIconsTint(SceytKitConfig.sceytColorAccent)
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