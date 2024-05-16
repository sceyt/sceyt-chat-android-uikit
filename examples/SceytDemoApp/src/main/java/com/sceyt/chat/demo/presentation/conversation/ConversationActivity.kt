package com.sceyt.chat.demo.presentation.conversation

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sceyt.chat.demo.databinding.ActivityConversationBinding
import com.sceyt.chat.demo.presentation.conversationinfo.CustomConversationInfoActivity
import com.sceyt.chat.demo.presentation.mainactivity.MainActivity
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageActionsViewClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModelFactory
import com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels.bindings.bind
import com.sceyt.chatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.chatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListenersImpl
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity.Companion.ACTION_SEARCH_MESSAGES
import com.sceyt.chatuikit.presentation.uicomponents.messageinfo.MessageInfoFragment

open class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageListViewModel by viewModels { factory }
    private lateinit var channel: SceytChannel
    private var replyMessage: SceytMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityConversationBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(
            statusBarColor = R.color.sceyt_color_primary,
            navigationBarColor = R.color.sceyt_color_primary_dark)

        getDataFromIntent()

        binding.headerView.initHeaderView()
        binding.messagesListView.initConversationView()

        viewModel.bind(binding.messagesListView, lifecycleOwner = this)
        viewModel.bind(binding.messageInputView, replyMessage, lifecycleOwner = this)
        viewModel.bind(binding.headerView, replyMessage, lifecycleOwner = this)
    }

    private val factory: MessageListViewModelFactory by lazy(LazyThreadSafetyMode.NONE) {
        MessageListViewModelFactory(channel)
    }

    private fun ConversationHeaderView.initHeaderView() {
        setCustomClickListener(object : HeaderClickListenersImpl(this) {
            override fun onAvatarClick(view: View) {
                CustomConversationInfoActivity.startWithLauncher(this@ConversationActivity, channel, conversationInfoLauncher)
            }

            override fun onToolbarClick(view: View) {
                CustomConversationInfoActivity.startWithLauncher(this@ConversationActivity, channel, conversationInfoLauncher)
            }
        })
    }

    private val conversationInfoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getBooleanExtra(ACTION_SEARCH_MESSAGES, true)?.let { search ->
                if (search)
                    binding.messagesListView.startSearchMessages()
            }
        }
    }

    private fun MessagesListView.initConversationView() {
        setMessageActionsClickListener(MessageActionsViewClickListeners.MessageInfo { message ->
            hideSoftInput()
            supportFragmentManager.commit {
                setCustomAnimations(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
                replace(com.sceyt.chat.demo.R.id.frameLayout, MessageInfoFragment.newInstance(message))
                addToBackStack(MessageInfoFragment::class.java.name)
            }
        })
    }


    private fun getDataFromIntent() {
        channel = requireNotNull(intent.parcelable(CHANNEL))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<ConversationActivity>(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold) {
                putExtra(CHANNEL, channel)
            }
        }
    }

    override fun finish() {
        if (isTaskRoot) {
            launchActivity<MainActivity>(0, 0)
        }
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }
}