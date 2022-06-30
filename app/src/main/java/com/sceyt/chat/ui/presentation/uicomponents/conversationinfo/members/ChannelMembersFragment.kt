package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelMembersEventData
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelMembersEventEnum
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelOwnerChangedEventData
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.toGroupChannel
import com.sceyt.chat.ui.data.toSceytMember
import com.sceyt.chat.ui.databinding.FragmentChannelMembersBinding
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.AddMembersActivity
import com.sceyt.chat.ui.presentation.uicomponents.changerole.ChangeRoleActivity
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
        initViews()
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

        viewModel.channelMemberEventLiveData.observe(viewLifecycleOwner, ::onChannelMembersEvent)

        viewModel.channelOwnerChangedEventLiveData.observe(viewLifecycleOwner, ::onChannelOwnerChanged)

        viewModel.pageStateLiveData.observe(viewLifecycleOwner) {
            if (it is PageState.StateError)
                customToastSnackBar(requireView(), it.errorMessage ?: "")
        }
    }

    private fun initViews() {
        binding.addMembers.setOnClickListener {
            addMembersActivityLauncher.launch(AddMembersActivity.newInstance(requireContext()))
            requireContext().asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }

    private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableArrayListExtra<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { users ->
                viewModel.addMembersToChannel(channel, users)
            }
        }
    }

    private fun setNewOwner(newOwnerId: String) {
        val oldOwnerPair = membersAdapter.getMemberItemByRole("owner")
        val newOwnerPair = membersAdapter.getMemberItemById(newOwnerId)
        oldOwnerPair?.let { updateMemberRole("participant", it) }
        newOwnerPair?.let { updateMemberRole("owner", it) }
        membersAdapter.showHideMoreItem(newOwnerId == ChatClient.getClient().user.id)
    }

    private fun updateMemberRole(newRole: String, pair: Pair<Int, MemberItem>) {
        val member = (pair.second as MemberItem.Member).member
        member.role = Role(newRole)
        membersAdapter.notifyItemChanged(pair.first, MemberItemPayloadDiff.NOT_CHANGED_STATE.apply {
            roleChanged = true
        })
    }

    private fun setupList(list: List<MemberItem>) {
        val currentUserIsOwner = channel.toGroupChannel().myRole() == Member.MemberType.MemberTypeOwner
        membersAdapter = ChannelMembersAdapter(list as ArrayList, currentUserIsOwner,
            ChannelMembersViewHolderFactory(requireContext()).also {
                it.setOnClickListener(MemberClickListeners.MoreClickClickListener(::showMemberMoreOptionPopup))
            })
        binding.rvMembers.adapter = membersAdapter
        binding.rvMembers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.isLastItemDisplaying() && viewModel.loadingItems.not() && viewModel.hasNext)
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
                    R.id.sceyt_change_role -> {
                        changeRoleActivityLauncher.launch(ChangeRoleActivity.newInstance(requireContext(), item.member))
                        requireContext().asAppCompatActivity()
                            .overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
                    }
                    R.id.sceyt_kick_member -> {
                        viewModel.kickMember(channel, item.member, false)
                    }
                    R.id.sceyt_block_and_kick_member -> {
                        viewModel.kickMember(channel, item.member, true)
                    }
                }
                return@setOnMenuItemClickListener false
            }
        }.show()
    }

    private val changeRoleActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(ChangeRoleActivity.CHOSEN_ROLE)?.let { role ->
                val member = result.data?.getParcelableExtra<SceytMember>(ChangeRoleActivity.MEMBER)
                        ?: return@let
                if (role == "owner")
                    viewModel.changeOwner(channel, member.id)
                else
                    viewModel.changeRole(channel, member.copy(role = Role(role)))
            }
        }
    }

    private fun onChannelMembersEvent(eventData: ChannelMembersEventData) {
        when (eventData.eventType) {
            ChannelMembersEventEnum.Role -> {
                eventData.members?.forEach { member ->
                    membersAdapter.getMemberItemById(member.id)?.let {
                        val memberItem = it.second as MemberItem.Member
                        memberItem.member = member.toSceytMember()
                        membersAdapter.notifyItemChanged(it.first, MemberItemPayloadDiff.DEFAULT)
                    }
                }
            }
            ChannelMembersEventEnum.Kicked, ChannelMembersEventEnum.Blocked -> {
                eventData.members?.forEach { member ->
                    removeMember(member.id)
                }
            }
            ChannelMembersEventEnum.Added -> {
                membersAdapter.addNewItemsFromStart(eventData.members?.map {
                    MemberItem.Member(it.toSceytMember())
                })
                binding.rvMembers.scrollToPosition(0)
            }
            else -> return
        }
    }

    private fun onChannelOwnerChanged(eventData: ChannelOwnerChangedEventData) {
        eventData.newOwner?.id?.let {
            setNewOwner(it)
        }
    }

    private fun removeMember(memberId: String) {
        membersAdapter.getMemberItemById(memberId)?.let {
            membersAdapter.getData().removeAt(it.first)
            membersAdapter.notifyItemRemoved(it.first)
        }
    }

    fun updateChannel(channel: SceytChannel) {
        this.channel = channel
        if (::membersAdapter.isInitialized.not()) return
        val isOwner = channel.toGroupChannel().myRole() == Member.MemberType.MemberTypeOwner
        membersAdapter.showHideMoreItem(isOwner)
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