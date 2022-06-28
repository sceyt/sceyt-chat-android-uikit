package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.databinding.ActivityConversationBinding
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.isNightTheme
import com.sceyt.chat.ui.extensions.launchActivity
import com.sceyt.chat.ui.extensions.statusBarIconsColorWithBackground
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessagePopupClickListenersImpl
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.bindView
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.listeners.MessageInputClickListenersImpl

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageListViewModel by viewModels { MyViewModelFactory() }
    private lateinit var channel: SceytChannel
    private var isReplayInThread = false
    private var replayMessage: SceytMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)

        statusBarIconsColorWithBackground(isNightTheme())

        viewModel.bindView(binding.messagesListView, lifecycleOwner = this)
        viewModel.bindView(binding.messageInputView, replayMessage, lifecycleOwner = this)
        viewModel.bindView(binding.headerView, replayMessage)

        viewModel.loadMessages(0, false)


        binding.messagesListView.setCustomMessagePopupClickListener(object : MessagePopupClickListenersImpl(binding.messagesListView) {
            override fun onReactMessageClick(view: View, message: SceytMessage) {
                super.onReactMessageClick(view, message)
                println("React")
            }
        })

        binding.messagesListView.setCustomMessageClickListener(object : MessageClickListenersImpl(binding.messagesListView) {
            override fun onAttachmentClick(view: View, item: FileListItem) {
                super.onAttachmentClick(view, item)
                println("AttachmentClick")
            }
        })

        binding.messageInputView.setCustomClickListener(object : MessageInputClickListenersImpl(binding.messageInputView) {
            override fun onSendMsgClick(view: View) {
                super.onSendMsgClick(view)
                println("send")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.channel
    }

    private fun getDataFromIntent() {
        channel = intent.getParcelableExtra(CHANNEL)!!
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
        if (isReplayInThread)
            overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
    }

    inner class MyViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            getDataFromIntent()
            val conversationId = if (isReplayInThread) replayMessage?.id ?: 0 else channel.id

            @Suppress("UNCHECKED_CAST")
            return MessageListViewModel(conversationId, isReplayInThread, channel) as T
        }
    }
}