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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.chat.ui.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.chat.ui.data.channeleventobserver.ChannelMembersEventEnum
import com.sceyt.chat.ui.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.toGroupChannel
import com.sceyt.chat.ui.data.toSceytMember
import com.sceyt.chat.ui.databinding.FragmentChannelMembersBinding
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.extensions.setBundleArguments
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class ChannelMembersFragment : Fragment() {
    private var binding: FragmentChannelMembersBinding? = null
    private var membersAdapter: ChannelMembersAdapter? = null
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
        loadInitialMembers()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(CHANNEL))
    }

    private fun initViewModel() {
        viewModel.membersLiveData.observe(viewLifecycleOwner, ::onInitialMembersList)

        viewModel.loadMoreMembersLiveData.observe(viewLifecycleOwner, ::onMoreMembersList)

        viewModel.changeOwnerLiveData.observe(viewLifecycleOwner, ::changeOwnerSuccess)

        viewModel.channelMemberEventLiveData.observe(viewLifecycleOwner, ::onChannelMembersEvent)

        viewModel.channelOwnerChangedEventLiveData.observe(viewLifecycleOwner, ::onChannelOwnerChanged)

        viewModel.channelEventEventLiveData.observe(viewLifecycleOwner, ::onChannelEvent)

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    private fun initViews() {
        binding?.addMembers?.setOnClickListener {
            addMembersActivityLauncher.launch(AddMembersActivity.newInstance(requireContext()))
            requireContext().asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }

    private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableArrayListExtra<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { users ->
                addMembersToChannel(users)
            }
        }
    }

    private fun setNewOwner(newOwnerId: String) {
        val oldOwnerPair = membersAdapter?.getMemberItemByRole("owner")
        val newOwnerPair = membersAdapter?.getMemberItemById(newOwnerId)
        oldOwnerPair?.let { updateMemberRole("participant", it) }
        newOwnerPair?.let { updateMemberRole("owner", it) }
        membersAdapter?.showHideMoreItem(newOwnerId == ChatClient.getClient().user.id)
    }

    private fun updateMemberRole(newRole: String, pair: Pair<Int, MemberItem>) {
        val member = (pair.second as MemberItem.Member).member
        member.role = Role(newRole)
        membersAdapter?.notifyItemChanged(pair.first, MemberItemPayloadDiff.NOT_CHANGED_STATE.apply {
            roleChanged = true
        })
    }

    private fun showMemberMoreOptionPopup(view: View, item: MemberItem.Member) {
        PopupMenuMember(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view).also { popupMenuMember ->
            popupMenuMember.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.sceyt_set_owner -> {
                        changeOwner(item.member.id)
                    }
                    R.id.sceyt_change_role -> {
                        changeRoleActivityLauncher.launch(ChangeRoleActivity.newInstance(requireContext(), item.member))
                        requireContext().asAppCompatActivity()
                            .overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
                    }
                    R.id.sceyt_kick_member -> {
                        kickMember(item.member.id)
                    }
                    R.id.sceyt_block_and_kick_member -> {
                        blockAndKickMember(item.member.id)
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
                    changeOwner(member.id)
                else
                    changeRole(member, role)
            }
        }
    }

    private fun addMembers(members: List<Member>?) {
        if (members.isNullOrEmpty()) return
        membersAdapter?.addNewItemsToStart(members.map {
            MemberItem.Member(it.toSceytMember())
        })
        binding?.rvMembers?.scrollToPosition(0)
    }

    private fun removeMember(memberId: String) {
        membersAdapter?.getMemberItemById(memberId)?.let {
            membersAdapter?.getData()?.removeAt(it.first)
            membersAdapter?.notifyItemRemoved(it.first)
        }
    }

    protected fun loadInitialMembers() {
        viewModel.getChannelMembers(channel.id, false)
    }

    protected fun loadMoreMembers() {
        viewModel.getChannelMembers(channel.id, true)
    }

    protected fun addMembersToChannel(members: List<SceytMember>) {
        viewModel.addMembersToChannel(channel, members as ArrayList)
    }

    protected fun changeOwner(newOwnerId: String) {
        viewModel.changeOwner(channel, newOwnerId)
    }

    protected fun changeRole(member: SceytMember, role: String) {
        viewModel.changeRole(channel, member.copy(role = Role(role)))
    }

    protected fun kickMember(memberId: String) {
        viewModel.kickMember(channel, memberId, false)
    }

    protected fun blockAndKickMember(memberId: String) {
        viewModel.kickMember(channel, memberId, true)
    }

    open fun onInitialMembersList(list: List<MemberItem>) {
        val currentUserIsOwner = channel.toGroupChannel().myRole() == Member.MemberType.MemberTypeOwner
        membersAdapter = ChannelMembersAdapter(list as ArrayList, currentUserIsOwner,
            ChannelMembersViewHolderFactory(requireContext()).also {
                it.setOnClickListener(MemberClickListeners.MoreClickClickListener(::showMemberMoreOptionPopup))
            })
        binding?.rvMembers?.adapter = membersAdapter
        binding?.rvMembers?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.isLastItemDisplaying() && viewModel.loadingItems.not() && viewModel.hasNext)
                    loadMoreMembers()
            }
        })
    }

    open fun onMoreMembersList(list: List<MemberItem>) {
        lifecycleScope.launch(Dispatchers.Default) {
            val existingMembers = (membersAdapter ?: return@launch).getData()
            (list as ArrayList).removeAll(existingMembers.toSet())
            withContext(Dispatchers.Main) {
                membersAdapter?.addNewItems(list)
            }
        }
    }

    open fun changeOwnerSuccess(newOwnerId: String) {
        setNewOwner(newOwnerId)
    }

    open fun onChannelEvent(eventData: ChannelEventData) {
        val groupChannel = (eventData.channel as? GroupChannel) ?: return
        when (eventData.eventType) {
            Left -> {
                groupChannel.members?.forEach {
                    removeMember(it.id)
                }
            }
            Joined, Invited -> addMembers(groupChannel.members)
            else -> return
        }
    }

    open fun onChannelMembersEvent(eventData: ChannelMembersEventData) {
        when (eventData.eventType) {
            ChannelMembersEventEnum.Role -> {
                eventData.members?.forEach { member ->
                    membersAdapter?.getMemberItemById(member.id)?.let {
                        val memberItem = it.second as MemberItem.Member
                        memberItem.member = member.toSceytMember()
                        membersAdapter?.notifyItemChanged(it.first, MemberItemPayloadDiff.DEFAULT)
                    }
                }
            }
            ChannelMembersEventEnum.Kicked, ChannelMembersEventEnum.Blocked -> {
                eventData.members?.forEach { member ->
                    removeMember(member.id)
                }
            }
            ChannelMembersEventEnum.Added -> {
                addMembers(eventData.members)
            }
            else -> return
        }
    }

    open fun onChannelOwnerChanged(eventData: ChannelOwnerChangedEventData) {
        eventData.newOwner?.id?.let {
            setNewOwner(it)
        }
    }

    open fun onPageStateChange(pageState: PageState) {
        if (pageState is PageState.StateError)
            customToastSnackBar(requireView(), pageState.errorMessage ?: "")
    }

    fun updateChannel(channel: SceytChannel) {
        this.channel = channel
        val isOwner = channel.toGroupChannel().myRole() == Member.MemberType.MemberTypeOwner
        membersAdapter?.showHideMoreItem(isOwner)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelMembersFragment {
            val fragment = ChannelMembersFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}