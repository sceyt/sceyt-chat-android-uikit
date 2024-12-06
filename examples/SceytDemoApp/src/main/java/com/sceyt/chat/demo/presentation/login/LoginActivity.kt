package com.sceyt.chat.demo.presentation.login

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.sceyt.chat.demo.databinding.ActivityLoginBinding
import com.sceyt.chat.demo.presentation.login.welcome.WelcomeFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        openFragment(WelcomeFragment())
        setContentView(view)
        statusBarIconsColorWithBackground()
    }

    companion object {
        fun launch(context: Context) {
            context.launchActivity<LoginActivity>(
                R.anim.sceyt_anim_slide_in_right,
                R.anim.sceyt_anim_slide_hold
            )
        }
    }

    fun openFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.sceyt_anim_slide_in_right,
                0,
                0,
                R.anim.sceyt_anim_slide_out_right
            )
            replace(com.sceyt.chat.demo.R.id.fragment_container, fragment)
            addToBackStack(null)
        }
    }
}