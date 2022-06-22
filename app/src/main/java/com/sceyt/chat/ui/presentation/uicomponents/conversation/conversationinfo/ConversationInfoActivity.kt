package com.sceyt.chat.ui.presentation.uicomponents.conversation.conversationinfo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chat.ui.databinding.ActivityConversationInfoBinding

class ConversationInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}