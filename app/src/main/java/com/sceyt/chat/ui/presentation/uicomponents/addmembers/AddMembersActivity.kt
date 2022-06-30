package com.sceyt.chat.ui.presentation.uicomponents.addmembers

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.databinding.ActivityAddMembersBinding
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.adapters.SelectedUsersAdapter
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.adapters.UserItem
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.adapters.UsersAdapter
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.adapters.viewholders.UsersViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.viewmodel.AddUsersViewModel

class AddMembersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMembersBinding
    private val viewModel: AddUsersViewModel by viewModels()
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var selectedUsersAdapter: SelectedUsersAdapter
    private var selectedUsers = arrayListOf<SceytMember>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityAddMembersBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        initViewModel()
        initViews()
        setupUsersList(arrayListOf())
        viewModel.loadMessages(0)
    }

    private fun initViewModel() {
        viewModel.channelsLiveData.observe(this) {
            setupUsersList(it)
        }

        viewModel.loadMoreChannelsLiveData.observe(this) {
            usersAdapter.addNewItems(it)
        }
    }

    private fun initViews() {
        binding.root.layoutTransition = LayoutTransition().apply { enableTransitionType(LayoutTransition.CHANGING) }

        binding.toolbar.setQueryChangeListener { query ->
            viewModel.loadMessages(0, query)
        }

        binding.toolbar.setBackClickListener {
            super.onBackPressed()
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

    private fun setupUsersList(list: List<UserItem>) {
        initSelectedItems(list)
        if (::usersAdapter.isInitialized.not()) {
            binding.rvUsers.adapter = UsersAdapter(list as ArrayList, UsersViewHolderFactory(this) {
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
                    if (recyclerView.isLastItemDisplaying() && viewModel.loadingItems.not() && viewModel.hasNext)
                        viewModel.loadMessages(usersAdapter.getSkip(), binding.toolbar.getQuery())
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
        else selectedUsers.removeIf { member -> member.user.id == userItem.user.id }
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

    override fun onBackPressed() {
        if (binding.toolbar.isSearchMode()) {
            binding.toolbar.cancelSearchMode()
            viewModel.loadMessages(0)
        } else super.onBackPressed()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
    }

    companion object {
        const val SELECTED_USERS = "selectedUsers"

        fun newInstance(context: Context): Intent {
            return Intent(context, AddMembersActivity::class.java)
        }
    }
}