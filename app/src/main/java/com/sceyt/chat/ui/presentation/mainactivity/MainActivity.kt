package com.sceyt.chat.ui.presentation.mainactivity

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.connection.SceytConnectionProvider
import com.sceyt.chat.ui.databinding.ActivityMainBinding
import com.sceyt.chat.ui.presentation.mainactivity.adapters.MainViewPagerAdapter
import com.sceyt.chat.ui.presentation.mainactivity.profile.ProfileFragment
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.extensions.isNightTheme
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val connectionProvider by inject<SceytConnectionProvider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isNightMode = isNightTheme()
        SceytKitConfig.SceytUITheme.isDarkMode = isNightMode
        statusBarIconsColorWithBackground(isNightMode)

        setPagerAdapter()
        setBottomNavClickListeners()
        connectionProvider.connectChatClient()

        SceytKitClient.getChannelsMiddleWare().getTotalUnreadCount().onEach {
            binding.bottomNavigationView.getOrCreateBadge(R.id.channelsFragment).apply {
                number = it
                isVisible = it > 0
                maxCharacterCount = 3
                backgroundColor = "#FA4C56".toColorInt()
                verticalOffset = 10
                horizontalOffset = 10
            }
        }.launchIn(lifecycleScope)

        onBackPressedDispatcher.addCallback(this) {
            if (binding.viewPager.currentItem > 0) {
                binding.viewPager.setCurrentItem(0, false)
                binding.bottomNavigationView.menu.findItem(R.id.channelsFragment).isChecked = true
            } else
                finish()
        }
    }

    private fun setBottomNavClickListeners() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.channelsFragment -> {
                    binding.viewPager.setCurrentItem(0, false)
                }

                R.id.profileFragment -> {
                    binding.viewPager.setCurrentItem(1, false)
                }
            }
            return@setOnItemSelectedListener true
        }
    }

    private fun setPagerAdapter() {
        val adapter = MainViewPagerAdapter(this, arrayListOf(ChannelsFragment(), ProfileFragment()))
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }
}