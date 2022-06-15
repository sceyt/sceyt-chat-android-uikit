package com.sceyt.chat.ui.presentation.mainactivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ActivityMainBinding
import com.sceyt.chat.ui.extensions.isNightTheme
import com.sceyt.chat.ui.extensions.statusBarIconsColorWithBackground
import com.sceyt.chat.ui.presentation.mainactivity.adapters.MainViewPagerAdapter
import com.sceyt.chat.ui.presentation.mainactivity.profile.ProfileFragment
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig


class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val isNightMode = isNightTheme()
        SceytUIKitConfig.SceytUITheme.isDarkMode = isNightMode
        statusBarIconsColorWithBackground(isNightMode)

        setPagerAdapter()
        setBottomNavClickListeners()
    }

    override fun onBackPressed() {
        if (mBinding.viewPager.currentItem == 1) {
            mBinding.viewPager.setCurrentItem(0, false)
            mBinding.bottomNavigationView.menu.findItem(R.id.channelsFragment).isChecked = true
        } else
            super.onBackPressed()
    }

    private fun setBottomNavClickListeners() {
        mBinding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.channelsFragment -> {
                    mBinding.viewPager.setCurrentItem(0, false)
                }
                R.id.profileFragment -> {
                    mBinding.viewPager.setCurrentItem(1, false)
                }
            }
            return@setOnItemSelectedListener true
        }
    }

    private fun setPagerAdapter() {
        val adapter = MainViewPagerAdapter(this, arrayListOf(ChannelsFragment(), ProfileFragment()),
            arrayListOf(::ChannelsFragment.name, ::ProfileFragment.name))
        mBinding.viewPager.adapter = adapter
        mBinding.viewPager.isUserInputEnabled = false
    }
}