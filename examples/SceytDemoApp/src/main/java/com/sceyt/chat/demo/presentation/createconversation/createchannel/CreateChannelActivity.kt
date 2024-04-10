package com.sceyt.chat.demo.presentation.createconversation.createchannel

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.demo.databinding.ActivityCreateChannelBinding
import com.sceyt.chat.demo.presentation.addmembers.AddMembersActivity
import com.sceyt.chat.demo.presentation.conversation.ConversationActivity
import com.sceyt.chat.demo.presentation.createconversation.viewmodel.CreateChatViewModel
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelableArrayList
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.common.SceytLoader.hideLoading
import com.sceyt.sceytchatuikit.presentation.common.SceytLoader.showLoading
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.MemberTypeEnum
import kotlinx.coroutines.launch

class CreateChannelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateChannelBinding
    private val viewModel: CreateChatViewModel by viewModels()
    private lateinit var createdChannel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityCreateChannelBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground()

        initViewModel()
        binding.initViews()
    }

    private fun initViewModel() {
        viewModel.createChatLiveData.observe(this) {
            lifecycleScope.launch {
                createdChannel = it
                val animOptions = ActivityOptionsCompat.makeCustomAnimation(this@CreateChannelActivity,
                    anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold)
                addMembersActivityLauncher.launch(
                    AddMembersActivity.newInstance(
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

    private fun ActivityCreateChannelBinding.initViews() {
        layoutToolbar.navigationIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun createChannel(data: CreateChannelData) {
        viewModel.createChat(data)
    }

    private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.parcelableArrayList<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { members ->
                viewModel.addMembers(createdChannel.id, members.map {
                    SceytMember(Role(MemberTypeEnum.Subscriber.toString()), User(it.id))
                })
            }
        } else startConversationPageAndFinish(createdChannel)
    }

    private fun startConversationPageAndFinish(channel: SceytChannel) {
        ConversationActivity.newInstance(this@CreateChannelActivity, channel)

        val intent = Intent()
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun finish() {
        super.finish()
        overrideTransitions(anim.sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right, false)
    }
}