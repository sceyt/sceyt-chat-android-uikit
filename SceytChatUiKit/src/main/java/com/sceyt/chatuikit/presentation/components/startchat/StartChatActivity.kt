package com.sceyt.chatuikit.presentation.components.startchat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytActivityStartChatBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.navigate
import com.sceyt.chatuikit.navigation.navigateForResult
import com.sceyt.chatuikit.presentation.components.channel_info.members.MemberTypeEnum
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersActivity
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersPageArgs
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersResult
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.select_users.viewmodel.UsersViewModel
import com.sceyt.chatuikit.presentation.components.startchat.adapters.UsersAdapter
import com.sceyt.chatuikit.presentation.components.startchat.adapters.holders.UserViewHolderFactory
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.create_channel.StartChatStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class StartChatActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityStartChatBinding
    private lateinit var style: StartChatStyle
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: UsersAdapter
    private var creatingChannel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        style = StartChatStyle.Builder(this, null).build()
        setContentView(
            SceytActivityStartChatBinding.inflate(layoutInflater)
                .also { binding = it }
                .root)

        binding.applyStyle()
        applyInsetsAndWindowColor(binding.root)
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

        MessageEventManager.onOutgoingMessageFlow.onEach {
            finish()
        }.launchIn(lifecycleScope)
    }

    protected open fun initViewModel() {
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
            creatingChannel = false
            openChannelActivity(it)
        }
    }

    protected open fun initViews() {
        binding.toolbar.setQueryChangeListener { query ->
            viewModel.loadUsers(query, false)
        }

        binding.toolbar.setNavigationClickListener {
            onBackClick()
        }

        binding.layoutNewGroup.setOnClickListener {
            onNewGroupClick()
        }

        binding.layoutNewChannel.setOnClickListener {
            onNewChannelClick()
        }
    }

    protected open fun openChannelActivity(channel: SceytChannel) {
        SceytChatUIKit.navigator.navigate(this, Destination.Channel(channel))
    }

    protected open fun onNewGroupClick() {
        val args = SelectUsersPageArgs(toolbarTitle = MemberTypeEnum.Member.getPageTitle(this))
        SceytChatUIKit.navigator.navigateForResult(
            context = this,
            launcher = selectUsersActivityLauncher,
            destination = Destination.SelectUsers(args)
        )
    }

    protected open fun onNewChannelClick() {
        SceytChatUIKit.navigator.navigateForResult(
            context = this,
            launcher = createConversationLauncher,
            destination = Destination.CreateChannel()
        )
    }

    protected open fun onBackClick() {
        onBackPressedDispatcher.onBackPressed()
        overrideTransitions(
            R.anim.sceyt_anim_slide_hold,
            R.anim.sceyt_anim_slide_out_right,
            false
        )
    }

    protected open fun onUserClick(user: UserItem.User) {
        if (creatingChannel) return
        creatingChannel = true
        viewModel.findOrCreatePendingDirectChannel(user.user)
    }

    protected open fun setupUsersList(list: List<UserItem>) {
        val listWithSelf = list.toMutableList()
        SceytChatUIKit.currentUser?.let {
            listWithSelf.add(0, UserItem.User(it))
        }
        if (::usersAdapter.isInitialized.not()) {
            binding.rvUsers.adapter =
                UsersAdapter(
                    list = listWithSelf, factory = UserViewHolderFactory(
                        context = this,
                        style = style.itemStyle,
                        listeners = ::onUserClick
                    )
                ).also { usersAdapter = it }

            binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (recyclerView.isLastItemDisplaying() && viewModel.canLoadNext())
                        viewModel.loadUsers(binding.toolbar.getQuery(), true)
                }
            })
        } else usersAdapter.notifyUpdate(listWithSelf)
    }

    protected open val selectUsersActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.parcelable<SelectUsersResult>(SelectUsersActivity.SELECTED_USERS_RESULT)
                    ?.let { data ->
                        val members = data.selectedUsers.map {
                            SceytMember(
                                Role(MemberTypeEnum.Member.toRole()),
                                it
                            )
                        }
                        SceytChatUIKit.navigator.navigateForResult(
                            context = this,
                            launcher = createGroupLauncher,
                            destination = Destination.CreateGroup(members)
                        )
                    }
            }
        }

    protected open val createConversationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                finish()
            }
        }

    protected open val createGroupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                finish()
            }
        }

    protected open fun SceytActivityStartChatBinding.applyStyle() {
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
        overrideTransitions(
            R.anim.sceyt_anim_slide_hold,
            R.anim.sceyt_anim_slide_out_right,
            false
        )
    }

    companion object {

        fun createIntent(context: Context): Intent = context.createIntent<StartChatActivity>()

    }
}