package com.sceyt.chat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sceyt.chat.ui.databinding.ActivityMainBinding
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        supportFragmentManager.commit {
            replace(R.id.mainFrameLayout, ChannelsFragment())
        }

        mBinding.switchCompat.isChecked = false
        mBinding.switchCompat.setOnCheckedChangeListener { _, b ->
            SceytUIKitConfig.SceytUITheme.isDarkMode = b
        }
    }
}