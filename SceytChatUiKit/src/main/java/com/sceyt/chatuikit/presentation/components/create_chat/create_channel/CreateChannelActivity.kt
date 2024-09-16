package com.sceyt.chatuikit.presentation.components.create_chat.create_channel

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytActivityCreateChannelBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.common.SceytLoader.hideLoading
import com.sceyt.chatuikit.presentation.common.SceytLoader.showLoading
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersActivity
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.channel_info.members.MemberTypeEnum
import com.sceyt.chatuikit.presentation.components.create_chat.viewmodel.CreateChatViewModel
import kotlinx.coroutines.launch

class CreateChannelActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityCreateChannelBinding
    private val viewModel: CreateChatViewModel by viewModels()
    private lateinit var createdChannel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytActivityCreateChannelBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        binding.applyStyle()
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
                selectUsersActivityLauncher.launch(
                    SelectUsersActivity.newInstance(
                        context = this@CreateChannelActivity,
                        buttonAlwaysEnable = true,
                        memberType = MemberTypeEnum.Subscriber), animOptions)
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
        toolbar.navigationIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun createChannel(data: CreateChannelData) {
        viewModel.createChat(data)
    }

    private val selectUsersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.parcelableArrayList<SceytMember>(SelectUsersActivity.SELECTED_USERS)?.let { members ->
                viewModel.addMembers(createdChannel.id, members.map {
                    SceytMember(Role(MemberTypeEnum.Subscriber.toString()), User(it.id))
                })
            }
        } else startConversationPageAndFinish(createdChannel)
    }

    private fun startConversationPageAndFinish(channel: SceytChannel) {
        ChannelActivity.newInstance(this@CreateChannelActivity, channel)

        val intent = Intent()
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun finish() {
        super.finish()
        overrideTransitions(com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_out_right, false)
    }

    private fun SceytActivityCreateChannelBinding.applyStyle() {
        root.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.backgroundColor))
        toolbar.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.primaryColor))
        toolbar.setIconsTint(SceytChatUIKit.theme.accentColor)
        toolbar.setTitleColorRes(SceytChatUIKit.theme.textPrimaryColor)
    }
}