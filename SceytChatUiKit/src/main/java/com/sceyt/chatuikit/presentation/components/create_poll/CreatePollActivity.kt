package com.sceyt.chatuikit.presentation.components.create_poll

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytActivityCreatePollBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle

open class CreatePollActivity : AppCompatActivity() {
    protected lateinit var binding: SceytActivityCreatePollBinding
    protected lateinit var style: CreatePollStyle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = CreatePollStyle.Builder(this, null).build()
        StyleRegistry.register(style)

        enableEdgeToEdge()
        binding = SceytActivityCreatePollBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsAndWindowColor(binding.root)

        loadCreatePollFragment()
        applyStyle()
    }

    protected open fun loadCreatePollFragment() {
        val fragment = CreatePollFragment.newInstance(
            styleId = style.styleId
        )

        supportFragmentManager.commit {
            replace(binding.fragmentContainer.id, fragment)
        }
    }

    protected open fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)
    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        StyleRegistry.unregister(style.styleId)
    }

    companion object {

        fun launch(context: Context) {
            context.launchActivity<CreatePollActivity>(
                enterAnimResId = R.anim.sceyt_anim_slide_in_right,
                exitAnimResId = R.anim.sceyt_anim_slide_hold,
            )
        }
    }
}

