package com.sceyt.chat.demo.presentation.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.databinding.ActivityMainBinding
import com.sceyt.chat.demo.presentation.main.adapters.MainViewPagerAdapter
import com.sceyt.chat.demo.presentation.main.profile.ProfileFragment
import com.sceyt.chat.demo.presentation.welcome.create.CreateAccountViewModel
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListFragment
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val createProfileViewModel by inject<CreateAccountViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsetsAndWindowColor(binding.root)
        binding.bottomNavigationView.setOnApplyWindowInsetsListener(null)
        statusBarIconsColorWithBackground()

        setPagerAdapter()
        setBottomNavClickListeners()
        initViewModel()

        SceytChatUIKit.chatUIFacade.channelInteractor.getTotalUnreadCount().onEach {
            binding.bottomNavigationView.getOrCreateBadge(R.id.channelsFragment).apply {
                number = it.toInt()
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
        requestNotificationPermission()
    }

    private fun initViewModel() {
        createProfileViewModel.pageStateLiveData.observe(this) { pageState ->
            if (pageState is PageState.StateError) customToastSnackBar(
                pageState.errorMessage
                        ?: return@observe
            )
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
        val adapter =
                MainViewPagerAdapter(this, arrayListOf(ChannelListFragment(), ProfileFragment()))
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 2
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { }
}