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
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTextViewsDrawableColor
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
            binding.rvUsers.adapter = UsersAdapter(listWithSelf, UserViewHolderFactory(this) {
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
        root.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))
        toolbar.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.colors.primaryColor))
        toolbar.setIconsTint(SceytChatUIKit.theme.colors.accentColor)
     ///   toolbar.setTitleColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
        tvNewGroup.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
        tvNewChannel.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
        setTextViewsDrawableColor(listOf(tvNewChannel, tvNewGroup), getCompatColor(SceytChatUIKit.theme.colors.accentColor))
        tvUsers.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
        tvUsers.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.colors.surface1Color))
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