package com.sceyt.chatuikit.presentation.components.create_poll

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytActivityCreatePollBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.presentation.custom_views.CustomToolbar
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle

open class CreatePollActivity : AppCompatActivity() {
    protected lateinit var binding: SceytActivityCreatePollBinding
    protected lateinit var style: CreatePollStyle
    private var createPollFragment: CreatePollFragment? = null

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
        initViews()
    }

    protected open fun initViews() = with(binding) {
        toolbar.setNavigationClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

      /*  toolbar.setMenuActionClickListener(object : CustomToolbar.MenuActionClickListener {
            override fun onMenuActionClick(menuItem: MenuItem) {
                when (menuItem.itemId) {
                    R.id.create -> onCreateClick()
                }
            }
        })*/
    }

    protected open fun loadCreatePollFragment() {
        val fragment = CreatePollFragment.newInstance(
            styleId = style.styleId
        )
        createPollFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    protected open fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(binding.toolbar)
        toolbar.setTitle(style.toolbarTitle)
        toolbar.inflateMenu(R.menu.sceyt_menu_create_poll)
    }

    protected open fun onCreateClick() {
        val pollData = createPollFragment?.getPollData()
        if (pollData != null) {
            // TODO: Handle poll creation - send to parent or implement callback
            finish()
        }
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

