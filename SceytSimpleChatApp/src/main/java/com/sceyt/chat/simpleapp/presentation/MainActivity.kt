package com.sceyt.chat.simpleapp.presentation

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chat.simpleapp.databinding.ActivityMainBinding
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.bind


class MainActivity : AppCompatActivity() {
    private val channelsViewModel: ChannelsViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityMainBinding.inflate(layoutInflater).also {
            binding = it
        }.root)

        // Step 1 - Connect Sceyt chat client
        SceytKitClient.connect("token")

        // Step 2 - Connect the ChannelsViewModel to the ChannelsListView
        channelsViewModel.bind(binding.channelsListView, this)
        binding.channelsListView.setChannelClickListener(ChannelClickListeners.ChannelClickListener {
            ConversationActivity.newInstance(this, it.channel)
        })
    }
}