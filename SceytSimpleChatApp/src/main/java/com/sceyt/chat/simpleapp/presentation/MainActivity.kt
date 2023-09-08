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
        SceytKitClient.connect("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE2OTQxNjg3NzksImV4cCI6MTY5NDI1NTE3OSwibmJmIjoxNjk0MTY4Nzc5LCJzdWIiOiJtIn0.W_UlmGIgQipA4MYr0Hm-6dZ8WuKZJWjMMzh4k36ATkgv1HamXFITfMYaSOJT_hp3RQi2UF3EAawcC-u5l35LJhRYmNopo_XUp7QZqQ76AhNXhz-OQBIsSkY3DGE6cr3xJ-TndOJKugHfXku0KcDfxCH8iMsUvxBo8kx2KFoYVFTTZUR7chy3EuBFp8XBHn_lZ6uCZJFoXoIJEz7H9zNV8SGeNCK_OnQh4xRrzATnTJUWhHST_iq9x5c6eXHhZFWco3a8LuvInlSqNSAbfRqr0aEAUQaT2ROJvddHU0snS0wTAc_g3-EJ_sCCblgO2dcp3oxrfEThJztiKNW21w-t2w", "testUser1")

        // Step 2 - Connect the ChannelsViewModel to the ChannelsListView
        channelsViewModel.bind(binding.channelsListView, this)
        binding.channelsListView.setChannelClickListener(ChannelClickListeners.ChannelClickListener {
            ConversationActivity.newInstance(this, it.channel)
        })
    }
}