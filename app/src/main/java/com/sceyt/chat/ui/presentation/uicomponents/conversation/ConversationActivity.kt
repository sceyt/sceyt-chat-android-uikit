package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ActivityConversationBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.bindMessageInputView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.bindView

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageListViewModel by viewModels { MyViewModelFactory() }
    private var channelId: Long = 0L
    private var isGroup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)


        viewModel.bindView(binding.messagesListView, lifecycleOwner = this)
        viewModel.bindMessageInputView(binding.messageInputView, lifecycleOwner = this)
        viewModel.loadMessages(0, false)


        binding.messagesListView.setMessageClickListener(MessageClickListeners.ReactionLongClickListener { _, reactionItem ->
            Toast.makeText(this, "ReactionLongClick  " + reactionItem.messageItem.message.body, Toast.LENGTH_SHORT).show()
        })
    }


    private fun getDataFromIntent() {
        channelId = intent.getLongExtra("channelId", 0)
        isGroup = intent.getBooleanExtra("isGroup", false)
        val channel = intent.getSerializableExtra("channel")

        println("$channel")
    }

    inner class MyViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            getDataFromIntent()
            return MessageListViewModel(channelId, isGroup) as T
        }
    }
}