package com.sceyt.chat.ui.presentation.newchannel

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ActivityNewChannelBinding
import com.sceyt.chat.ui.presentation.addmembers.AddMembersActivity
import com.sceyt.chat.ui.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.ui.presentation.addmembers.viewmodel.UsersViewModel
import com.sceyt.chat.ui.presentation.creategroup.CreateGroupActivity
import com.sceyt.chat.ui.presentation.newchannel.adapters.UserViewHolderFactory
import com.sceyt.chat.ui.presentation.newchannel.adapters.UsersAdapter
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.chat.ui.presentation.conversation.ConversationActivity
import com.sceyt.sceytchatuikit.R.*
import com.sceyt.sceytchatuikit.R.anim.sceyt_anim_slide_hold

class NewChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewChannelBinding
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: UsersAdapter
    private var creatingChannel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusBarIconsColorWithBackground(isNightTheme())

        setContentView(ActivityNewChannelBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        initViewModel()
        initViews()
        setupUsersList(arrayListOf())
        viewModel.loadUsers(isLoadMore = false)
    }

    private fun initViewModel() {
        viewModel.channelsLiveData.observe(this) {
            setupUsersList(it)
        }

        viewModel.loadMoreChannelsLiveData.observe(this) {
            usersAdapter.addNewItems(it)
        }

        viewModel.createChannelLiveData.observe(this) {
            ConversationActivity.newInstance(this, it)
            creatingChannel = false
        }
    }

    private fun initViews() {
        binding.toolbar.setQueryChangeListener { query ->
            viewModel.loadUsers(query, false)
        }

        binding.toolbar.setBackClickListener {
            super.onBackPressed()
        }

        binding.tvNewGroup.setOnClickListener {
            addMembersActivityLauncher.launch(AddMembersActivity.newInstance(this))
            overridePendingTransition(anim.sceyt_anim_slide_in_right, sceyt_anim_slide_hold)
        }
    }

    private fun setupUsersList(list: List<UserItem>) {
        if (::usersAdapter.isInitialized.not()) {
            binding.rvUsers.adapter = UsersAdapter(list as ArrayList, UserViewHolderFactory(this) {
                if (creatingChannel) return@UserViewHolderFactory
                creatingChannel = true
                viewModel.createDirectChannel(it.user)
            }).also { usersAdapter = it }

            binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (recyclerView.isLastItemDisplaying() && viewModel.loadingItems.get().not() && viewModel.hasNext)
                        viewModel.loadUsers(binding.toolbar.getQuery(), true)
                }
            })
        } else usersAdapter.notifyUpdate(list)
    }

    private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableArrayListExtra<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { members ->
                CreateGroupActivity.launch(this, members)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.toolbar.isSearchMode()) {
            binding.toolbar.cancelSearchMode()
            viewModel.loadUsers(isLoadMore = false)
        } else {
            super.onBackPressed()
            overridePendingTransition(sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right)
        }
    }

    companion object {

        fun launch(context: Context) {
            context.launchActivity<NewChannelActivity>()
            context.asAppCompatActivity().overridePendingTransition(anim.sceyt_anim_slide_in_right, sceyt_anim_slide_hold)
        }
    }
}