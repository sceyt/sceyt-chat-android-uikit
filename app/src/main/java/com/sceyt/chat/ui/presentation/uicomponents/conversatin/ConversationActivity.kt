package com.sceyt.chat.ui.presentation.uicomponents.conversatin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ActivityConversationBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversatin.adapter.MessagesAdapter

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)

        binding.rvMessages.adapter = MessagesAdapter()
    }
}