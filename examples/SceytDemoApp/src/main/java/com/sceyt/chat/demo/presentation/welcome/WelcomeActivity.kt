package com.sceyt.chat.demo.presentation.welcome

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sceyt.chat.demo.databinding.ActivityWelcomeBinding
import com.sceyt.chat.demo.presentation.welcome.create.CreateAccountFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        statusBarIconsColorWithBackground()
    }

    companion object {
        fun launch(context: Context) {
            context.launchActivity<WelcomeActivity>(
                R.anim.sceyt_anim_slide_in_right,
                R.anim.sceyt_anim_slide_hold
            )
        }
    }

    fun openCreateAccountFragment() {
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.sceyt_anim_slide_in_right,
                R.anim.sceyt_anim_slide_hold,
                R.anim.sceyt_anim_slide_hold,
                R.anim.sceyt_anim_slide_out_right
            )
            val tag = CreateAccountFragment::class.java.simpleName
            replace(binding.fragmentContainer.id, CreateAccountFragment(), tag)
            addToBackStack(tag)
        }
    }
}