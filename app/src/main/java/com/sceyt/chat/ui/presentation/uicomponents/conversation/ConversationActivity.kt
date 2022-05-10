package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ActivityConversationBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.bindView

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageListViewModel by viewModels { MyViewModelFactory() }
    private var channelId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)


        channelId = intent.getLongExtra("channelId", 0)
        viewModel.bindView(binding.messagesListView, lifecycleOwner = this)
        viewModel.loadMessages(0, false)
    }

    inner class MyViewModelFactory : ViewModelProvider.Factory {
        @SuppressWarnings("unchecked")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessageListViewModel(intent.getLongExtra("channelId", 0)) as T
        }
    }
}