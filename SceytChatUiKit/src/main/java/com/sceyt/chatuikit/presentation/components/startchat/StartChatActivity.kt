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
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytActivityStartChatBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.channel_info.members.MemberTypeEnum
import com.sceyt.chatuikit.presentation.components.create_chat.create_channel.CreateChannelActivity
import com.sceyt.chatuikit.presentation.components.create_chat.create_group.CreateGroupActivity
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersActivity
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersPageArgs
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersResult
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.select_users.viewmodel.UsersViewModel
import com.sceyt.chatuikit.presentation.components.startchat.adapters.UsersAdapter
import com.sceyt.chatuikit.presentation.components.startchat.adapters.holders.UserViewHolderFactory
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.StartChatStyle

class StartChatActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityStartChatBinding
    private lateinit var style: StartChatStyle
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: UsersAdapter
    private var creatingChannel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = StartChatStyle.Builder(this, null).build()
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
            ChannelActivity.launch(this, it)
            finish()
            creatingChannel = false
        }
    }

    private fun initViews() {
        binding.toolbar.setQueryChangeListener { query ->
            viewModel.loadUsers(query, false)
        }

        binding.toolbar.setNavigationClickListener {
            onBackPressedDispatcher.onBackPressed()
            overrideTransitions(sceyt_anim_slide_hold, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_out_right, false)
        }

        binding.tvNewGroup.setOnClickListener {
            val args = SelectUsersPageArgs(toolbarTitle = MemberTypeEnum.Member.getPageTitle(this))
            selectUsersActivityLauncher.launch(SelectUsersActivity.newIntent(this, args), animOptions)
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
        SceytChatUIKit.currentUser?.let {
            listWithSelf.add(0, UserItem.User(it))
        }
        if (::usersAdapter.isInitialized.not()) {
            binding.rvUsers.adapter = UsersAdapter(listWithSelf, UserViewHolderFactory(this, style.itemStyle) {
                if (creatingChannel) return@UserViewHolderFactory
                creatingChannel = true
                viewModel.findOrCreatePendingDirectChannel(it.user)
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
            result.data?.parcelable<SelectUsersResult>(SelectUsersActivity.SELECTED_USERS_RESULT)?.let { data ->
                val members = data.selectedUsers.map { SceytMember(Role(MemberTypeEnum.Member.toRole()), it) }
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
        root.setBackgroundColor(style.backgroundColor)
        style.separatorTextStyle.apply(tvUsers)
        with(toolbar) {
            style.toolbarStyle.apply(this)
            setTitle(style.toolbarTitle)
        }
        with(tvNewGroup) {
            style.createGroupTextStyle.apply(this)
            text = style.createGroupText
            setDrawableStart(style.createGroupIcon)
        }
        with(tvNewChannel) {
            style.createChannelTextStyle.apply(this)
            text = style.createChannelText
            setDrawableStart(style.createChannelIcon)
        }
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