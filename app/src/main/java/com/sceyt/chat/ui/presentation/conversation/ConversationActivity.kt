package com.sceyt.chat.ui.presentation.conversation

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.ui.databinding.ActivityConversationBinding
import com.sceyt.chat.ui.presentation.conversationinfo.CustomConversationInfoActivity
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.asAppCompatActivity
import com.sceyt.sceytchatuikit.extensions.isNightTheme
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessagePopupClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.listeners.HeaderClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.MessageInputClickListenersImpl

open class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageListViewModel by viewModels {
        MyViewModelFactory()
    }
    private lateinit var channel: SceytChannel
    private var isReplayInThread = false
    private var replayMessage: SceytMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusBarIconsColorWithBackground(isNightTheme())

        getDataFromIntent()

        setContentView(ActivityConversationBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)


        viewModel.bindView(binding.messagesListView, lifecycleOwner = this)
        viewModel.bindView(binding.messageInputView, replayMessage, lifecycleOwner = this)
        viewModel.bindView(binding.headerView, replayMessage, lifecycleOwner = this)

        viewModel.loadMessages(0, 0)

        //This listener will not work if you have added custom click listener
        binding.messagesListView.setMessageClickListener(MessageClickListeners.ReplayCountClickListener { _, item ->
            newInstance(this, channel, item.message)
        })

        binding.messagesListView.setCustomMessagePopupClickListener(object : MessagePopupClickListenersImpl(binding.messagesListView) {
            override fun onReactMessageClick(view: View, message: SceytMessage) {
                super.onReactMessageClick(view, message)
                println("React")
            }

            override fun onReplayMessageInThreadClick(message: SceytMessage) {
                super.onReplayMessageInThreadClick(message)
                newInstance(this@ConversationActivity, channel, message)
            }
        })

        binding.messagesListView.setCustomMessageClickListener(object : MessageClickListenersImpl(binding.messagesListView) {
            override fun onAttachmentClick(view: View, item: FileListItem) {
                super.onAttachmentClick(view, item)
                println("AttachmentClick")
            }

            override fun onReplayCountClick(view: View, item: MessageListItem.MessageItem) {
                super.onReplayCountClick(view, item)
                newInstance(this@ConversationActivity, channel, item.message)
            }
        })

        binding.messageInputView.setCustomClickListener(object : MessageInputClickListenersImpl(binding.messageInputView) {
            override fun onSendMsgClick(view: View) {
                super.onSendMsgClick(view)
                println("send")
            }
        })

        binding.headerView.setCustomClickListener(object : HeaderClickListenersImpl(binding.headerView) {
            override fun onAvatarClick(view: View) {
                CustomConversationInfoActivity.newInstance(this@ConversationActivity, channel)
            }
        })
    }

    private fun getDataFromIntent() {
        channel = requireNotNull(intent.getParcelableExtra(CHANNEL))
        isReplayInThread = intent.getBooleanExtra(REPLAY_IN_THREAD, false)
        replayMessage = intent.getParcelableExtra(REPLAY_IN_THREAD_MESSAGE)
    }

    companion object {
        private const val CHANNEL = "CHANNEL"
        private const val REPLAY_IN_THREAD = "REPLAY_IN_THREAD"
        private const val REPLAY_IN_THREAD_MESSAGE = "REPLAY_IN_THREAD_MESSAGE"

        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<ConversationActivity> {
                putExtra(CHANNEL, channel)
            }
            context.asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }

        fun newInstance(context: Context, channel: SceytChannel, message: SceytMessage) {
            context.launchActivity<ConversationActivity> {
                putExtra(CHANNEL, channel)
                putExtra(REPLAY_IN_THREAD, true)
                putExtra(REPLAY_IN_THREAD_MESSAGE, message)
            }
            context.asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
    }

    inner class MyViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val channel: SceytChannel = requireNotNull(intent.getParcelableExtra(CHANNEL))
            val conversationId = if (isReplayInThread) replayMessage?.id ?: 0 else channel.id

            @Suppress("UNCHECKED_CAST")
            return MessageListViewModel(conversationId, isReplayInThread, channel) as T
        }
    }
}