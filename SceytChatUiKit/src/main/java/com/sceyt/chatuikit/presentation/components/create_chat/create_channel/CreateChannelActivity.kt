package com.sceyt.chatuikit.presentation.components.create_chat.create_channel

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytActivityCreateChannelBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.common.SceytLoader.hideLoading
import com.sceyt.chatuikit.presentation.common.SceytLoader.showLoading
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.channel_info.members.MemberTypeEnum
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.CreateChatViewModel
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersActivity
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersPageArgs
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersResult
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.CreateChannelStyle
import kotlinx.coroutines.launch

class CreateChannelActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityCreateChannelBinding
    private lateinit var style: CreateChannelStyle
    private val viewModel: CreateChatViewModel by viewModels()
    private lateinit var createdChannel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        style = CreateChannelStyle.Builder(this, null).build()
        setContentView(SceytActivityCreateChannelBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        binding.applyStyle()
        applyInsetsAndWindowColor(binding.root)
        statusBarIconsColorWithBackground()

        initViewModel()
        binding.initViews()
    }

    private fun initViewModel() {
        viewModel.createChatLiveData.observe(this) {
            lifecycleScope.launch {
                createdChannel = it
                val animOptions = ActivityOptionsCompat.makeCustomAnimation(this@CreateChannelActivity,
                    com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold)
                val args = SelectUsersPageArgs(
                    toolbarTitle = MemberTypeEnum.Subscriber.getPageTitle(this@CreateChannelActivity),
                    actionButtonAlwaysEnable = true,
                )
                selectUsersActivityLauncher.launch(
                    SelectUsersActivity.newIntent(
                        context = this@CreateChannelActivity,
                        args = args), animOptions)
            }
        }

        viewModel.addMembersLiveData.observe(this) {
            ((createdChannel.members?.toArrayList())
                    ?: arrayListOf()).addAll(it.members ?: return@observe)
            startConversationPageAndFinish(it)
        }

        viewModel.pageStateLiveData.observe(this) {
            when (it) {
                is PageState.StateLoading -> {
                    if (it.isLoading)
                        showLoading(this)
                    else hideLoading()
                }

                else -> hideLoading()
            }
        }
    }

    private fun SceytActivityCreateChannelBinding.initViews() {
        toolbar.setNavigationClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun createChannel(data: CreateChannelData) {
        viewModel.createChat(data)
    }

    private val selectUsersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.parcelable<SelectUsersResult>(SelectUsersActivity.SELECTED_USERS_RESULT)?.let { data ->
                val members = data.selectedUsers.map { SceytMember(Role(MemberTypeEnum.Subscriber.toRole()), it) }
                viewModel.addMembers(createdChannel.id, members)
            }
        } else startConversationPageAndFinish(createdChannel)
    }

    private fun startConversationPageAndFinish(channel: SceytChannel) {
        ChannelActivity.launch(this@CreateChannelActivity, channel)

        val intent = Intent()
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun finish() {
        super.finish()
        overrideTransitions(com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_out_right, false)
    }

    private fun SceytActivityCreateChannelBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
    }
}