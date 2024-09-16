package com.sceyt.chatuikit.presentation.components.select_users

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytActivityAddMembersBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.components.select_users.adapters.SelectableUsersAdapter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.SelectedUsersAdapter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.select_users.adapters.holders.SelectableUserViewHolderFactory
import com.sceyt.chatuikit.presentation.components.select_users.viewmodel.UsersViewModel
import com.sceyt.chatuikit.presentation.components.channel_info.members.MemberTypeEnum

open class SelectUsersActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityAddMembersBinding
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: SelectableUsersAdapter
    private lateinit var selectedUsersAdapter: SelectedUsersAdapter
    private var selectedUsers = arrayListOf<SceytMember>()
    private var memberType: MemberTypeEnum = MemberTypeEnum.Member
    private var buttonAlwaysEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(SceytActivityAddMembersBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        binding.applyStyle()
        statusBarIconsColorWithBackground()

        getIntentExtra()
        initViewModel()
        initViews()
        initStringsWithMemberType()
        setupUsersList(arrayListOf())
        viewModel.loadUsers(isLoadMore = false)

        onBackPressedDispatcher.addCallback(this) {
            if (binding.toolbar.isSearchMode()) {
                binding.toolbar.cancelSearchMode()
                viewModel.loadUsers(isLoadMore = false)
            } else finish()
        }
    }

    protected open fun getIntentExtra() {
        intent?.getIntExtra(MEMBER_TYPE, memberType.ordinal)?.let { ordinal ->
            memberType = MemberTypeEnum.entries.getOrNull(ordinal) ?: memberType
        }

        intent?.getBooleanExtra(BUTTON_ALWAYS_ENABLE, false)?.let {
            buttonAlwaysEnable = it
            binding.fabNext.setEnabledOrNot(it)
        }

        intent?.getBooleanExtra(BUTTON_ICON_TYPE_NEXT, false)?.let {
            if (it)
                binding.fabNext.setImageResource(R.drawable.sceyt_ic_arrow_next)
        }
    }

    protected open fun initViewModel() {
        viewModel.usersLiveData.observe(this) {
            setupUsersList(it)
        }

        viewModel.loadMoreChannelsLiveData.observe(this) {
            initSelectedItems(it)
            usersAdapter.addNewItems(it)
        }
    }

    protected open fun initViews() {
        binding.root.layoutTransition = LayoutTransition().apply { enableTransitionType(LayoutTransition.CHANGING) }

        binding.toolbar.setQueryChangeListener { query ->
            viewModel.loadUsers(query, false)
        }

        binding.toolbar.setNavigationIconClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.fabNext.setOnClickListener {
            if (selectedUsers.isEmpty()) {
                setResult(RESULT_CANCELED, intent)
                finish()
            } else {
                val intent = Intent()
                intent.putParcelableArrayListExtra(SELECTED_USERS, selectedUsers)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    protected open fun initStringsWithMemberType() {
        with(binding) {
            when (memberType) {
                MemberTypeEnum.Member -> toolbar.setTitle(getString(R.string.sceyt_add_members))
                MemberTypeEnum.Subscriber -> toolbar.setTitle(getString(R.string.sceyt_add_subscribers))
                MemberTypeEnum.Admin -> toolbar.setTitle(getString(R.string.sceyt_add_admins))
            }
        }
    }

    protected open fun setupUsersList(list: List<UserItem>) {
        initSelectedItems(list)
        if (::usersAdapter.isInitialized.not()) {
            binding.rvUsers.adapter = SelectableUsersAdapter(list as ArrayList, SelectableUserViewHolderFactory(this) {
                if (it.chosen) {
                    addOrRemoveFromSelectedUsers(it, true)
                    setSelectedUsersAdapter(it)
                } else {
                    if (::selectedUsersAdapter.isInitialized)
                        selectedUsersAdapter.removeItem(it)
                    addOrRemoveFromSelectedUsers(it, false)
                }

            }).also { usersAdapter = it }

            binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (recyclerView.isLastItemDisplaying() && viewModel.canLoadNext())
                        viewModel.loadUsers(binding.toolbar.getQuery(), true)
                }
            })
        } else usersAdapter.notifyUpdate(list)
    }

    protected open fun setSelectedUsersAdapter(item: UserItem.User) {
        if (::selectedUsersAdapter.isInitialized.not()) {
            binding.rvSelectedUsers.adapter = SelectedUsersAdapter(arrayListOf(item)) {
                addOrRemoveFromSelectedUsers(it, false)
                usersAdapter.uncheckItem(it.user.id)
            }.also { selectedUsersAdapter = it }

            binding.rvSelectedUsers.itemAnimator = DefaultItemAnimator().apply {
                addDuration = 100
                removeDuration = 100
                changeDuration = 100
            }
        } else {
            selectedUsersAdapter.addItem(item)
            binding.rvSelectedUsers.scrollToPosition(selectedUsersAdapter.itemCount - 1)
        }
    }

    protected open fun addOrRemoveFromSelectedUsers(userItem: UserItem.User, isAdd: Boolean) {
        if (isAdd)
            selectedUsers.add(SceytMember(userItem.user))
        else {
            val member = selectedUsers.find { it.user.id == userItem.user.id }
            selectedUsers.remove(member)
        }
        binding.fabNext.setEnabledOrNot(selectedUsers.isNotEmpty() || buttonAlwaysEnable)
    }

    protected open fun initSelectedItems(data: List<UserItem>) {
        if (selectedUsers.isEmpty()) return
        val common = data.toMutableSet()
        common.retainAll {
            it is UserItem.User && selectedUsers.find { member -> member.id == it.user.id } != null
        }
        common.forEach {
            (it as? UserItem.User)?.chosen = true
        }
    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    protected open fun SceytActivityAddMembersBinding.applyStyle() {
        root.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.backgroundColor))
        toolbar.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.primaryColor))
        toolbar.setIconsTint(SceytChatUIKit.theme.accentColor)
        toolbar.setTitleColorRes(SceytChatUIKit.theme.textPrimaryColor)
        divider.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
        divider.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.surface1Color))
    }

    companion object {
        private const val BUTTON_ALWAYS_ENABLE = "BUTTON_ALWAYS_ENABLE"
        private const val BUTTON_ICON_TYPE_NEXT = "BUTTON_ICON_TYPE_NEXT"
        const val SELECTED_USERS = "selectedUsers"
        const val MEMBER_TYPE = "memberType"

        fun newInstance(context: Context,
                        memberType: MemberTypeEnum = MemberTypeEnum.Member,
                        buttonAlwaysEnable: Boolean? = null,
                        iconTypeNext: Boolean? = null
        ): Intent {

            return Intent(context, SelectUsersActivity::class.java).apply {
                buttonAlwaysEnable?.let { putExtra(BUTTON_ALWAYS_ENABLE, it) }
                iconTypeNext?.let { putExtra(BUTTON_ICON_TYPE_NEXT, it) }
                putExtra(MEMBER_TYPE, memberType.ordinal)
            }
        }
    }
}