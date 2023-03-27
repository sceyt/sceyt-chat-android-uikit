package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum.*
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.databinding.SceytFragmentChannelMembersBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.ChannelMembersAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders.ChannelMembersViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.popups.MemberActionsDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.popups.MemberActionsDialog.ActionsEnum.Delete
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.popups.MemberActionsDialog.ActionsEnum.RevokeAdmin
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject

open class ChannelMembersFragment : Fragment(), SceytKoinComponent {
    protected val viewModel by viewModel<ChannelMembersViewModel>()
    private val preferences: SceytSharedPreference by inject()
    protected var membersAdapter: ChannelMembersAdapter? = null
    protected var binding: SceytFragmentChannelMembersBinding? = null
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var memberType: MemberTypeEnum
        private set
    private var currentUserRole: Role? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelMembersBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()
        initViews()
        initStringsWithAddType()
        loadInitialMembers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        membersAdapter = null
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(CHANNEL))
        val type = requireNotNull(arguments?.getInt(MEMBER_TYPE, MemberTypeEnum.Member.ordinal))
        memberType = MemberTypeEnum.values().getOrNull(type) ?: MemberTypeEnum.Member
        getCurrentUserRole()
    }

    private fun initViewModel() {
        viewModel.membersLiveData.observe(viewLifecycleOwner, ::onMembersList)

        viewModel.changeOwnerLiveData.observe(viewLifecycleOwner, ::onChangeOwnerSuccess)

        viewModel.channelMemberEventLiveData.observe(viewLifecycleOwner, ::onChannelMembersEvent)

        viewModel.channelOwnerChangedEventLiveData.observe(viewLifecycleOwner, ::onChannelOwnerChanged)

        viewModel.channelEventEventLiveData.observe(viewLifecycleOwner, ::onChannelEvent)

        viewModel.channelAddMemberLiveData.observe(viewLifecycleOwner, ::onAddedMember)

        viewModel.channelRemoveMemberLiveData.observe(viewLifecycleOwner, ::onRemovedMember)

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    private fun initViews() {
        with(binding ?: return) {
            icAddMembers.imageTintList = ColorStateList.valueOf(requireContext().getCompatColor(SceytKitConfig.sceytColorAccent))

            toolbar.setIconsTint(SceytKitConfig.sceytColorAccent)

            layoutAddMembers.setOnClickListener {
                onAddMembersClick(memberType)
            }

            toolbar.setNavigationIconClickListener {
                requireActivity().finish()
            }
        }
    }

    private fun initStringsWithAddType() {
        with(binding ?: return) {
            when (memberType) {
                MemberTypeEnum.Member -> {
                    toolbar.setTitle(getString(R.string.sceyt_members))
                    addMembers.text = getString(R.string.sceyt_add_members)
                }
                MemberTypeEnum.Subscriber -> {
                    toolbar.setTitle(getString(R.string.sceyt_subscribers))
                    addMembers.text = getString(R.string.sceyt_add_subscribers)
                }
                MemberTypeEnum.Admin -> {
                    toolbar.setTitle(getString(R.string.sceyt_admins))
                    addMembers.text = getString(R.string.sceyt_add_admins)
                }
            }
        }
    }

    private fun getCurrentUserRole() {
        (channel as? SceytGroupChannel)?.members?.find { it.id == preferences.getUserId() }?.let {
            currentUserRole = it.role
        }
    }

    private fun setNewOwner(newOwnerId: String) {
        val oldOwnerPair = membersAdapter?.getMemberItemByRole(RoleTypeEnum.Owner.toString())
        val newOwnerPair = membersAdapter?.getMemberItemById(newOwnerId)
        oldOwnerPair?.let { updateMemberRole(RoleTypeEnum.Member.toString(), it) }
        newOwnerPair?.let { updateMemberRole(RoleTypeEnum.Owner.toString(), it) }
    }

    private fun updateMemberRole(newRole: String, pair: Pair<Int, MemberItem>) {
        val member = (pair.second as MemberItem.Member).member
        member.role = Role(newRole)
        membersAdapter?.notifyItemChanged(pair.first, MemberItemPayloadDiff.NOT_CHANGED_STATE.apply {
            roleChanged = true
        })
    }

    private fun updateMembersWithServerResponse(data: PaginationResponse.ServerResponse<MemberItem>, hasNext: Boolean) {
        val itemsDb = data.cacheData as ArrayList
        binding?.rvMembers?.awaitAnimationEnd {
            val members = ArrayList(membersAdapter?.getData() ?: arrayListOf())

            /* if (members.size > itemsDb.size) {
                 val items = members.subList(itemsDb.size - 1, members.size)
                 itemsDb.addAll(items.minus(itemsDb.toSet()))
             }*/

            if (data.offset + SceytKitConfig.CHANNELS_MEMBERS_LOAD_SIZE >= members.size)
                if (hasNext) {
                    if (!itemsDb.contains(MemberItem.LoadingMore))
                        itemsDb.add(MemberItem.LoadingMore)
                } else itemsDb.remove(MemberItem.LoadingMore)

            Log.i(TAG, "final " + itemsDb.map { (it as? MemberItem.Member)?.member?.fullName }.toString())
            setOrUpdateMembersAdapter(itemsDb)
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

    private fun getRole(): String? {
        return when (memberType) {
            MemberTypeEnum.Admin -> RoleTypeEnum.Admin.toString()
            MemberTypeEnum.Subscriber -> null
            MemberTypeEnum.Member -> null
        }
    }

    protected open fun showMemberLongClick(item: MemberItem.Member) {
        if (currentUserIsOwnerOrAdmin().not() || item.member.id == preferences.getUserId()) return

        MemberActionsDialog
            .newInstance(requireContext(), item.member, currentUserRole?.name == RoleTypeEnum.Owner.toString())
            .apply {
                setChooseTypeCb {
                    when (it) {
                        RevokeAdmin -> onRevokeAdminClick(item.member)
                        Delete -> onKickMemberClick(item.member)
                    }
                }
            }.show()
    }

    protected open fun setOrUpdateMembersAdapter(data: List<MemberItem>) {
        if (membersAdapter == null) {
            val currentUser = (channel as SceytGroupChannel).members.find {
                it.id == preferences.getUserId()
            }
            currentUserRole = currentUser?.role

            membersAdapter = ChannelMembersAdapter(data as ArrayList,
                ChannelMembersViewHolderFactory(requireContext()).also {
                    it.setOnClickListener(MemberClickListeners.MemberLongClickListener { _, item ->
                        showMemberLongClick(item)
                    })
                })

            binding?.rvMembers?.adapter = membersAdapter
            binding?.rvMembers?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (recyclerView.isLastItemDisplaying() && viewModel.canLoadNext())
                        loadMoreMembers(membersAdapter?.getSkip() ?: return)
                }
            })
        } else {
            binding?.rvMembers?.awaitAnimationEnd {
                membersAdapter?.notifyUpdate(data)
            }
        }
    }

    protected open fun onAddedMember(data: List<SceytMember>) {
    }

    protected open fun onRemovedMember(data: List<SceytMember>) {
    }

    protected fun currentUserIsOwnerOrAdmin(): Boolean {
        return currentUserRole?.name == RoleTypeEnum.Owner.toString() || currentUserRole?.name == RoleTypeEnum.Admin.toString()
    }

    protected fun loadInitialMembers() {
        lifecycleScope.launch {
            delay(300)
            viewModel.getChannelMembers(channel.id, 0, getRole())
        }
    }

    protected fun loadMoreMembers(offset: Int) {
        viewModel.getChannelMembers(channel.id, offset, getRole())
    }

    protected fun addMembersToChannel(members: List<SceytMember>) {
        viewModel.addMembersToChannel(channel.id, members as ArrayList)
    }

    protected open fun onAddMembersClick(memberType: MemberTypeEnum) {
        // Override and add your logic
    }

    protected open fun onRevokeAdminClick(member: SceytMember) {
        SceytDialog.showSceytDialog(requireContext(), R.string.sceyt_revoke_admin_title, R.string.sceyt_revoke_admin_desc, R.string.sceyt_revoke) {
            revokeAdmin(member)
        }.apply {
            val name = SceytKitConfig.userNameBuilder?.invoke(member.user)
                    ?: member.user.getPresentableNameCheckDeleted(requireContext())
            val desc = String.format(getString(R.string.sceyt_revoke_admin_desc), name)
            val nameFromIndex = desc.lastIndexOf(name)
            val roleFromIndex = desc.lastIndexOf("“Admin”")
            var formatted = desc.setBoldSpan(nameFromIndex, nameFromIndex + name.length)
            formatted = formatted.setBoldSpan(roleFromIndex, roleFromIndex + "“Admin”".length)
            setDescription(formatted)
        }
    }

    protected open fun onKickMemberClick(member: SceytMember) {
        val titleId: Int
        val descId: Int
        when (channel.channelType) {
            Private -> {
                titleId = R.string.sceyt_remove_member_title
                descId = R.string.sceyt_remove_member_desc
            }
            Public -> {
                titleId = R.string.sceyt_remove_subscriber_title
                descId = R.string.sceyt_remove_subscriber_desc
            }
            Direct -> return
        }
        SceytDialog.showSceytDialog(requireContext(), titleId = titleId, positiveBtnTitleId = R.string.sceyt_remove) {
            viewModel.kickMember(channel.id, member.id, false)
        }.apply {
            val name = SceytKitConfig.userNameBuilder?.invoke(member.user)
                    ?: member.user.getPresentableNameCheckDeleted(requireContext())

            val desc = String.format(getString(descId), name)
            val fromIndex = desc.lastIndexOf(name)
            setDescription(desc.setBoldSpan(fromIndex, fromIndex + name.length))
        }
    }

    protected open fun onMembersList(data: PaginationResponse<MemberItem>) {
        when (data) {
            is PaginationResponse.DBResponse -> {
                if (data.offset == 0) {
                    setOrUpdateMembersAdapter(data.data)
                    Log.i(TAG, "db =0 " + data.data.map { (it as? MemberItem.Member)?.member?.fullName }.toString())
                } else {
                    Log.i(TAG, "db >0 " + data.data.map { (it as? MemberItem.Member)?.member?.fullName }.toString())

                    membersAdapter?.addNewItems(data.data)
                }
            }
            is PaginationResponse.ServerResponse -> {
                if (data.data is SceytResponse.Success) {
                    Log.i(TAG, "server " + data.data.data?.map { (it as? MemberItem.Member)?.member?.fullName }.toString())
                    updateMembersWithServerResponse(data, data.hasNext)
                }
            }
            else -> return
        }
    }

    protected open fun onChangeOwnerSuccess(newOwnerId: String) {
        setNewOwner(newOwnerId)
    }

    protected open fun revokeAdmin(member: SceytMember) {
        viewModel.changeRole(channel.id, member.copy(role = Role(RoleTypeEnum.Member.toString())))
    }

    protected open fun changeRole(vararg member: SceytMember) {
        viewModel.changeRole(channel.id, *member)
    }

    protected open fun onChannelEvent(eventData: ChannelEventData) {
        val groupChannel = (eventData.channel as? GroupChannel) ?: return
        when (eventData.eventType) {
            Left -> {
                groupChannel.lastActiveMembers?.forEach {
                    removeMember(it.id)
                }
            }
            Joined, Invited -> addMembers(groupChannel.lastActiveMembers)
            else -> return
        }
    }

    protected open fun onChannelMembersEvent(eventData: ChannelMembersEventData) {
        when (eventData.eventType) {
            ChannelMembersEventEnum.Role -> {
                eventData.members?.forEach { member ->
                    if (memberType == MemberTypeEnum.Admin && member.role.name != RoleTypeEnum.Admin.toString()) {
                        removeMember(member.id)
                    } else
                        membersAdapter?.getMemberItemById(member.id)?.let {
                            val memberItem = it.second as MemberItem.Member
                            memberItem.member = member.toSceytMember()
                            membersAdapter?.notifyItemChanged(it.first, MemberItemPayloadDiff.DEFAULT)
                        } ?: addMembers(arrayListOf(member))
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

    protected open fun onChannelOwnerChanged(eventData: ChannelOwnerChangedEventData) {
        setNewOwner(eventData.newOwner.id)
    }

    protected open fun onPageStateChange(pageState: PageState) {
        if (pageState is PageState.StateError)
            customToastSnackBar(requireView(), pageState.errorMessage.toString())
    }

    fun updateChannel(channel: SceytChannel) {
        this.channel = channel
        getCurrentUserRole()
    }

    companion object {
        const val CHANNEL = "CHANNEL"
        const val MEMBER_TYPE = "ADD_BUTTON_TITLE"

        fun newInstance(channel: SceytChannel, addMemberType: MemberTypeEnum = MemberTypeEnum.Member): ChannelMembersFragment {
            val fragment = ChannelMembersFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
                putInt(MEMBER_TYPE, addMemberType.ordinal)
            }
            return fragment
        }
    }
}