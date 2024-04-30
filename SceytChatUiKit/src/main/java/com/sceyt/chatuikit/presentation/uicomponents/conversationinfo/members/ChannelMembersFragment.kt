package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Invited
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Joined
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Left
import com.sceyt.chatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.chatuikit.data.channeleventobserver.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Broadcast
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Direct
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Group
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Private
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Public
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytFragmentChannelMembersBinding
import com.sceyt.chatuikit.extensions.awaitAnimationEnd
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBoldSpan
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.common.SceytDialog
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ConversationInfoStyleApplier
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.ChannelMembersAdapter
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders.ChannelMembersViewHolderFactory
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.popups.MemberActionsDialog
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.popups.MemberActionsDialog.ActionsEnum.Delete
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.popups.MemberActionsDialog.ActionsEnum.RevokeAdmin
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytstyles.ConversationInfoStyle
import org.koin.androidx.viewmodel.ext.android.viewModel

open class ChannelMembersFragment : Fragment(), ChannelUpdateListener, ConversationInfoStyleApplier, SceytKoinComponent {
    protected val viewModel by viewModel<ChannelMembersViewModel>()
    protected var membersAdapter: ChannelMembersAdapter? = null
    protected var binding: SceytFragmentChannelMembersBinding? = null
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var memberType: MemberTypeEnum
        private set
    protected lateinit var style: ConversationInfoStyle
        private set
    protected var currentUserRole: Role? = null
        private set
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId

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
        binding?.applyStyle()
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
        memberType = MemberTypeEnum.entries.getOrNull(type) ?: MemberTypeEnum.Member
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

        viewModel.findOrCreateChatLiveData.observe(viewLifecycleOwner, ::onFindOrCreateChat)
    }

    private fun initViews() {
        with(binding ?: return) {
            layoutAddMembers.setOnClickListener {
                onAddMembersClick(memberType)
            }

            toolbar.setNavigationIconClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
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
        channel.members?.find { it.id == myId }?.let {
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
            setOrUpdateMembersAdapter(itemsDb)
        }
    }

    private fun addMembers(members: List<SceytMember>?) {
        if (members.isNullOrEmpty()) return
        membersAdapter?.addNewItemsToStart(members.map {
            MemberItem.Member(it)
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

    protected open fun onMemberClick(item: MemberItem.Member) {
        if (item.member.id == myId) return
        viewModel.findOrCreateChat(item.member.user)
    }

    protected open fun onMemberLongClick(item: MemberItem.Member) {
        if (currentUserIsOwnerOrAdmin().not() || item.member.id == myId) return

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
            val currentUser = channel.members?.find {
                it.id == myId
            }
            currentUserRole = currentUser?.role

            membersAdapter = ChannelMembersAdapter(data.toArrayList(),
                ChannelMembersViewHolderFactory(requireContext()).also {
                    it.setOnClickListener(object : MemberClickListeners.ClickListeners {
                        override fun onMemberClick(view: View, item: MemberItem.Member) {
                            onMemberClick(item)
                        }

                        override fun onMemberLongClick(view: View, item: MemberItem.Member) {
                            onMemberLongClick(item)
                        }
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

    protected open fun currentUserIsOwnerOrAdmin(): Boolean {
        return currentUserRole?.name == RoleTypeEnum.Owner.toString() || currentUserRole?.name == RoleTypeEnum.Admin.toString()
    }

    protected open fun loadInitialMembers() {
        viewModel.getChannelMembers(channel.id, 0, getRole())
    }

    protected fun loadMoreMembers(offset: Int) {
        viewModel.getChannelMembers(channel.id, offset, getRole())
    }

    protected fun addMembersToChannel(members: List<SceytMember>) {
        viewModel.addMembersToChannel(channel.id, members.toArrayList())
    }

    protected open fun onAddMembersClick(memberType: MemberTypeEnum) {
        // Override and add your logic
    }

    protected open fun onRevokeAdminClick(member: SceytMember) {
        SceytDialog.showSceytDialog(requireContext(), R.string.sceyt_revoke_admin_title, R.string.sceyt_revoke_admin_desc, R.string.sceyt_revoke, positiveCb = {
            revokeAdmin(member)
        }).apply {
            val name = SceytChatUIKit.userNameFormatter?.format(member.user)
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
        when (channel.getChannelType()) {
            Private, Group -> {
                titleId = R.string.sceyt_remove_member_title
                descId = R.string.sceyt_remove_member_desc
            }

            Public, Broadcast -> {
                titleId = R.string.sceyt_remove_subscriber_title
                descId = R.string.sceyt_remove_subscriber_desc
            }

            Direct -> return
        }
        SceytDialog.showSceytDialog(requireContext(), titleId = titleId, positiveBtnTitleId = R.string.sceyt_remove, positiveCb = {
            viewModel.kickMember(channel.id, member.id, false)
        }).apply {
            val name = SceytChatUIKit.userNameFormatter?.format(member.user)
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
                } else {
                    membersAdapter?.addNewItems(data.data)
                }
            }

            is PaginationResponse.ServerResponse -> {
                if (data.data is SceytResponse.Success)
                    updateMembersWithServerResponse(data, data.hasNext)
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
        when (val event = eventData.eventType) {
            is Left -> {
                event.leftMembers.forEach {
                    removeMember(it.id)
                }
            }

            is Joined -> addMembers(event.joinedMembers)
            is Invited -> {}
            else -> return
        }
    }

    protected open fun onChannelMembersEvent(eventData: ChannelMembersEventData) {
        when (eventData.eventType) {
            ChannelMembersEventEnum.Role -> {
                eventData.members.forEach { member ->
                    if (memberType == MemberTypeEnum.Admin && member.role.name != RoleTypeEnum.Admin.toString()) {
                        removeMember(member.id)
                    } else
                        membersAdapter?.getMemberItemById(member.id)?.let {
                            val memberItem = it.second as MemberItem.Member
                            memberItem.member = member
                            membersAdapter?.notifyItemChanged(it.first, MemberItemPayloadDiff.DEFAULT)
                        } ?: addMembers(arrayListOf(member))
                }
            }

            ChannelMembersEventEnum.Kicked, ChannelMembersEventEnum.Blocked -> {
                eventData.members.forEach { member ->
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

    protected open fun onFindOrCreateChat(sceytChannel: SceytChannel) {
        ConversationInfoActivity.launch(requireContext(), sceytChannel)
    }

    protected open fun onPageStateChange(pageState: PageState) {
        if (pageState is PageState.StateError)
            customToastSnackBar(pageState.errorMessage.toString())
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        this.channel = channel
        getCurrentUserRole()
    }

    override fun setStyle(style: ConversationInfoStyle) {
        this.style = style
    }

    private fun SceytFragmentChannelMembersBinding.applyStyle() {
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColor))
        toolbar.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.primaryColor))
        toolbar.setTitleColor(SceytChatUIKit.theme.textPrimaryColor)
        toolbar.setIconsTint(SceytChatUIKit.theme.accentColor)
        icAddMembers.setTintColorRes(SceytChatUIKit.theme.accentColor)
        addMembers.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
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