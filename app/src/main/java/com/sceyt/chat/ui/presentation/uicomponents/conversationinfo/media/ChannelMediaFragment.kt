package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.chat.ui.databinding.FragmentChannelMediaBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.ChannelMediaAdapter

class ChannelMediaFragment : Fragment() {
    private lateinit var binding: FragmentChannelMediaBinding
    private var mediaAdapter: ChannelMediaAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelMediaBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
    }

    private fun setupList() {
        mediaAdapter = ChannelMediaAdapter()
        binding.rvFiles.adapter = mediaAdapter
    }
}