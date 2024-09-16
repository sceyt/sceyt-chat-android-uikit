package com.sceyt.chatuikit.presentation.uicomponents.messageinfo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytFragmentMessageInfoBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setBundleArgumentsAs
import com.sceyt.chatuikit.extensions.setTextViewsTextColor
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.openFile
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageInfoViewProvider
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.mediaview.MediaPreviewActivity
import com.sceyt.chatuikit.presentation.uicomponents.messageinfo.adapter.UserMarkerAdapter
import com.sceyt.chatuikit.sceytstyles.MessageInfoStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Suppress("MemberVisibilityCanBePrivate")
open class MessageInfoFragment : Fragment() {
    protected var binding: SceytFragmentMessageInfoBinding? = null
    protected var messageId: Long = 0
    protected var channelId: Long = 0
    protected val viewModelFactory by lazy { MessageInfoViewModelFactory(messageId, channelId) }
    protected val viewModel: MessageInfoViewModel by viewModels { viewModelFactory }
    protected val messageViewProvider: MessageInfoViewProvider by lazy { getMessageInfoViewProvider() }
    protected var readMarkersAdapter: UserMarkerAdapter? = null
    protected var deliveredMarkersAdapter: UserMarkerAdapter? = null
    protected var playedMarkersAdapter: UserMarkerAdapter? = null
    protected lateinit var style: MessageInfoStyle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentMessageInfoBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()

        binding?.applyStyle()
        initViews()
        initViewModel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = MessageInfoStyle.Builder(context, null).build()
    }

    private fun getBundleArguments() {
        messageId = arguments?.getLong(KEY_MESSAGE_ID) ?: 0
        channelId = arguments?.getLong(KEY_CHANNEL_ID) ?: 0
    }

    private fun initViews() {
        binding?.toolbar?.setNavigationIconClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initViewModel() {
        viewModel.uiState.onEach {
            when (it) {
                is UIState.Success -> onUiStateSuccess(it)
                is UIState.Error -> onUIStateError(it)
                is UIState.Loading -> onUIStateLoading(it)
            }
        }.launchIn(lifecycleScope)

        viewModel.initMessageViewFlow.onEach {
            onMessage(it)
        }.launchIn(lifecycleScope)


        viewModel.linkPreviewLiveData.observe(viewLifecycleOwner) {
            messageViewProvider.updateMessage(it)
        }
    }

    protected open fun onUiStateSuccess(it: UIState.Success) {
        updateMessageView(it.message)
        setReadUsers(it.readMarkers)
        setDeliveredUsers(it.deliveredMarkers)
        setPlayedUsers(it.playedMarkers)
    }

    protected open fun onUIStateError(it: UIState.Error) {
        customToastSnackBar(it.exception?.message ?: "")
    }

    protected open fun onUIStateLoading(uiState: UIState.Loading) {

    }

    protected open fun onMessage(message: SceytMessage) {
        setMessageView(message, binding?.viewStub)
        setMessageDetails(message)
    }

    protected open fun setMessageView(message: SceytMessage?, viewStub: ViewStub?) {
        viewStub ?: return
        messageViewProvider.displayMessagePreview(viewStub, message ?: return)
    }

    protected open fun updateMessageView(message: SceytMessage?) {
        messageViewProvider.updateMessageStatus(message ?: return)
    }

    protected open fun setMessageDetails(message: SceytMessage?) {
        message ?: return
        with(binding ?: return) {
            tvSentDate.text = DateTimeUtil.getDateTimeString(message.createdAt, "dd.MM.yy")
            groupSizeViews.isVisible = viewModel.getMessageAttachmentSizeIfExist(message)?.let {
                tvSize.text = it.toPrettySize()
                it
            } != null
        }
    }

    protected open fun setReadUsers(list: List<SceytMarker>) {
        binding?.groupViewsRead?.isVisible = list.isNotEmpty()
        if (readMarkersAdapter != null) {
            readMarkersAdapter?.submitList(list)
            return
        }

        binding?.rvReadByUsers?.adapter = UserMarkerAdapter().apply {
            submitList(list)
        }.also { readMarkersAdapter = it }
    }

    protected open fun setDeliveredUsers(list: List<SceytMarker>) {
        binding?.groupViewsDelivered?.isVisible = list.isNotEmpty()
        if (deliveredMarkersAdapter != null) {
            deliveredMarkersAdapter?.submitList(list)
            return
        }
        binding?.rvDeliveredToUsers?.adapter = UserMarkerAdapter().apply {
            submitList(list)
        }.also { deliveredMarkersAdapter = it }
    }

    protected open fun setPlayedUsers(list: List<SceytMarker>) {
        binding?.groupViewsPlayed?.isVisible = list.isNotEmpty()
        if (playedMarkersAdapter != null) {
            playedMarkersAdapter?.submitList(list)
            return
        }
        binding?.rvPlayedByUsers?.adapter = UserMarkerAdapter().apply {
            submitList(list)
        }.also { playedMarkersAdapter = it }
    }

    protected open fun getMessageInfoViewProvider(): MessageInfoViewProvider {
        return MessageInfoViewProvider(requireContext()).also { provider ->
            provider.setMessageListener(MessageClickListeners.AttachmentClickListener { _, item, message ->
                onAttachmentClick(item, message)
            })

            provider.setNeedMediaDataCallback { viewModel.needMediaInfo(it) }
        }
    }

    protected open fun onAttachmentClick(item: FileListItem, message: SceytMessage) {
        when (item) {
            is FileListItem.Image -> {
                MediaPreviewActivity.launch(requireContext(), item.file, message.user, message.channelId)
            }

            is FileListItem.Video -> {
                MediaPreviewActivity.launch(requireContext(), item.file, message.user, message.channelId)
            }

            else -> item.file.openFile(requireContext())
        }
    }

    protected open fun SceytFragmentMessageInfoBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        toolbar.setBackgroundColor(style.toolbarColor)
        toolbar.setTitleColor(style.titleColor)
        toolbar.setTitle(style.title)
        setTextViewsTextColor(listOf(tvSentDate, tvPlayedByHint, tvReadByHint, tvDeliveredToHint),
            requireContext().getCompatColor(SceytChatUIKit.theme.textSecondaryColor))
        divider.setBackgroundColor(style.borderColor)
        dividerPlayed.setBackgroundColor(style.borderColor)
        dividerRead.setBackgroundColor(style.borderColor)
        dividerPlayed.setBackgroundColor(style.borderColor)
        dividerDelivered.setBackgroundColor(style.borderColor)
    }

    companion object {
        const val KEY_MESSAGE_ID = "key_message_id"
        const val KEY_CHANNEL_ID = "key_channel_id"

        fun newInstance(message: SceytMessage): MessageInfoFragment {
            return MessageInfoFragment().setBundleArgumentsAs {
                putLong(KEY_MESSAGE_ID, message.id)
                putLong(KEY_CHANNEL_ID, message.channelId)
            }
        }

        fun newInstance(messageId: Long, channelId: Long): MessageInfoFragment {
            return MessageInfoFragment().setBundleArgumentsAs {
                putLong(KEY_MESSAGE_ID, messageId)
                putLong(KEY_CHANNEL_ID, channelId)
            }
        }
    }
}