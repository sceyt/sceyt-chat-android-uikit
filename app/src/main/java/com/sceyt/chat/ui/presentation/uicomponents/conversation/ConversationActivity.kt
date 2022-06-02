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
    private var channelId: Long = 0L
    private lateinit var channel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)

        val isNightMode = isNightTheme()
        statusBarIconsColorWithBackground(isNightMode)

        viewModel.bindView(binding.messagesListView, lifecycleOwner = this)
        viewModel.bindView(binding.messageInputView, lifecycleOwner = this)
        viewModel.bindView(binding.headerView, lifecycleOwner = this)
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

    private fun getDataFromIntent() {
        channelId = intent.getLongExtra("channelId", 0)
        channel = intent.getParcelableExtra("gr")!!
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<ConversationActivity> {
                putExtra("gr", channel)
            }
        }
    }

    inner class MyViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            getDataFromIntent()
            return MessageListViewModel(channel) as T
        }
    }
}