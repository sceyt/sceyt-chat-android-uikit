package com.sceyt.chat.ui.presentation.mainactivity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ActivityMainBinding
import com.sceyt.chat.ui.presentation.mainactivity.adapters.MainViewPagerAdapter
import com.sceyt.chat.ui.presentation.mainactivity.profile.ProfileFragment
import com.sceyt.sceytchatuikit.extensions.isNightTheme
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isNightMode = isNightTheme()
        SceytUIKitConfig.SceytUITheme.isDarkMode = isNightMode
        statusBarIconsColorWithBackground(isNightMode)

        setPagerAdapter()
        setBottomNavClickListeners()
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 1) {
            binding.viewPager.setCurrentItem(0, false)
            binding.bottomNavigationView.menu.findItem(R.id.channelsFragment).isChecked = true
        } else
            super.onBackPressed()
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
        val adapter = MainViewPagerAdapter(this, arrayListOf(ChannelsFragment(), ProfileFragment()),
            arrayListOf(::ChannelsFragment.name, ::ProfileFragment.name))
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }
}