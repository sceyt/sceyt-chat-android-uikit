package com.sceyt.chatuikit.presentation.components.message_info

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
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytFragmentMessageInfoBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.setBundleArgumentsAs
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.openFile
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageInfoViewProvider
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewActivity
import com.sceyt.chatuikit.presentation.components.message_info.adapter.UserMarkerAdapter
import com.sceyt.chatuikit.presentation.components.message_info.viewmodel.MessageInfoViewModel
import com.sceyt.chatuikit.presentation.components.message_info.viewmodel.MessageInfoViewModelFactory
import com.sceyt.chatuikit.presentation.components.message_info.viewmodel.UIState
import com.sceyt.chatuikit.styles.MessageInfoStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Date

@Suppress("MemberVisibilityCanBePrivate")
open class MessageInfoFragment : Fragment {
    constructor() : super()

    constructor(messageItemStyle: MessageItemStyle) : super() {
        this.messageItemStyle = messageItemStyle
    }

    protected var binding: SceytFragmentMessageInfoBinding? = null
    protected var messageId: Long = 0
    protected var channelId: Long = 0
    protected val viewModelFactory by lazy { provideViewModelFactory() }
    protected val viewModel: MessageInfoViewModel by viewModels { viewModelFactory }
    protected val messageViewProvider: MessageInfoViewProvider by lazy { getMessageInfoViewProvider() }
    protected var readMarkersAdapter: UserMarkerAdapter? = null
    protected var deliveredMarkersAdapter: UserMarkerAdapter? = null
    protected var playedMarkersAdapter: UserMarkerAdapter? = null
    protected lateinit var style: MessageInfoStyle
    protected lateinit var messageItemStyle: MessageItemStyle

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Keep the style in the view model.
        // If the style is not initialized it will be taken from the view model.
        if (::messageItemStyle.isInitialized)
            viewModel.messageItemStyle = messageItemStyle
        else
            messageItemStyle = viewModel.messageItemStyle

        style = MessageInfoStyle.Builder(context, messageItemStyle).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentMessageInfoBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()

        binding?.applyStyle()
        initViews()
    }

    private fun getBundleArguments() {
        messageId = arguments?.getLong(KEY_MESSAGE_ID) ?: 0
        channelId = arguments?.getLong(KEY_CHANNEL_ID) ?: 0
    }

    private fun initViews() {
        binding?.toolbar?.setNavigationClickListener {
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

    protected open fun setMessageDetails(message: SceytMessage) {
        with(binding ?: return) {
            tvSentDate.text = style.messageDateFormatter.format(requireContext(), Date(message.createdAt))
            groupSizeViews.isVisible = viewModel.getMessageAttachmentToShowSizeIfExist(message)?.let {
                tvSize.text = style.attachmentSizeFormatter.format(requireContext(), it)
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
        return MessageInfoViewProvider(requireContext(), style.messageItemStyle).also { provider ->
            provider.setMessageListener(MessageClickListeners.AttachmentClickListener { _, item, message ->
                onAttachmentClick(item, message)
            })

            provider.setNeedMediaDataCallback { viewModel.needMediaInfo(it) }
        }
    }

    protected open fun onAttachmentClick(item: FileListItem, message: SceytMessage) {
        when (item.type) {
            AttachmentTypeEnum.Image -> {
                MediaPreviewActivity.launch(requireContext(), item.attachment, message.user, message.channelId)
            }

            AttachmentTypeEnum.Video -> {
                MediaPreviewActivity.launch(requireContext(), item.attachment, message.user, message.channelId)
            }

            else -> item.attachment.openFile(requireContext())
        }
    }

    protected open fun SceytFragmentMessageInfoBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)

        tvSentLabel.apply {
            text = style.sentLabelText
            style.descriptionTitleTextStyle.apply(this)
        }

        tvSizeLabel.apply {
            text = style.sizeLabelText
            style.descriptionTitleTextStyle.apply(this)
        }

        with(style.descriptionValueTextStyle) {
            apply(tvSentDate)
            apply(tvSize)
        }

        tvSeenByLabel.apply {
            text = style.markerTitleProvider.provide(requireContext(), MarkerType.Displayed)
            style.headerTextStyle.apply(this)
        }
        tvDeliveredToLabel.apply {
            text = style.markerTitleProvider.provide(requireContext(), MarkerType.Received)
            style.headerTextStyle.apply(this)
        }
        tvPlayedByLabel.apply {
            text = style.markerTitleProvider.provide(requireContext(), MarkerType.Played)
            style.headerTextStyle.apply(this)
        }

        divider.setBackgroundColor(style.borderColor)
        dividerPlayed.setBackgroundColor(style.borderColor)
        dividerRead.setBackgroundColor(style.borderColor)
        dividerPlayed.setBackgroundColor(style.borderColor)
        dividerDelivered.setBackgroundColor(style.borderColor)
    }

    protected fun provideViewModelFactory(): MessageInfoViewModelFactory {
        getBundleArguments()
        return MessageInfoViewModelFactory(messageId, channelId)
    }

    companion object {
        const val KEY_MESSAGE_ID = "key_message_id"
        const val KEY_CHANNEL_ID = "key_channel_id"

        fun newInstance(
                message: SceytMessage,
                messageItemStyle: MessageItemStyle
        ) = MessageInfoFragment(messageItemStyle).setBundleArgumentsAs<MessageInfoFragment> {
            putLong(KEY_MESSAGE_ID, message.id)
            putLong(KEY_CHANNEL_ID, message.channelId)
        }

        fun newInstance(
                messageId: Long,
                channelId: Long,
                messageItemStyle: MessageItemStyle
        ) = MessageInfoFragment(messageItemStyle).setBundleArgumentsAs<MessageInfoFragment> {
            putLong(KEY_MESSAGE_ID, messageId)
            putLong(KEY_CHANNEL_ID, channelId)
        }
    }
}