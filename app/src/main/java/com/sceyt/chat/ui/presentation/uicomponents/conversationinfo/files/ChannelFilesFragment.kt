package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.chat.ui.databinding.FragmentChannelFilesBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files.adapter.ChannelFilesAdapter

class ChannelFilesFragment : Fragment() {
    private lateinit var binding: FragmentChannelFilesBinding
    private var filesAdapter: ChannelFilesAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelFilesBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
    }

    private fun setupList() {
        filesAdapter = ChannelFilesAdapter()
        binding.rvFiles.adapter = filesAdapter
    }
}