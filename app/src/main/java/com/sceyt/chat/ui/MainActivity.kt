package com.sceyt.chat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.sceyt.chat.ui.databinding.ActivityMainBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.extensions.isNightTheme
import com.sceyt.chat.ui.extensions.statusBarBackgroundColor
import com.sceyt.chat.ui.extensions.statusBarIconsColorWithBackground
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig


class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val isNightMode = isNightTheme()
        mBinding.switchCompat.isChecked = isNightMode
        SceytUIKitConfig.SceytUITheme.isDarkMode = isNightMode
        statusBarIconsColorWithBackground(isNightMode)

        mBinding.switchCompat.setOnClickListener {
            val oldIsDark = SceytUIKitConfig.SceytUITheme.isDarkMode
            SceytUIKitConfig.SceytUITheme.isDarkMode = !oldIsDark
            statusBarIconsColorWithBackground(!oldIsDark)
            if (oldIsDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}