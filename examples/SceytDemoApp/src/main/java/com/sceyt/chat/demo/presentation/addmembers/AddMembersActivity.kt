package com.sceyt.chat.demo.presentation.addmembers

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.demo.databinding.ActivityAddMembersBinding
import com.sceyt.chat.demo.presentation.addmembers.adapters.SelectableUsersAdapter
import com.sceyt.chat.demo.presentation.addmembers.adapters.SelectedUsersAdapter
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.demo.presentation.addmembers.adapters.viewholders.SelectableUserViewHolderFactory
import com.sceyt.chat.demo.presentation.addmembers.viewmodel.UsersViewModel
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.isLastItemDisplaying
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.MemberTypeEnum
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class AddMembersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMembersBinding
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: SelectableUsersAdapter
    private lateinit var selectedUsersAdapter: SelectedUsersAdapter
    private var selectedUsers = arrayListOf<SceytMember>()
    private var memberType: MemberTypeEnum = MemberTypeEnum.Member
    private var buttonAlwaysEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityAddMembersBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode)

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

    private fun getIntentExtra() {
        intent?.getIntExtra(MEMBER_TYPE, memberType.ordinal)?.let { ordinal ->
            memberType = MemberTypeEnum.values().getOrNull(ordinal) ?: memberType
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

    private fun initViewModel() {
        viewModel.usersLiveData.observe(this) {
            setupUsersList(it)
        }

        viewModel.loadMoreChannelsLiveData.observe(this) {
            initSelectedItems(it)
            usersAdapter.addNewItems(it)
        }
    }

    private fun initViews() {
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

    private fun initStringsWithMemberType() {
        with(binding) {
            when (memberType) {
                MemberTypeEnum.Member -> toolbar.setTitle(getString(R.string.sceyt_add_members))
                MemberTypeEnum.Subscriber -> toolbar.setTitle(getString(R.string.sceyt_add_subscribers))
                MemberTypeEnum.Admin -> toolbar.setTitle(getString(R.string.sceyt_add_admins))
            }
        }
    }

    private fun setupUsersList(list: List<UserItem>) {
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

    private fun setSelectedUsersAdapter(item: UserItem.User) {
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

    private fun addOrRemoveFromSelectedUsers(userItem: UserItem.User, isAdd: Boolean) {
        if (isAdd)
            selectedUsers.add(SceytMember(userItem.user))
        else {
            val member = selectedUsers.find { it.user.id == userItem.user.id }
            selectedUsers.remove(member)
        }
        binding.fabNext.setEnabledOrNot(selectedUsers.isNotEmpty() || buttonAlwaysEnable)
    }

    private fun initSelectedItems(data: List<UserItem>) {
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
        overrideTransitions(anim.sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right, false)
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

            return Intent(context, AddMembersActivity::class.java).apply {
                buttonAlwaysEnable?.let { putExtra(BUTTON_ALWAYS_ENABLE, it) }
                iconTypeNext?.let { putExtra(BUTTON_ICON_TYPE_NEXT, it) }
                putExtra(MEMBER_TYPE, memberType.ordinal)
            }
        }
    }
}