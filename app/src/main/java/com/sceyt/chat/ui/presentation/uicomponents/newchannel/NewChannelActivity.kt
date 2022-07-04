package com.sceyt.chat.ui.presentation.uicomponents.newchannel

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.databinding.ActivityNewChannelBinding
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.AddMembersActivity
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.adapters.UserItem
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.viewmodel.UsersViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.ConversationActivity
import com.sceyt.chat.ui.presentation.uicomponents.creategroup.CreateGroupActivity
import com.sceyt.chat.ui.presentation.uicomponents.newchannel.adapters.UserViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.newchannel.adapters.UsersAdapter

class NewChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewChannelBinding
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: UsersAdapter

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
            overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }

    private fun setupUsersList(list: List<UserItem>) {
        if (::usersAdapter.isInitialized.not()) {
            binding.rvUsers.adapter = UsersAdapter(list as ArrayList, UserViewHolderFactory(this) {
                viewModel.createDirectChannel(it.user)
            }).also { usersAdapter = it }

            binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (recyclerView.isLastItemDisplaying() && viewModel.loadingItems.not() && viewModel.hasNext)
                        viewModel.loadUsers(binding.toolbar.getQuery(), true)
                }
            })
        } else usersAdapter.notifyUpdate(list)
    }

    private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableArrayListExtra<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { users ->
                launchActivity<CreateGroupActivity>()
                overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.toolbar.isSearchMode()) {
            binding.toolbar.cancelSearchMode()
            viewModel.loadUsers(isLoadMore = false)
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
        }
    }

    companion object {

        fun launch(context: Context) {
            context.launchActivity<NewChannelActivity>()
            context.asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }
}