package com.sceyt.chat.demo.presentation.splash

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.presentation.main.MainActivity
import com.sceyt.chat.demo.presentation.welcome.WelcomeActivity
import com.sceyt.chatuikit.extensions.launchActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val viewModel: SplashViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeViewModel()
        viewModel.checkIntent(intent)
    }

    private fun observeViewModel() {
        viewModel.navigationState.observe(this) { state ->
            when (state) {
                is NavigationState.Main -> {
                    launchActivity<MainActivity>()
                    finishAfterTransition()
                }

                is NavigationState.Welcome -> {
                    launchActivity<WelcomeActivity>()
                    finishAfterTransition()
                }
            }
        }
    }
}