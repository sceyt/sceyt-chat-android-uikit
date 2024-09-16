package com.sceyt.chatuikit.presentation.components.startchat

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
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytActivityStartChatBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersActivity
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.select_users.viewmodel.UsersViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.create_chat.create_channel.CreateChannelActivity
import com.sceyt.chatuikit.presentation.components.create_chat.create_group.CreateGroupActivity
import com.sceyt.chatuikit.presentation.components.startchat.adapters.holders.UserViewHolderFactory
import com.sceyt.chatuikit.presentation.components.startchat.adapters.UsersAdapter

class StartChatActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityStartChatBinding
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: UsersAdapter
    private var creatingChannel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(SceytActivityStartChatBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        binding.applyStyle()
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
            ChannelActivity.newInstance(this, it)
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
            overrideTransitions(sceyt_anim_slide_hold, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_out_right, false)
        }

        binding.tvNewGroup.setOnClickListener {
            selectUsersActivityLauncher.launch(SelectUsersActivity.newInstance(this), animOptions)
        }

        binding.tvNewChannel.setOnClickListener {
            createConversationLauncher.launch(Intent(this, CreateChannelActivity::class.java), animOptions)
        }
    }

    private val animOptions
        get() = ActivityOptionsCompat.makeCustomAnimation(this,
            com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right, sceyt_anim_slide_hold)

    private fun setupUsersList(list: List<UserItem>) {
        val listWithSelf = list.toMutableList()
        listWithSelf.add(0, UserItem.User(ClientWrapper.currentUser
                ?: User(SceytChatUIKit.chatUIFacade.myId.toString())))

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

    private val selectUsersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.parcelableArrayList<SceytMember>(SelectUsersActivity.SELECTED_USERS)?.let { members ->
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

    private fun SceytActivityStartChatBinding.applyStyle() {
        root.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.backgroundColor))
        toolbar.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.primaryColor))
        toolbar.setIconsTint(SceytChatUIKit.theme.accentColor)
        toolbar.setTitleColorRes(SceytChatUIKit.theme.textPrimaryColor)
        tvNewGroup.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        tvNewChannel.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        setTextViewsDrawableColor(listOf(tvNewChannel, tvNewGroup), getCompatColor(SceytChatUIKit.theme.accentColor))
        tvUsers.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
        tvUsers.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.surface1Color))
    }

    override fun finish() {
        super.finish()
        overrideTransitions(sceyt_anim_slide_hold, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_out_right, false)
    }

    companion object {

        fun launch(context: Context) {
            context.launchActivity<StartChatActivity>(com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right, sceyt_anim_slide_hold)
        }
    }
}