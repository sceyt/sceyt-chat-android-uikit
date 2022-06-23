package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.databinding.FragmentChannelMembersBinding
import com.sceyt.chat.ui.extensions.setBundleArguments
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.ChannelMembersAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel

class ChannelMembersFragment : Fragment() {
    private lateinit var binding: FragmentChannelMembersBinding
    private lateinit var membersAdapter: ChannelMembersAdapter
    private val viewModel: ChannelMembersViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelMembersBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
    }

    private fun setupList() {
        membersAdapter = ChannelMembersAdapter()
        binding.rvMembers.adapter = membersAdapter
        binding.rvMembers.setRecycledViewPool(null)
    }

    companion object {
        private const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelMembersFragment {
            val fragment = ChannelMembersFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}