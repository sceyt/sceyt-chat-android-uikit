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
        SceytKitClient.connect("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE2OTQwMDc1OTgsImV4cCI6MTY5NDA5Mzk5OCwibmJmIjoxNjk0MDA3NTk4LCJzdWIiOiJtIn0.ImUJhBLtjMTnLRUJdu762mO-G6i4O49wbWlatuftvN08Z6kED8SL8AfRofCKydu9CeZLFYqr4vHETAvgK0EapSkm5kLzDwd7RF3bxMULyPFgRGMJg5L9IEtaGHdEe7YCKruX_IWJk3SfW6bEu9rJiwD_--tZzcJaRPy69nGEwRETZZ5jYLltj8hOPXBw38M_m_sP4P_igpwY7pXs2nNRo-PCh4-snljFs97FnuLbOcz5bRtoGE5whLe7QvOZ8oDWSukxQsgGoQPUeVlJLXJD4zlu51YnLbWjl1Ni4YHMo7napLNfDmfIk1-0A6hEDWsqzjYw0dUiXwDECLThhx9JYg", "testUser1")

        // Step 2 - Connect the ChannelsViewModel to the ChannelsListView
        channelsViewModel.bind(binding.channelsListView, this)
        binding.channelsListView.setChannelClickListener(ChannelClickListeners.ChannelClickListener {
            ConversationActivity.newInstance(this, it.channel)
        })
    }
}