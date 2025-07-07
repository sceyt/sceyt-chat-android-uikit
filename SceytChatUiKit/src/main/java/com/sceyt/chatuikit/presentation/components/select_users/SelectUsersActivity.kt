package com.sceyt.chatuikit.presentation.components.select_users

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytActivitySelectUsersBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.components.select_users.adapters.SelectableUsersAdapter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.SelectedUsersAdapter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.select_users.adapters.holders.SelectableUserViewHolderFactory
import com.sceyt.chatuikit.presentation.components.select_users.viewmodel.UsersViewModel
import com.sceyt.chatuikit.styles.SelectUsersStyle
import kotlinx.parcelize.Parcelize

open class SelectUsersActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivitySelectUsersBinding
    private lateinit var style: SelectUsersStyle
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var usersAdapter: SelectableUsersAdapter
    private lateinit var selectedUsersAdapter: SelectedUsersAdapter
    private var selectedUsers = arrayListOf<SceytUser>()
    private val pageArgs by lazy { intent.parcelable<SelectUsersPageArgs>(PAGE_ARGS) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = SelectUsersStyle.Builder(this, null).build()

        setContentView(SceytActivitySelectUsersBinding.inflate(layoutInflater)
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
        with(binding) {
            root.layoutTransition = LayoutTransition().apply { enableTransitionType(LayoutTransition.CHANGING) }
            toolbar.setTitle(pageArgs?.toolbarTitle ?: "")
            pageArgs?.actionButtonIcon?.let { fabNext.setImageResource(it) }
            fabNext.setEnabledOrNot(pageArgs?.actionButtonAlwaysEnable == true)

            toolbar.setQueryChangeListener { query ->
                viewModel.loadUsers(query, false)
            }

            toolbar.setNavigationClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            fabNext.setOnClickListener {
                if (selectedUsers.isEmpty()) {
                    setResult(RESULT_CANCELED, intent)
                    finish()
                } else {
                    val intent = Intent()
                    intent.putExtra(SELECTED_USERS_RESULT, SelectUsersResult(selectedUsers))
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    protected open fun setupUsersList(list: List<UserItem>) {
        initSelectedItems(list)
        if (::usersAdapter.isInitialized.not()) {
            binding.rvUsers.adapter = SelectableUsersAdapter(list,
                SelectableUserViewHolderFactory(this, style.itemStyle) {
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
            binding.rvSelectedUsers.adapter = SelectedUsersAdapter(
                listOf(item), style.selectedItemStyle
            ) {
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
            selectedUsers.add(userItem.user)
        else {
            val member = selectedUsers.find { it.id == userItem.user.id }
            selectedUsers.remove(member)
        }
        binding.fabNext.setEnabledOrNot(selectedUsers.isNotEmpty() || pageArgs?.actionButtonAlwaysEnable == true)
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

    protected open fun SceytActivitySelectUsersBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
        with(divider) {
            style.separatorTextStyle.apply(this)
            text = style.separatorText
        }
        style.actionButton.applyToCustomButton(fabNext)
    }

    companion object {
        private const val PAGE_ARGS = "pageArgs"
        const val SELECTED_USERS_RESULT = "selectedUsersResult"

        fun newIntent(
                context: Context,
                args: SelectUsersPageArgs,
        ) = Intent(context, SelectUsersActivity::class.java).apply {
            putExtra(PAGE_ARGS, args)
        }
    }
}

@Parcelize
data class SelectUsersPageArgs(
        val toolbarTitle: String? = null,
        val actionButtonAlwaysEnable: Boolean = false,
        @DrawableRes val actionButtonIcon: Int = R.drawable.sceyt_ic_arrow_next,
) : Parcelable


@Parcelize
data class SelectUsersResult(
        val selectedUsers: List<SceytUser>,
) : Parcelable