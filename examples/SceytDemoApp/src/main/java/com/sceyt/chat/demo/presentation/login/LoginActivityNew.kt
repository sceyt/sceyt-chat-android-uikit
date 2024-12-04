package com.sceyt.chat.demo.presentation.login

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chat.demo.databinding.ActivityLoginNewBinding
import com.sceyt.chatuikit.extensions.launchActivity

private lateinit var binding: ActivityLoginNewBinding

class LoginActivityNew : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        binding = ActivityLoginNewBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    companion object {
        fun launch(context: Context) {
            context.launchActivity<LoginActivityNew>()
        }
    }
}