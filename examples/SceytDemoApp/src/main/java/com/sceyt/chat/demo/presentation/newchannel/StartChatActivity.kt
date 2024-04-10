package com.sceyt.chat.demo.presentation.newchannel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.demo.databinding.ActivityStartChatBinding
import com.sceyt.chat.demo.presentation.addmembers.AddMembersActivity
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.demo.presentation.addmembers.viewmodel.UsersViewModel
import com.sceyt.chat.demo.presentation.conversation.ConversationActivity
import com.sceyt.chat.demo.presentation.createconversation.createchannel.CreateChannelActivity
import com.sceyt.chat.demo.presentation.createconversation.newgroup.CreateGroupActivity
import com.sceyt.chat.demo.presentation.newchannel.adapters.UserViewHolderFactory
import com.sceyt.chat.demo.presentation.newchannel.adapters.UsersAdapter
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.R.anim.sceyt_anim_slide_hold
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.isLastItemDisplaying
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelableArrayList
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.root.PageState

class StartChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartChatBinding
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: UsersAdapter
    private var creatingChannel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityStartChatBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground()

        initViewModel()
        initViews()
        setupUsersList(arrayListOf())
        viewModel.loadUsers(isLoadMore = false)

        onBackPressedDispatcher.addCallback(this) {
            if (binding.toolbar.isSearchMode()) {
                binding.toolbar.cancelSearchMode()
                viewModel.loadUsers(isLoadMore = false)
            } else finish()
        }
    }

    private fun initViewModel() {
        viewModel.pageStateLiveData.observe(this) {
            if (it is PageState.StateError) {
                //@param creatingChannel need change when error is connected with create channel
                creatingChannel = false
                customToastSnackBar(it.errorMessage)
            }
        }

        viewModel.usersLiveData.observe(this) {
            setupUsersList(it)
        }

        viewModel.loadMoreChannelsLiveData.observe(this) {
            usersAdapter.addNewItems(it)
        }

        viewModel.createChannelLiveData.observe(this) {
            ConversationActivity.newInstance(this, it)
            finish()
            creatingChannel = false
        }
    }

    private fun initViews() {
        binding.toolbar.setQueryChangeListener { query ->
            viewModel.loadUsers(query, false)
        }

        binding.toolbar.setNavigationIconClickListener {
            onBackPressedDispatcher.onBackPressed()
            overrideTransitions(sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right, false)
        }

        binding.tvNewGroup.setOnClickListener {
            addMembersActivityLauncher.launch(AddMembersActivity.newInstance(this), animOptions)
        }

        binding.tvNewChannel.setOnClickListener {
            createConversationLauncher.launch(Intent(this, CreateChannelActivity::class.java), animOptions)
        }
    }

    private val animOptions get() = ActivityOptionsCompat.makeCustomAnimation(this, anim.sceyt_anim_slide_in_right, sceyt_anim_slide_hold)

    private fun setupUsersList(list: List<UserItem>) {
        val listWithSelf = list.toMutableList()
        listWithSelf.add(0, UserItem.User(ClientWrapper.currentUser
                ?: User(SceytKitClient.myId.toString())))

        if (::usersAdapter.isInitialized.not()) {
            binding.rvUsers.adapter = UsersAdapter(listWithSelf.toArrayList(), UserViewHolderFactory(this) {
                if (creatingChannel) return@UserViewHolderFactory
                creatingChannel = true
                viewModel.findOrCreateDirectChannel(it.user)
            }).also { usersAdapter = it }

            binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (recyclerView.isLastItemDisplaying() && viewModel.canLoadNext())
                        viewModel.loadUsers(binding.toolbar.getQuery(), true)
                }
            })
        } else usersAdapter.notifyUpdate(listWithSelf)
    }

    private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.parcelableArrayList<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { members ->
                createGroupLauncher.launch(CreateGroupActivity.newIntent(this, members), animOptions)
            }
        }
    }

    private val createConversationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    private val createGroupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overrideTransitions(sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right, false)
    }

    companion object {

        fun launch(context: Context) {
            context.launchActivity<StartChatActivity>(anim.sceyt_anim_slide_in_right, sceyt_anim_slide_hold)
        }
    }
}