package com.sceyt.chatuikit.presentation.components.channel_info.files

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoFilesBinding
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.screenHeightPx
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.common.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.openFile
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.channel_info.ViewPagerAdapter.HistoryClearedListener
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.ChannelMediaAdapter
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.MediaStickHeaderItemDecoration
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.chatuikit.presentation.components.channel_info.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.presentation.di.ChannelInfoFilesViewModelQualifier
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoStyle
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

@Suppress("MemberVisibilityCanBePrivate")
open class ChannelInfoFilesFragment : Fragment, SceytKoinComponent, HistoryClearedListener {
    constructor() : super()

    constructor(infoStyle: ChannelInfoStyle) : super() {
        this.infoStyle = infoStyle
    }

    protected lateinit var channel: SceytChannel
    protected var binding: SceytFragmentChannelInfoFilesBinding? = null
    protected var mediaAdapter: ChannelMediaAdapter? = null
    protected var pageStateView: PageStateView? = null
    protected val mediaType = listOf(AttachmentTypeEnum.File.value)
    protected val viewModel: ChannelAttachmentsViewModel by viewModel(ChannelInfoFilesViewModelQualifier)
    protected lateinit var infoStyle: ChannelInfoStyle

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Keep the style in the view model.
        // If the style is not initialized it will be taken from the view model.
        if (::infoStyle.isInitialized)
            viewModel.infoStyle = infoStyle
        else
            infoStyle = viewModel.infoStyle
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelInfoFilesBinding.inflate(inflater, container, false).also {
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
        viewModel.observeToUpdateAfterOnResume(this)

        lifecycleScope.launch {
            viewModel.filesFlow.filterNot { it.isEmpty() }.collect(::onInitialFilesList)
        }

        lifecycleScope.launch {
            viewModel.loadMoreFilesFlow.filterNot { it.isEmpty() }.collect(::onMoreFilesList)
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    open fun onInitialFilesList(list: List<ChannelFileItem>) {
        if (mediaAdapter == null) {
            val adapter = ChannelMediaAdapter(SyncArrayList(list), ChannelAttachmentViewHolderFactory(
                requireContext(), infoStyle, infoStyle.filesStyle.dateSeparatorStyle).also {
                it.setClickListener(AttachmentClickListeners.AttachmentClickListener { _, item ->
                    item.attachment.openFile(requireContext())
                })

                it.setClickListener(AttachmentClickListeners.AttachmentLoaderClickListener { _, item ->
                    viewModel.pauseOrResumeUpload(item, channel.id)
                })

                it.setNeedMediaDataCallback { data -> viewModel.needMediaInfo(data) }
            }).also { mediaAdapter = it }

            with((binding ?: return).rvFiles) {
                this.adapter = adapter
                addItemDecoration(MediaStickHeaderItemDecoration(adapter))

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (isLastItemDisplaying() && viewModel.canLoadPrev())
                            loadMoreFilesList(adapter.getLastMediaItem()?.attachment?.id
                                    ?: 0, adapter.getFileItems().size)
                    }
                })
            }
        } else binding?.rvFiles?.let { mediaAdapter?.notifyUpdate(list, it) }
    }

    open fun onMoreFilesList(list: List<ChannelFileItem>) {
        mediaAdapter?.addNewItems(list)
    }

    open fun onPageStateChange(pageState: PageState) {
        pageStateView?.updateState(pageState, mediaAdapter?.itemCount == 0, enableErrorSnackBar = false)
    }

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
        binding?.root?.addView(PageStateView(requireContext()).apply {
            setEmptyStateView(R.layout.sceyt_empty_state).also {
                it.findViewById<TextView>(R.id.empty_state_title).text = getString(R.string.sceyt_no_file_items_yet)
            }
            setLoadingStateView(R.layout.sceyt_loading_state)
            pageStateView = this

            post {
                (activity as? ChannelInfoActivity)?.getViewPagerY()?.let {
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

    private fun SceytFragmentChannelInfoFilesBinding.applyStyle() {
        root.setBackgroundColor(infoStyle.filesStyle.backgroundColor)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(
                channel: SceytChannel,
                infoStyle: ChannelInfoStyle
        ) = ChannelInfoFilesFragment(infoStyle).apply {
            setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
        }
    }
}