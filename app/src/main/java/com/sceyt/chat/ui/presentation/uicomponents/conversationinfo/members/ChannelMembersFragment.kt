package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytGroupChannel
import com.sceyt.chat.ui.databinding.FragmentChannelMembersBinding
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.extensions.findIndexed
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.extensions.setBundleArguments
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.ChannelMembersAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.popups.PopupMenuMember
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel

class ChannelMembersFragment : Fragment() {
    private lateinit var binding: FragmentChannelMembersBinding
    private lateinit var membersAdapter: ChannelMembersAdapter
    private val viewModel: ChannelMembersViewModel by viewModels()
    private lateinit var channel: SceytChannel
    private var hasNext: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelMembersBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()
        viewModel.getChannelMembers(channel.id, false)
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(CHANNEL))
    }

    private fun initViewModel() {
        viewModel.membersLiveData.observe(viewLifecycleOwner) {
            setupList(it)
        }

        viewModel.loadMoreMembersLiveData.observe(viewLifecycleOwner) {
            membersAdapter.addNewItems(it)
        }

        viewModel.changeOwnerLiveData.observe(viewLifecycleOwner) {
            setNewOwner(it)
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner) {
            if (it is PageState.StateError)
                customToastSnackBar(requireView(), it.errorMessage ?: "")
        }
    }

    private fun setNewOwner(newOwnerId: String) {
        val oldOwnerPair = membersAdapter.getData().findIndexed { it is MemberItem.Member && it.member.role.name == "owner" }
        val newOwnerPair = membersAdapter.getData().findIndexed { it is MemberItem.Member && it.member.id == newOwnerId }
        oldOwnerPair?.let { updateMemberRole("participant", it) }
        newOwnerPair?.let { updateMemberRole("owner", it) }
        membersAdapter.notifyUpdate(membersAdapter.getData(), false)
    }

    private fun updateMemberRole(newRole: String, pair: Pair<Int, MemberItem>) {
        val member = (pair.second as MemberItem.Member).member
        member.role = Role(newRole)
        membersAdapter.notifyItemChanged(pair.first, MemberItemPayloadDiff.NOT_CHANGED_STATE.apply {
            roleChanged = true
        })
    }

    private fun setupList(list: List<MemberItem>) {
        val channelOwnerId = (channel as SceytGroupChannel).members.find { it.role.name == "owner" }?.id
        val currentUserIsOwner = ChatClient.getClient().user.id == channelOwnerId
        membersAdapter = ChannelMembersAdapter(list as ArrayList, currentUserIsOwner,
            ChannelMembersViewHolderFactory(requireContext()).also {
                it.setOnClickListener(MemberClickListeners.MoreClickClickListener(::showMemberMoreOptionPopup))
            })
        binding.rvMembers.adapter = membersAdapter
        binding.rvMembers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.isLastItemDisplaying() && viewModel.loadingMembers.not() && viewModel.hasNext)
                    viewModel.getChannelMembers(channel.id, true)
            }
        })
    }

    private fun showMemberMoreOptionPopup(view: View, item: MemberItem.Member) {
        PopupMenuMember(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view).also { popupMenuMember ->
            popupMenuMember.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.sceyt_set_owner -> {
                        viewModel.changeOwner(channel, item.member.id)
                    }
                }
                return@setOnMenuItemClickListener false
            }
        }.show()
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