package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.message.Marker
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.databinding.SceytFragmentMessageInfoBinding
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.setBundleArgumentsAs
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.openFile
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageInfoViewProvider
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.SceytMediaActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinfo.adapter.UserMarkerAdapter
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class MessageInfoFragment : Fragment() {
    protected lateinit var binding: SceytFragmentMessageInfoBinding
    protected lateinit var message: SceytMessage
    protected val viewModelFactory by lazy { MessageInfoViewModelFactory(message) }
    protected val viewModel: MessageInfoViewModel by viewModels { viewModelFactory }
    protected var messageViewProvider: MessageInfoViewProvider? = null
    protected var readMarkersAdapter: UserMarkerAdapter? = null
    protected var deliveredMarkersAdapter: UserMarkerAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentMessageInfoBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()

        initViews()
        initViewModel()
        setMessageView()
        setMessageDetails()
        viewModel.getAllMarkers(0, 100)
    }

    private fun getBundleArguments() {
        message = requireNotNull(
            arguments?.parcelable<SceytMessage>(KEY_MESSAGE)
        )
    }

    private fun initViews() {
        binding.toolbar.setNavigationIconClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initViewModel() {
        viewModel.uiState.onEach {
            when (it) {
                is UIState.Success -> {
                    message = it.message
                    updateMessageView()
                    setDividerVisibility(it)
                    setReadUsers(it.readMarkers)
                    setDeliveredUsers(it.deliveredMarkers)
                }

                is UIState.Error -> customToastSnackBar(it.exception?.message ?: "")
                is UIState.Loading -> return@onEach
            }
        }.launchIn(lifecycleScope)
    }

    protected open fun setMessageView() {
        messageViewProvider = getMessageInfoViewProvider()
        messageViewProvider?.displayMessagePreview(binding.viewStub, message)
        messageViewProvider?.setMessageListener(MessageClickListeners.AttachmentClickListener { _, item ->
            onAttachmentClick(item)
        })
    }

    protected open fun updateMessageView() {
        messageViewProvider?.updateMessageStatus(message)
    }

    protected open fun setDividerVisibility(uiState: UIState.Success) {
        binding.dividerDelivered.isVisible = uiState.readMarkers.isNotEmpty() && uiState.deliveredMarkers.isNotEmpty()
    }

    protected open fun setMessageDetails() {
        with(binding) {
            tvSentDate.text = DateTimeUtil.getDateTimeString(message.createdAt, "dd.MM.yy")
            groupSizeViews.isVisible = viewModel.getMessageAttachmentSizeIfExist(message)?.let {
                tvSize.text = it.toPrettySize()
                it
            } != null
        }
    }

    protected open fun setReadUsers(list: List<Marker>) {
        binding.groupViewsRead.isVisible = list.isNotEmpty()
        if (readMarkersAdapter != null) {
            readMarkersAdapter?.submitList(list)
            return
        }

        binding.rvReadByUsers.adapter = UserMarkerAdapter().apply {
            submitList(list)
        }.also { readMarkersAdapter = it }
    }

    protected open fun setDeliveredUsers(list: List<Marker>) {
        binding.groupViewsDelivered.isVisible = list.isNotEmpty()
        if (deliveredMarkersAdapter != null) {
            deliveredMarkersAdapter?.submitList(list)
            return
        }
        binding.rvDeliveredToUsers.adapter = UserMarkerAdapter().apply {
            submitList(list)
        }.also { deliveredMarkersAdapter = it }
    }

    protected open fun getMessageInfoViewProvider(): MessageInfoViewProvider {
        return MessageInfoViewProvider(requireContext())
    }

    protected open fun onAttachmentClick(item: FileListItem) {
        when (item) {
            is FileListItem.Image -> {
                SceytMediaActivity.openMediaView(requireContext(), item.file, item.sceytMessage.user, item.message.channelId)
            }

            is FileListItem.Video -> {
                SceytMediaActivity.openMediaView(requireContext(), item.file, item.sceytMessage.user, item.message.channelId)
            }

            else -> item.file.openFile(requireContext())
        }
    }

    companion object {
        private const val KEY_MESSAGE = "key_message"

        fun newInstance(message: SceytMessage): MessageInfoFragment {
            return MessageInfoFragment().setBundleArgumentsAs {
                putParcelable(KEY_MESSAGE, message)
            }
        }
    }
}