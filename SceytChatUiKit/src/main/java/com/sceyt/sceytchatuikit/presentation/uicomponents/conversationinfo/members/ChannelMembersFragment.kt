package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.sceytchatuikit.SceytKoinComponent
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.ChannelMembersAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders.ChannelMembersViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.popups.PopupMenuMember
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel
import com.sceyt.sceytchatuikit.databinding.FragmentChannelMembersBinding
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.root.PageStateView
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import com.sceyt.sceytchatuikit.extensions.awaitAnimationEnd
import com.sceyt.sceytchatuikit.extensions.isLastItemDisplaying
import com.sceyt.sceytchatuikit.extensions.screenHeightPx
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import org.koin.androidx.viewmodel.ext.android.viewModel

open class ChannelMembersFragment : Fragment(), SceytKoinComponent {
    private var binding: FragmentChannelMembersBinding? = null
    private val viewModel by viewModel<ChannelMembersViewModel>()
    private var membersAdapter: ChannelMembersAdapter? = null
    private var pageStateView: PageStateView? = null
    private lateinit var channel: SceytChannel

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
        addPageStateView()
        loadInitialMembers()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(CHANNEL))
    }

    private fun initViewModel() {
        viewModel.membersLiveData.observe(viewLifecycleOwner, ::onMembersList)

        viewModel.changeOwnerLiveData.observe(viewLifecycleOwner, ::onChangeOwnerSuccess)

        viewModel.channelMemberEventLiveData.observe(viewLifecycleOwner, ::onChannelMembersEvent)

        viewModel.channelOwnerChangedEventLiveData.observe(viewLifecycleOwner, ::onChannelOwnerChanged)

        viewModel.channelEventEventLiveData.observe(viewLifecycleOwner, ::onChannelEvent)

        viewModel.pageStateLiveData.observe(viewLifecycleOwner, ::onPageStateChange)
    }

    private fun initViews() {
        binding?.addMembers?.setOnClickListener {
            /* addMembersActivityLauncher.launch(AddMembersActivity.newInstance(requireContext()))
             requireContext().asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)*/
        }
    }

    private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        /* if (result.resultCode == Activity.RESULT_OK) {
             result.data?.getParcelableArrayListExtra<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { users ->
                 addMembersToChannel(users)
             }
         }*/
    }

    private fun setNewOwner(newOwnerId: String) {
        val oldOwnerPair = membersAdapter?.getMemberItemByRole("owner")
        val newOwnerPair = membersAdapter?.getMemberItemById(newOwnerId)
        oldOwnerPair?.let { updateMemberRole("participant", it) }
        newOwnerPair?.let { updateMemberRole("owner", it) }
        membersAdapter?.showHideMoreIcon(newOwnerId == ChatClient.getClient().user.id)
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
                        changeOwnerClick(item.member.id)
                    }
                    R.id.sceyt_change_role -> {
                        changeRoleClick(item.member)
                    }
                    R.id.sceyt_kick_member -> {
                        kickMemberClick(item.member.id)
                    }
                    R.id.sceyt_block_and_kick_member -> {
                        blockAndKickMemberClick(item.member.id)
                    }
                }
                return@setOnMenuItemClickListener false
            }
        }.show()
    }

    private val changeRoleActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            /* result.data?.getStringExtra(ChangeRoleActivity.CHOSEN_ROLE)?.let { role ->
                 val member = result.data?.getParcelableExtra<SceytMember>(ChangeRoleActivity.MEMBER)
                         ?: return@let
                 if (role == "owner")
                     changeOwner(member.id)
                 else
                     changeRole(member, role)
             }*/
        }
    }

    private fun addPageStateView() {
        binding?.root?.addView(PageStateView(requireContext()).apply {
            setLoadingStateView(R.layout.sceyt_loading_state)
            pageStateView = this

            post {
                (requireActivity() as? ConversationInfoActivity)?.getViewPagerY()?.let {
                    if (it > 0) {
                        layoutParams.height = screenHeightPx() - it
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
            }
        })
    }

    private fun setOrUpdateMembersAdapter(data: List<MemberItem>) {
        if (membersAdapter == null) {
            val currentUserIsOwner = channel.toGroupChannel().myRole() == Member.MemberType.MemberTypeOwner

            membersAdapter = ChannelMembersAdapter(data as ArrayList, currentUserIsOwner,
                ChannelMembersViewHolderFactory(requireContext()).also {
                    it.setOnClickListener(MemberClickListeners.MoreClickClickListener(::showMemberMoreOptionPopup))
                })

            binding?.rvMembers?.adapter = membersAdapter
            binding?.rvMembers?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (recyclerView.isLastItemDisplaying() && viewModel.loadingItems.get().not() && viewModel.hasNext)
                        loadMoreMembers(membersAdapter?.getSkip() ?: return)
                }
            })
        } else {
            binding?.rvMembers?.awaitAnimationEnd {
                membersAdapter?.notifyUpdate(data)
            }
        }
    }

    private fun updateMembersWithServerResponse(data: PaginationResponse.ServerResponse<MemberItem>, hasNext: Boolean) {
        val itemsDb = data.dbData as ArrayList
        binding?.rvMembers?.awaitAnimationEnd {
            val members = ArrayList(membersAdapter?.getData() ?: arrayListOf())

            if (members.size > itemsDb.size) {
                val items = members.subList(itemsDb.size - 1, members.size)
                itemsDb.addAll(items.minus(itemsDb.toSet()))
            }

            if (data.offset + SceytUIKitConfig.CHANNELS_MEMBERS_LOAD_SIZE >= members.size)
                if (hasNext) {
                    if (!itemsDb.contains(MemberItem.LoadingMore))
                        itemsDb.add(MemberItem.LoadingMore)
                } else itemsDb.remove(MemberItem.LoadingMore)

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

    protected fun loadInitialMembers() {
        viewModel.getChannelMembers(channel.id, 0)
    }

    protected fun loadMoreMembers(offset: Int) {
        viewModel.getChannelMembers(channel.id, offset)
    }

    protected fun addMembersToChannel(members: List<SceytMember>) {
        viewModel.addMembersToChannel(channel, members as ArrayList)
    }

    protected open fun changeOwnerClick(newOwnerId: String) {
        viewModel.changeOwner(channel, newOwnerId)
    }

    protected open fun changeRoleClick(member: SceytMember) {
        //Override Do your functional
    }

    protected open fun changeRoleClick(member: SceytMember, role: String) {
        viewModel.changeRole(channel, member.copy(role = Role(role)))
    }

    protected open fun kickMemberClick(memberId: String) {
        viewModel.kickMember(channel, memberId, false)
    }

    protected open fun blockAndKickMemberClick(memberId: String) {
        viewModel.kickMember(channel, memberId, true)
    }

    protected open fun onMembersList(data: PaginationResponse<MemberItem>) {
        when (data) {
            is PaginationResponse.DBResponse -> {
                if (data.offset == 0) {
                    setOrUpdateMembersAdapter(data.data)
                } else
                    membersAdapter?.addNewItems(data.data)
            }
            is PaginationResponse.ServerResponse -> {
                if (data.data is SceytResponse.Success)
                    updateMembersWithServerResponse(data, data.hasNext)
            }
            is PaginationResponse.Nothing -> return
        }
    }

    protected open fun onChangeOwnerSuccess(newOwnerId: String) {
        setNewOwner(newOwnerId)
    }

    protected open fun onChannelEvent(eventData: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData) {
        val groupChannel = (eventData.channel as? GroupChannel) ?: return
        when (eventData.eventType) {
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Left -> {
                groupChannel.members?.forEach {
                    removeMember(it.id)
                }
            }
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Joined, com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Invited -> addMembers(groupChannel.members)
            else -> return
        }
    }

    protected open fun onChannelMembersEvent(eventData: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData) {
        when (eventData.eventType) {
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Role -> {
                eventData.members?.forEach { member ->
                    membersAdapter?.getMemberItemById(member.id)?.let {
                        val memberItem = it.second as MemberItem.Member
                        memberItem.member = member.toSceytMember()
                        membersAdapter?.notifyItemChanged(it.first, MemberItemPayloadDiff.DEFAULT)
                    }
                }
            }
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Kicked, com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Blocked -> {
                eventData.members?.forEach { member ->
                    removeMember(member.id)
                }
            }
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Added -> {
                addMembers(eventData.members)
            }
            else -> return
        }
    }

    protected open fun onChannelOwnerChanged(eventData: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData) {
        setNewOwner(eventData.newOwner.id)
    }

    protected open fun onPageStateChange(pageState: PageState) {
        pageStateView?.updateState(pageState, (membersAdapter?.itemCount ?: 0) == 0)
    }

    fun updateChannel(channel: SceytChannel) {
        this.channel = channel
        val isOwner = channel.toGroupChannel().myRole() == Member.MemberType.MemberTypeOwner
        membersAdapter?.showHideMoreIcon(isOwner)
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