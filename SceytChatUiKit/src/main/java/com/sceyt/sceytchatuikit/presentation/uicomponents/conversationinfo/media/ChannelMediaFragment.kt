package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytFragmentChannelMediaBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.isLastItemDisplaying
import com.sceyt.sceytchatuikit.extensions.screenHeightPx
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.root.PageStateView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem.Companion.getData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ViewPagerAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.ChannelAttachmentViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.ChannelMediaAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.MediaActivity
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

open class ChannelMediaFragment : Fragment(), SceytKoinComponent, ViewPagerAdapter.HistoryClearedListener {
    private lateinit var channel: SceytChannel
    private var binding: SceytFragmentChannelMediaBinding? = null
    private var mediaAdapter: ChannelMediaAdapter? = null
    private val mediaType = listOf("image", "video")
    private var pageStateView: PageStateView? = null
    private val viewModel by viewModel<ChannelAttachmentsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelMediaBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        addPageStateView()
        initViewModel()
        loadInitialMediaList()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(CHANNEL))
    }

    private fun initViewModel() {
        lifecycleScope.launch {
            viewModel.filesFlow.collect(::onInitialMediaList)
        }

        lifecycleScope.launch {
            viewModel.loadMoreFilesFlow.collect(::onMoreMediaList)
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    private fun addPageStateView() {
        binding?.root?.addView(PageStateView(requireContext()).apply {
            setEmptyStateView(R.layout.sceyt_empty_state).also {
                it.findViewById<TextView>(R.id.empty_state_title).text = getString(R.string.sceyt_no_media_items_yet)
            }
            setLoadingStateView(R.layout.sceyt_loading_state)
            pageStateView = this

            post {
                (requireActivity() as? ConversationInfoActivity)?.getViewPagerY()?.let {
                    if (it > 0)
                        layoutParams.height = screenHeightPx() - it
                }
            }
        })
    }

    open fun onInitialMediaList(list: List<ChannelFileItem>) {
        if (mediaAdapter == null) {
            mediaAdapter = ChannelMediaAdapter(list.toArrayList(), ChannelAttachmentViewHolderFactory(requireContext()).also {
                it.setNeedMediaDataCallback { data ->
                    viewModel.needMediaInfo(data)
                }

                it.setClickListener(AttachmentClickListeners.AttachmentClickListener { _, item ->
                    item.getData()?.let { data ->
                        MediaActivity.openMediaView(requireContext(), data.attachment, data.user, channel.id,true)
                    }
                })
            })
            with((binding ?: return).rvFiles) {
                adapter = mediaAdapter
                layoutManager = GridLayoutManager(requireContext(), 3).also {
                    it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (mediaAdapter?.getItemViewType(position)) {
                                ChannelAttachmentViewHolderFactory.ItemType.Loading.ordinal -> 3
                                else -> 1
                            }
                        }
                    }
                }

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (isLastItemDisplaying() && viewModel.canLoadPrev())
                            loadMoreMediaList(mediaAdapter?.getLastMediaItem()?.file?.id ?: 0,
                                mediaAdapter?.getFileItems()?.size ?: 0)
                    }
                })
            }
        } else binding?.rvFiles?.let { mediaAdapter?.notifyUpdate(list, it) }
    }

    open fun onMoreMediaList(list: List<ChannelFileItem>) {
        mediaAdapter?.addNewItems(list)
    }

    open fun onPageStateChange(pageState: PageState) {
        pageStateView?.updateState(pageState, mediaAdapter?.itemCount == 0, enableErrorSnackBar = false)
    }

    protected fun loadInitialMediaList() {
        viewModel.loadAttachments(channel.id, 0, false, mediaType, 0)
    }

    protected fun loadMoreMediaList(lastAttachmentId: Long, offset: Int) {
        viewModel.loadAttachments(channel.id, lastAttachmentId, true, mediaType, offset)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelMediaFragment {
            val fragment = ChannelMediaFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }

    override fun onCleared() {
        mediaAdapter?.clearData()
        pageStateView?.updateState(PageState.StateEmpty())
    }
}