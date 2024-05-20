package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.voice

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.databinding.SceytFragmentChannelVoiceBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.common.SyncArrayList
import com.sceyt.chatuikit.presentation.customviews.SceytPageStateView
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.SceytConversationInfoActivity
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ViewPagerAdapter
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.ChannelMediaAdapter
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.MediaStickHeaderItemDecoration
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.sceytstyles.ConversationInfoMediaStyle
import kotlinx.coroutines.launch

open class ChannelVoiceFragment : Fragment(), SceytKoinComponent, ViewPagerAdapter.HistoryClearedListener {
    private lateinit var channel: SceytChannel
    private var binding: SceytFragmentChannelVoiceBinding? = null
    protected open var mediaAdapter: ChannelMediaAdapter? = null
    protected open var pageStateView: SceytPageStateView? = null
    protected open val mediaType = listOf(AttachmentTypeEnum.Voice.value())
    private lateinit var viewModel: ChannelAttachmentsViewModel
    protected lateinit var style: ConversationInfoMediaStyle
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = ConversationInfoMediaStyle.Builder(context, null).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelVoiceBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()
        addPageStateView()
        loadInitialFilesList()
        binding?.applyStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(CHANNEL))
    }

    private fun initViewModel() {
        viewModel = viewModels<ChannelAttachmentsViewModel>().value

        lifecycleScope.launch {
            viewModel.filesFlow.collect(::onInitialVoiceList)
        }

        lifecycleScope.launch {
            viewModel.loadMoreFilesFlow.collect(::onMoreFilesList)
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    open fun onInitialVoiceList(list: List<ChannelFileItem>) {
        if (mediaAdapter == null) {
            val adapter = ChannelMediaAdapter(SyncArrayList(list), ChannelAttachmentViewHolderFactory(requireContext(), style).also {
                it.setClickListener(AttachmentClickListeners.AttachmentClickListener { _, _ ->
                    // voice message play functionality is handled in VoiceMessageViewHolder
                })
                it.setClickListener(AttachmentClickListeners.AttachmentLoaderClickListener { _, item ->
                    viewModel.pauseOrResumeUpload(item, channel.id)
                })

                it.setNeedMediaDataCallback { data -> viewModel.needMediaInfo(data) }
            }).also { mediaAdapter = it }

            with((binding ?: return).rvVoice) {
                this.adapter = adapter
                addItemDecoration(MediaStickHeaderItemDecoration(adapter))

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (isLastItemDisplaying() && viewModel.canLoadPrev())
                            loadMoreFilesList(adapter.getLastMediaItem()?.file?.id ?: 0,
                                adapter.getFileItems().size)
                    }
                })
            }
        } else binding?.rvVoice?.let { mediaAdapter?.notifyUpdate(list, it) }
    }

    open fun onMoreFilesList(list: List<ChannelFileItem>) {
        mediaAdapter?.addNewItems(list)
    }

    open fun onPageStateChange(pageState: PageState) {
        pageStateView?.updateState(pageState, mediaAdapter?.itemCount == 0, enableErrorSnackBar = false)
    }

    open fun getUserNameFormatter(): UserNameFormatter? = SceytChatUIKit.userNameFormatter

    protected open fun loadInitialFilesList() {
        if (channel.pending) {
            binding?.root?.post { pageStateView?.updateState(PageState.StateEmpty()) }
            return
        }
        viewModel.loadAttachments(channel.id, 0, false, mediaType, 0)
    }

    protected fun loadMoreFilesList(lastAttachmentId: Long, offset: Int) {
        viewModel.loadAttachments(channel.id, lastAttachmentId, true, mediaType, offset)
    }

    private fun addPageStateView() {
        binding?.root?.addView(SceytPageStateView(requireContext()).apply {
            setEmptyStateView(R.layout.sceyt_empty_state).also {
                it.findViewById<TextView>(R.id.empty_state_title).text = getString(R.string.sceyt_no_voice_items_yet)
            }
            setLoadingStateView(R.layout.sceyt_loading_state)
            pageStateView = this

            post {
                (activity as? SceytConversationInfoActivity)?.getViewPagerY()?.let {
                    if (it > 0)
                        layoutParams.height = screenHeightPx() - it
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaAdapter = null
    }

    override fun onHistoryCleared() {
        mediaAdapter?.clearData()
        pageStateView?.updateState(PageState.StateEmpty())
    }

    private fun SceytFragmentChannelVoiceBinding.applyStyle() {
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColor))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelVoiceFragment {
            val fragment = ChannelVoiceFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}